package com.ryuqq.gateway.adapter.out.redis.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * IpBlockRedisRepository 통합 테스트
 *
 * <p>TestContainers Redis를 사용하여 실제 IP 차단 동작을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("IpBlockRedisRepository 통합 테스트")
class IpBlockRedisRepositoryIntegrationTest extends RedisTestSupport {

    @Autowired private IpBlockRedisRepository ipBlockRedisRepository;

    @Nested
    @DisplayName("block 메서드")
    class BlockTest {

        @Test
        @DisplayName("IPv4 주소를 차단할 수 있어야 한다")
        void shouldBlockIpv4Address() {
            // given
            String ipAddress = "192.168.1.100";
            Duration blockDuration = Duration.ofMinutes(30);

            // when
            StepVerifier.create(ipBlockRedisRepository.block(ipAddress, blockDuration))
                    .assertNext(success -> assertThat(success).isTrue())
                    .verifyComplete();

            // then
            StepVerifier.create(ipBlockRedisRepository.isBlocked(ipAddress))
                    .assertNext(blocked -> assertThat(blocked).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("IPv6 주소를 차단할 수 있어야 한다")
        void shouldBlockIpv6Address() {
            // given
            String ipAddress = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
            Duration blockDuration = Duration.ofMinutes(60);

            // when
            StepVerifier.create(ipBlockRedisRepository.block(ipAddress, blockDuration))
                    .assertNext(success -> assertThat(success).isTrue())
                    .verifyComplete();

            // then
            StepVerifier.create(ipBlockRedisRepository.isBlocked(ipAddress))
                    .assertNext(blocked -> assertThat(blocked).isTrue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("unblock 메서드")
    class UnblockTest {

        @Test
        @DisplayName("차단된 IP를 해제하면 true를 반환해야 한다")
        void shouldUnblockBlockedIp() {
            // given
            String ipAddress = "10.0.0.1";
            ipBlockRedisRepository.block(ipAddress, Duration.ofMinutes(30)).block();

            // when
            StepVerifier.create(ipBlockRedisRepository.unblock(ipAddress))
                    .assertNext(success -> assertThat(success).isTrue())
                    .verifyComplete();

            // then
            StepVerifier.create(ipBlockRedisRepository.isBlocked(ipAddress))
                    .assertNext(blocked -> assertThat(blocked).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("차단되지 않은 IP 해제 시 false를 반환해야 한다")
        void shouldReturnFalseWhenUnblockingNonBlockedIp() {
            // given
            String ipAddress = "172.16.0.1";

            // when & then
            StepVerifier.create(ipBlockRedisRepository.unblock(ipAddress))
                    .assertNext(success -> assertThat(success).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("isBlocked 메서드")
    class IsBlockedTest {

        @Test
        @DisplayName("차단된 IP는 true를 반환해야 한다")
        void shouldReturnTrueForBlockedIp() {
            // given
            String ipAddress = "192.168.100.50";
            ipBlockRedisRepository.block(ipAddress, Duration.ofMinutes(30)).block();

            // when & then
            StepVerifier.create(ipBlockRedisRepository.isBlocked(ipAddress))
                    .assertNext(blocked -> assertThat(blocked).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("차단되지 않은 IP는 false를 반환해야 한다")
        void shouldReturnFalseForNonBlockedIp() {
            // given
            String ipAddress = "192.168.200.100";

            // when & then
            StepVerifier.create(ipBlockRedisRepository.isBlocked(ipAddress))
                    .assertNext(blocked -> assertThat(blocked).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getBlockTtl 메서드")
    class GetBlockTtlTest {

        @Test
        @DisplayName("차단된 IP의 남은 TTL을 반환해야 한다")
        void shouldReturnRemainingTtlForBlockedIp() {
            // given
            String ipAddress = "10.10.10.10";
            Duration blockDuration = Duration.ofSeconds(300);
            ipBlockRedisRepository.block(ipAddress, blockDuration).block();

            // when & then
            StepVerifier.create(ipBlockRedisRepository.getBlockTtl(ipAddress))
                    .assertNext(
                            ttl -> {
                                assertThat(ttl).isPositive();
                                assertThat(ttl).isLessThanOrEqualTo(300L);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 키는 -2를 반환해야 한다")
        void shouldReturnNegativeTwoForNonExistentKey() {
            // given
            String ipAddress = "0.0.0.0";

            // when & then
            StepVerifier.create(ipBlockRedisRepository.getBlockTtl(ipAddress))
                    .assertNext(ttl -> assertThat(ttl).isEqualTo(-2L))
                    .verifyComplete();
        }
    }
}
