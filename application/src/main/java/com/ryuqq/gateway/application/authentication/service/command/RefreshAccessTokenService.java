package com.ryuqq.gateway.application.authentication.service.command;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshAccessTokenUseCase;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.application.authentication.port.out.command.RedisLockCommandPort;
import com.ryuqq.gateway.application.authentication.port.out.command.RefreshTokenBlacklistCommandPort;
import com.ryuqq.gateway.application.authentication.port.out.query.RefreshTokenBlacklistQueryPort;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Access Token Refresh Service (RefreshAccessTokenUseCase 구현체)
 *
 * <p>Refresh Token을 사용하여 새 Access Token을 발급받는 UseCase 구현체
 *
 * <p><strong>처리 순서</strong>:
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
 * <p><strong>보안 특징</strong>:
 *
 * <ul>
 *   <li>Refresh Token Rotation: 매 Refresh 시 새 Refresh Token 발급
 *   <li>Refresh Token Reuse Detection: Blacklist 확인으로 탈취 감지
 *   <li>Distributed Lock: 동시 Refresh 요청 직렬화
 * </ul>
 *
 * <p><strong>Zero-Tolerance 준수</strong>:
 *
 * <ul>
 *   <li>Transaction 불필요 (외부 API + Redis 사용)
 *   <li>Lombok 금지
 *   <li>Reactive Programming (Mono/Flux)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshAccessTokenService.class);

    /** Refresh Token Blacklist TTL (7일, 초 단위) */
    private static final long BLACKLIST_TTL_SECONDS = 7L * 24 * 60 * 60;

    private final RedisLockCommandPort redisLockCommandPort;
    private final RefreshTokenBlacklistQueryPort blacklistQueryPort;
    private final RefreshTokenBlacklistCommandPort blacklistCommandPort;
    private final AuthHubClient authHubClient;

    /** 생성자 (Lombok 금지) */
    public RefreshAccessTokenService(
            RedisLockCommandPort redisLockCommandPort,
            RefreshTokenBlacklistQueryPort blacklistQueryPort,
            RefreshTokenBlacklistCommandPort blacklistCommandPort,
            AuthHubClient authHubClient) {
        this.redisLockCommandPort = redisLockCommandPort;
        this.blacklistQueryPort = blacklistQueryPort;
        this.blacklistCommandPort = blacklistCommandPort;
        this.authHubClient = authHubClient;
    }

    /**
     * Access Token Refresh 실행
     *
     * @param command RefreshAccessTokenCommand (tenantId, userId, refreshToken)
     * @return Mono&lt;RefreshAccessTokenResponse&gt; (새 Access Token + Refresh Token)
     */
    @Override
    public Mono<RefreshAccessTokenResponse> execute(RefreshAccessTokenCommand command) {
        String tenantId = command.tenantId();
        Long userId = command.userId();
        RefreshToken currentRefreshToken = RefreshToken.of(command.refreshToken());

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
        return redisLockCommandPort
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
        return redisLockCommandPort
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
        return blacklistQueryPort
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

        return blacklistCommandPort
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
