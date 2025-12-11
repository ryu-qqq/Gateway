package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
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
 * IpBlockQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpBlockQueryAdapter 단위 테스트")
class IpBlockQueryAdapterTest {

    @Mock
    private IpBlockRedisRepository ipBlockRedisRepository;

    private IpBlockQueryAdapter ipBlockQueryAdapter;

    @BeforeEach
    void setUp() {
        ipBlockQueryAdapter = new IpBlockQueryAdapter(ipBlockRedisRepository);
    }

    @Nested
    @DisplayName("isBlocked 메서드")
    class IsBlockedTest {

        @Test
        @DisplayName("차단된 IP는 true를 반환해야 한다")
        void shouldReturnTrueWhenIpIsBlocked() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.isBlocked(eq(ipAddress)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockQueryAdapter.isBlocked(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();

            then(ipBlockRedisRepository).should().isBlocked(ipAddress);
        }

        @Test
        @DisplayName("차단되지 않은 IP는 false를 반환해야 한다")
        void shouldReturnFalseWhenIpIsNotBlocked() {
            // given
            String ipAddress = "192.168.1.200";

            given(ipBlockRedisRepository.isBlocked(eq(ipAddress)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = ipBlockQueryAdapter.isBlocked(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("IPv6 주소도 확인할 수 있어야 한다")
        void shouldCheckIpv6Address() {
            // given
            String ipAddress = "2001:0db8:85a3::8a2e:0370:7334";

            given(ipBlockRedisRepository.isBlocked(eq(ipAddress)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockQueryAdapter.isBlocked(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("localhost도 확인할 수 있어야 한다")
        void shouldCheckLocalhost() {
            // given
            String ipAddress = "127.0.0.1";

            given(ipBlockRedisRepository.isBlocked(eq(ipAddress)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = ipBlockQueryAdapter.isBlocked(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.isBlocked(eq(ipAddress)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = ipBlockQueryAdapter.isBlocked(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getBlockTtlSeconds 메서드")
    class GetBlockTtlSecondsTest {

        @Test
        @DisplayName("차단 남은 시간을 초 단위로 반환해야 한다")
        void shouldReturnBlockTtlInSeconds() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.just(3600L)); // 1시간

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(3600L)
                    .verifyComplete();

            then(ipBlockRedisRepository).should().getBlockTtl(ipAddress);
        }

        @Test
        @DisplayName("차단되지 않은 IP는 -2를 반환해야 한다")
        void shouldReturnNegativeForNotBlockedIp() {
            // given
            String ipAddress = "192.168.1.200";

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.just(-2L)); // 키 없음

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(-2L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("TTL이 없는 차단은 -1을 반환해야 한다")
        void shouldReturnNegativeOneForNoTtl() {
            // given
            String ipAddress = "192.168.1.150";

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.just(-1L)); // TTL 없음 (영구 차단)

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(-1L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("긴 차단 시간도 정확히 반환해야 한다")
        void shouldReturnLongBlockTtl() {
            // given
            String ipAddress = "10.0.0.1";
            long longTtl = 2592000L; // 30일

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.just(longTtl));

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(longTtl)
                    .verifyComplete();
        }

        @Test
        @DisplayName("짧은 TTL도 정확히 반환해야 한다")
        void shouldReturnShortTtl() {
            // given
            String ipAddress = "172.16.0.1";

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.just(30L)); // 30초

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(30L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.getBlockTtl(eq(ipAddress)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Long> result = ipBlockQueryAdapter.getBlockTtlSeconds(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
