package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
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
 * IpBlockCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpBlockCommandAdapter 단위 테스트")
class IpBlockCommandAdapterTest {

    @Mock
    private IpBlockRedisRepository ipBlockRedisRepository;

    private IpBlockCommandAdapter ipBlockCommandAdapter;

    @BeforeEach
    void setUp() {
        ipBlockCommandAdapter = new IpBlockCommandAdapter(ipBlockRedisRepository);
    }

    @Nested
    @DisplayName("block 메서드")
    class BlockTest {

        @Test
        @DisplayName("IP를 차단하고 true를 반환해야 한다")
        void shouldBlockIpAndReturnTrue() {
            // given
            String ipAddress = "192.168.1.100";
            Duration duration = Duration.ofHours(1);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();

            then(ipBlockRedisRepository).should().block(ipAddress, duration);
        }

        @Test
        @DisplayName("IPv6 주소도 차단할 수 있어야 한다")
        void shouldBlockIpv6Address() {
            // given
            String ipAddress = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
            Duration duration = Duration.ofHours(24);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("짧은 기간으로 IP를 차단할 수 있어야 한다")
        void shouldBlockWithShortDuration() {
            // given
            String ipAddress = "10.0.0.1";
            Duration duration = Duration.ofMinutes(5);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("긴 기간으로 IP를 차단할 수 있어야 한다")
        void shouldBlockWithLongDuration() {
            // given
            String ipAddress = "172.16.0.1";
            Duration duration = Duration.ofDays(30);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("차단 실패 시 false를 반환해야 한다")
        void shouldReturnFalseWhenBlockFails() {
            // given
            String ipAddress = "192.168.1.200";
            Duration duration = Duration.ofHours(1);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

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
            Duration duration = Duration.ofHours(1);

            given(ipBlockRedisRepository.block(eq(ipAddress), eq(duration)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.block(ipAddress, duration);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("unblock 메서드")
    class UnblockTest {

        @Test
        @DisplayName("IP 차단을 해제하고 true를 반환해야 한다")
        void shouldUnblockIpAndReturnTrue() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.unblock(eq(ipAddress)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.unblock(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();

            then(ipBlockRedisRepository).should().unblock(ipAddress);
        }

        @Test
        @DisplayName("차단되지 않은 IP 해제 시도 시 false를 반환해야 한다")
        void shouldReturnFalseWhenIpNotBlocked() {
            // given
            String ipAddress = "192.168.1.200";

            given(ipBlockRedisRepository.unblock(eq(ipAddress)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.unblock(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("IPv6 주소도 차단 해제할 수 있어야 한다")
        void shouldUnblockIpv6Address() {
            // given
            String ipAddress = "::1";

            given(ipBlockRedisRepository.unblock(eq(ipAddress)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.unblock(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String ipAddress = "192.168.1.100";

            given(ipBlockRedisRepository.unblock(eq(ipAddress)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = ipBlockCommandAdapter.unblock(ipAddress);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
