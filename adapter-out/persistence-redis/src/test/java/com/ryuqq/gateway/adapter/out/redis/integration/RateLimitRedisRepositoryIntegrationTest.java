package com.ryuqq.gateway.adapter.out.redis.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * RateLimitRedisRepository 통합 테스트
 *
 * <p>TestContainers Redis를 사용하여 실제 Rate Limit 동작을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("RateLimitRedisRepository 통합 테스트")
class RateLimitRedisRepositoryIntegrationTest extends RedisTestSupport {

    @Autowired
    private RateLimitRedisRepository rateLimitRedisRepository;

    @Nested
    @DisplayName("incrementAndExpire 메서드")
    class IncrementAndExpireTest {

        @Test
        @DisplayName("카운터 증가 및 TTL 설정이 원자적으로 동작해야 한다")
        void shouldIncrementAndSetTtlAtomically() {
            // given
            String key = "gateway:rate_limit:ip:192.168.1.1";
            Duration ttl = Duration.ofSeconds(60);

            // when & then - 첫 번째 증가
            StepVerifier.create(rateLimitRedisRepository.incrementAndExpire(key, ttl))
                    .assertNext(count -> assertThat(count).isEqualTo(1L))
                    .verifyComplete();

            // when & then - 두 번째 증가
            StepVerifier.create(rateLimitRedisRepository.incrementAndExpire(key, ttl))
                    .assertNext(count -> assertThat(count).isEqualTo(2L))
                    .verifyComplete();

            // TTL이 설정되었는지 확인
            StepVerifier.create(rateLimitRedisRepository.getTtl(key))
                    .assertNext(
                            remainingTtl -> {
                                assertThat(remainingTtl).isPositive();
                                assertThat(remainingTtl).isLessThanOrEqualTo(60L);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("여러 번 증가해도 TTL은 최초 설정값을 유지해야 한다")
        void shouldKeepOriginalTtlOnSubsequentIncrements() {
            // given
            String key = "gateway:rate_limit:user:user-123";
            Duration ttl = Duration.ofSeconds(30);

            // when - 여러 번 증가
            for (int i = 0; i < 5; i++) {
                rateLimitRedisRepository.incrementAndExpire(key, ttl).block();
            }

            // then
            StepVerifier.create(rateLimitRedisRepository.getCount(key))
                    .assertNext(count -> assertThat(count).isEqualTo(5L))
                    .verifyComplete();

            StepVerifier.create(rateLimitRedisRepository.getTtl(key))
                    .assertNext(
                            remainingTtl -> {
                                assertThat(remainingTtl).isPositive();
                                assertThat(remainingTtl).isLessThanOrEqualTo(30L);
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getCount 메서드")
    class GetCountTest {

        @Test
        @DisplayName("존재하지 않는 키는 0을 반환해야 한다")
        void shouldReturnZeroForNonExistentKey() {
            // given
            String key = "gateway:rate_limit:non-existent";

            // when & then
            StepVerifier.create(rateLimitRedisRepository.getCount(key))
                    .assertNext(count -> assertThat(count).isZero())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    class DeleteTest {

        @Test
        @DisplayName("존재하는 키를 삭제하면 true를 반환해야 한다")
        void shouldReturnTrueWhenDeletingExistingKey() {
            // given
            String key = "gateway:rate_limit:to-delete";
            rateLimitRedisRepository.incrementAndExpire(key, Duration.ofSeconds(60)).block();

            // when & then
            StepVerifier.create(rateLimitRedisRepository.delete(key))
                    .assertNext(deleted -> assertThat(deleted).isTrue())
                    .verifyComplete();

            // 삭제 후 카운트가 0인지 확인
            StepVerifier.create(rateLimitRedisRepository.getCount(key))
                    .assertNext(count -> assertThat(count).isZero())
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 키 삭제 시 false를 반환해야 한다")
        void shouldReturnFalseWhenDeletingNonExistentKey() {
            // given
            String key = "gateway:rate_limit:never-existed";

            // when & then
            StepVerifier.create(rateLimitRedisRepository.delete(key))
                    .assertNext(deleted -> assertThat(deleted).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("exists 메서드")
    class ExistsTest {

        @Test
        @DisplayName("존재하는 키는 true를 반환해야 한다")
        void shouldReturnTrueForExistingKey() {
            // given
            String key = "gateway:rate_limit:existing";
            rateLimitRedisRepository.incrementAndExpire(key, Duration.ofSeconds(60)).block();

            // when & then
            StepVerifier.create(rateLimitRedisRepository.exists(key))
                    .assertNext(exists -> assertThat(exists).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 키는 false를 반환해야 한다")
        void shouldReturnFalseForNonExistentKey() {
            // given
            String key = "gateway:rate_limit:non-existing";

            // when & then
            StepVerifier.create(rateLimitRedisRepository.exists(key))
                    .assertNext(exists -> assertThat(exists).isFalse())
                    .verifyComplete();
        }
    }
}
