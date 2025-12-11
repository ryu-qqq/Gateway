package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.RefreshTokenBlacklistRedisRepository;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
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
 * RefreshTokenBlacklistQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenBlacklistQueryAdapter 단위 테스트")
class RefreshTokenBlacklistQueryAdapterTest {

    @Mock private RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository;

    private RefreshTokenBlacklistQueryAdapter refreshTokenBlacklistQueryAdapter;

    @BeforeEach
    void setUp() {
        refreshTokenBlacklistQueryAdapter =
                new RefreshTokenBlacklistQueryAdapter(refreshTokenBlacklistRedisRepository);
    }

    @Nested
    @DisplayName("isBlacklisted 메서드")
    class IsBlacklistedTest {

        @Test
        @DisplayName("Blacklist에 존재하면 true를 반환해야 한다")
        void shouldReturnTrueWhenBlacklisted() {
            // given
            String tenantId = "tenant-123";
            String tokenValue = "a".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);

            given(refreshTokenBlacklistRedisRepository.isBlacklisted(eq(tenantId), eq(tokenValue)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result =
                    refreshTokenBlacklistQueryAdapter.isBlacklisted(tenantId, refreshToken);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();

            then(refreshTokenBlacklistRedisRepository).should().isBlacklisted(tenantId, tokenValue);
        }

        @Test
        @DisplayName("Blacklist에 존재하지 않으면 false를 반환해야 한다")
        void shouldReturnFalseWhenNotBlacklisted() {
            // given
            String tenantId = "tenant-123";
            String tokenValue = "b".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);

            given(refreshTokenBlacklistRedisRepository.isBlacklisted(eq(tenantId), eq(tokenValue)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result =
                    refreshTokenBlacklistQueryAdapter.isBlacklisted(tenantId, refreshToken);

            // then
            StepVerifier.create(result).expectNext(false).verifyComplete();
        }

        @Test
        @DisplayName("다른 테넌트의 토큰은 별도로 조회되어야 한다")
        void shouldQueryByTenantId() {
            // given
            String tenantId1 = "tenant-1";
            String tenantId2 = "tenant-2";
            String tokenValue = "c".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);

            given(refreshTokenBlacklistRedisRepository.isBlacklisted(eq(tenantId1), eq(tokenValue)))
                    .willReturn(Mono.just(true));
            given(refreshTokenBlacklistRedisRepository.isBlacklisted(eq(tenantId2), eq(tokenValue)))
                    .willReturn(Mono.just(false));

            // when & then
            StepVerifier.create(
                            refreshTokenBlacklistQueryAdapter.isBlacklisted(
                                    tenantId1, refreshToken))
                    .expectNext(true)
                    .verifyComplete();

            StepVerifier.create(
                            refreshTokenBlacklistQueryAdapter.isBlacklisted(
                                    tenantId2, refreshToken))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String tenantId = "tenant-123";
            String tokenValue = "d".repeat(64);
            RefreshToken refreshToken = RefreshToken.of(tokenValue);

            given(refreshTokenBlacklistRedisRepository.isBlacklisted(eq(tenantId), eq(tokenValue)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result =
                    refreshTokenBlacklistQueryAdapter.isBlacklisted(tenantId, refreshToken);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}
