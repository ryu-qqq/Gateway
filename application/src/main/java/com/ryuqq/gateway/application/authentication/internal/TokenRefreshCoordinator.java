package com.ryuqq.gateway.application.authentication.internal;

import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.manager.RedisLockCommandManager;
import com.ryuqq.gateway.application.authentication.manager.RefreshTokenBlacklistCommandManager;
import com.ryuqq.gateway.application.authentication.manager.RefreshTokenBlacklistQueryManager;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Token Refresh Coordinator
 *
 * <p>Access Token Refresh 로직을 조율하는 Coordinator
 *
 * <p><strong>처리 흐름</strong>:
 *
 * <ol>
 *   <li>Redis Lock 획득 (Race Condition 방지)
 *   <li>Refresh Token Blacklist 확인 (재사용 탐지)
 *   <li>AuthHub Token Refresh 호출
 *   <li>기존 Refresh Token Blacklist 등록 (Rotation)
 *   <li>새 Token Pair 반환
 *   <li>Redis Lock 해제 (finally)
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TokenRefreshCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshCoordinator.class);

    /** Refresh Token Blacklist TTL (7일, 초 단위) */
    private static final long BLACKLIST_TTL_SECONDS = 7L * 24 * 60 * 60;

    private final RedisLockCommandManager redisLockCommandManager;
    private final RefreshTokenBlacklistQueryManager blacklistQueryManager;
    private final RefreshTokenBlacklistCommandManager blacklistCommandManager;
    private final AuthHubClient authHubClient;

    public TokenRefreshCoordinator(
            RedisLockCommandManager redisLockCommandManager,
            RefreshTokenBlacklistQueryManager blacklistQueryManager,
            RefreshTokenBlacklistCommandManager blacklistCommandManager,
            AuthHubClient authHubClient) {
        this.redisLockCommandManager = redisLockCommandManager;
        this.blacklistQueryManager = blacklistQueryManager;
        this.blacklistCommandManager = blacklistCommandManager;
        this.authHubClient = authHubClient;
    }

    /**
     * Token Refresh 조율 실행
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @param currentRefreshToken 현재 Refresh Token
     * @return Mono&lt;RefreshAccessTokenResponse&gt;
     */
    public Mono<RefreshAccessTokenResponse> coordinate(
            String tenantId, Long userId, RefreshToken currentRefreshToken) {

        return acquireLock(tenantId, userId)
                .flatMap(
                        lockAcquired -> {
                            if (!lockAcquired) {
                                return Mono.error(
                                        new TokenRefreshFailedException(
                                                "Failed to acquire lock for token refresh"));
                            }
                            return executeRefresh(tenantId, userId, currentRefreshToken)
                                    .doFinally(signal -> releaseLock(tenantId, userId).subscribe());
                        });
    }

    /** Lock 획득 */
    private Mono<Boolean> acquireLock(String tenantId, Long userId) {
        return redisLockCommandManager
                .tryLock(tenantId, userId)
                .doOnNext(
                        acquired -> {
                            if (acquired) {
                                log.debug("Lock acquired for tenant:{}, user:{}", tenantId, userId);
                            } else {
                                log.warn(
                                        "Failed to acquire lock for tenant:{}, user:{}",
                                        tenantId,
                                        userId);
                            }
                        });
    }

    /** Lock 해제 */
    private Mono<Void> releaseLock(String tenantId, Long userId) {
        return redisLockCommandManager
                .unlock(tenantId, userId)
                .doOnSuccess(
                        v -> log.debug("Lock released for tenant:{}, user:{}", tenantId, userId))
                .onErrorResume(
                        e -> {
                            log.warn(
                                    "Failed to release lock for tenant:{}, user:{}",
                                    tenantId,
                                    userId,
                                    e);
                            return Mono.empty();
                        });
    }

    /** Token Refresh 실행 (Lock 획득 후) */
    private Mono<RefreshAccessTokenResponse> executeRefresh(
            String tenantId, Long userId, RefreshToken currentRefreshToken) {

        return checkBlacklist(tenantId, currentRefreshToken)
                .flatMap(
                        isBlacklisted -> {
                            if (isBlacklisted) {
                                log.warn(
                                        "Refresh token reuse detected for tenant:{}, user:{}",
                                        tenantId,
                                        userId);
                                return Mono.error(
                                        new RefreshTokenReusedException(
                                                "Refresh token reuse detected - possible token"
                                                        + " theft"));
                            }
                            return refreshAndRotate(tenantId, currentRefreshToken);
                        });
    }

    /** Blacklist 확인 */
    private Mono<Boolean> checkBlacklist(String tenantId, RefreshToken refreshToken) {
        return blacklistQueryManager
                .isBlacklisted(tenantId, refreshToken)
                .doOnNext(
                        isBlacklisted -> {
                            if (isBlacklisted) {
                                log.warn("Blacklisted token attempted for tenant:{}", tenantId);
                            }
                        });
    }

    /** AuthHub Refresh 호출 및 Rotation (Blacklist 등록) */
    private Mono<RefreshAccessTokenResponse> refreshAndRotate(
            String tenantId, RefreshToken currentRefreshToken) {

        return authHubClient
                .refreshAccessToken(tenantId, currentRefreshToken.value())
                .flatMap(
                        newTokenPair ->
                                addToBlacklistAndReturn(
                                        tenantId, currentRefreshToken, newTokenPair))
                .onErrorResume(
                        this::isExternalServiceError,
                        e -> {
                            log.error("AuthHub token refresh failed", e);
                            return Mono.error(
                                    new TokenRefreshFailedException("Token refresh failed", e));
                        });
    }

    /** 기존 Token Blacklist 등록 후 새 Token 반환 */
    private Mono<RefreshAccessTokenResponse> addToBlacklistAndReturn(
            String tenantId, RefreshToken oldToken, TokenPair newTokenPair) {

        return blacklistCommandManager
                .addToBlacklist(tenantId, oldToken, BLACKLIST_TTL_SECONDS)
                .thenReturn(RefreshAccessTokenResponse.from(newTokenPair))
                .doOnSuccess(
                        response ->
                                log.debug(
                                        "Token refresh completed successfully for tenant:{}",
                                        tenantId));
    }

    /** 외부 서비스 오류인지 확인 */
    private boolean isExternalServiceError(Throwable e) {
        return !(e instanceof RefreshTokenReusedException)
                && !(e instanceof TokenRefreshFailedException);
    }
}
