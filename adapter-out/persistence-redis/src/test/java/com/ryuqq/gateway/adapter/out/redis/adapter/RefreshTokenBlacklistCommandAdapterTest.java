package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.RefreshTokenBlacklistRedisRepository;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * RefreshTokenBlacklistCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenBlacklistCommandAdapter 단위 테스트")
class RefreshTokenBlacklistCommandAdapterTest {

    @Mock private RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository;

    private RefreshTokenBlacklistCommandAdapter refreshTokenBlacklistCommandAdapter;

    @BeforeEach
    void setUp() {
        refreshTokenBlacklistCommandAdapter =
                new RefreshTokenBlacklistCommandAdapter(refreshTokenBlacklistRedisRepository);
    }

    @Nested
    @DisplayName("addToBlacklist 메서드")
    class AddToBlacklistTest {

        @Test
        @DisplayName("Refresh Token을 Blacklist에 등록해야 한다")
        void shouldAddRefreshTokenToBlacklist() {
            // given
            String tenantId = "tenant-123";
            String tokenValue = "a".repeat(64); // 64자 이상 토큰
            RefreshToken refreshToken = RefreshToken.of(tokenValue);
            long ttlSeconds = 604800L; // 7일

            given(
                            refreshTokenBlacklistRedisRepository.addToBlacklist(
                                    eq(tenantId),
                                    eq(tokenValue),
                                    eq(Duration.ofSeconds(ttlSeconds))))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result =
                    refreshTokenBlacklistCommandAdapter.addToBlacklist(
                            tenantId, refreshToken, ttlSeconds);

            // then
            StepVerifier.create(result).verifyComplete();

            then(refreshTokenBlacklistRedisRepository)
                    .should()
                    .addToBlacklist(tenantId, tokenValue, Duration.ofSeconds(ttlSeconds));
        }

        @Test
        @DisplayName("짧은 TTL로도 등록할 수 있어야 한다")
        void shouldAddWithShortTtl() {
            // given
            String tenantId = "tenant-456";
            String tokenValue = "b".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);
            long ttlSeconds = 60L; // 1분

            given(
                            refreshTokenBlacklistRedisRepository.addToBlacklist(
                                    anyString(), anyString(), any(Duration.class)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result =
                    refreshTokenBlacklistCommandAdapter.addToBlacklist(
                            tenantId, refreshToken, ttlSeconds);

            // then
            StepVerifier.create(result).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String tenantId = "tenant-123";
            String tokenValue = "c".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);
            long ttlSeconds = 3600L;

            given(
                            refreshTokenBlacklistRedisRepository.addToBlacklist(
                                    anyString(), anyString(), any(Duration.class)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result =
                    refreshTokenBlacklistCommandAdapter.addToBlacklist(
                            tenantId, refreshToken, ttlSeconds);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}
