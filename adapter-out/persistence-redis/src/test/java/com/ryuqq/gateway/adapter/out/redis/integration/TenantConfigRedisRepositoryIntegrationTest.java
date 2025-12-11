package com.ryuqq.gateway.adapter.out.redis.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.redis.entity.SessionConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantRateLimitConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * TenantConfigRedisRepository 통합 테스트
 *
 * <p>TestContainers Redis를 사용하여 실제 Tenant Config 동작을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("TenantConfigRedisRepository 통합 테스트")
class TenantConfigRedisRepositoryIntegrationTest extends RedisTestSupport {

    @Autowired private TenantConfigRedisRepository tenantConfigRedisRepository;

    private TenantConfigEntity createTestEntity(String tenantId) {
        SessionConfigEntity sessionConfig = new SessionConfigEntity(5, 900L, 604800L);
        TenantRateLimitConfigEntity rateLimitConfig = new TenantRateLimitConfigEntity(10, 5);
        return new TenantConfigEntity(
                tenantId,
                true,
                Set.of("KAKAO", "GOOGLE"),
                Map.of("ADMIN", Set.of("READ", "WRITE", "DELETE"), "USER", Set.of("READ")),
                sessionConfig,
                rateLimitConfig);
    }

    @Nested
    @DisplayName("save 메서드")
    class SaveTest {

        @Test
        @DisplayName("TenantConfig를 저장하고 조회할 수 있어야 한다")
        void shouldSaveAndRetrieveTenantConfig() {
            // given
            String tenantId = "tenant-001";
            TenantConfigEntity entity = createTestEntity(tenantId);
            Duration ttl = Duration.ofHours(1);

            // when
            tenantConfigRedisRepository.save(tenantId, entity, ttl).block();

            // then
            StepVerifier.create(tenantConfigRedisRepository.findByTenantId(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result.getTenantId()).isEqualTo(tenantId);
                                assertThat(result.isMfaRequired()).isTrue();
                                assertThat(result.getAllowedSocialLogins())
                                        .containsExactlyInAnyOrder("KAKAO", "GOOGLE");
                                assertThat(result.getSessionConfig().getMaxActiveSessions())
                                        .isEqualTo(5);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("기본 TTL(1시간)로 저장할 수 있어야 한다")
        void shouldSaveWithDefaultTtl() {
            // given
            String tenantId = "tenant-default-ttl";
            TenantConfigEntity entity = createTestEntity(tenantId);

            // when
            tenantConfigRedisRepository.save(tenantId, entity).block();

            // then
            StepVerifier.create(tenantConfigRedisRepository.existsByTenantId(tenantId))
                    .assertNext(exists -> assertThat(exists).isTrue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByTenantId 메서드")
    class FindByTenantIdTest {

        @Test
        @DisplayName("존재하는 Tenant Config를 조회할 수 있어야 한다")
        void shouldFindExistingTenantConfig() {
            // given
            String tenantId = "tenant-find-001";
            TenantConfigEntity entity = createTestEntity(tenantId);
            tenantConfigRedisRepository.save(tenantId, entity).block();

            // when & then
            StepVerifier.create(tenantConfigRedisRepository.findByTenantId(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 Tenant Config는 empty를 반환해야 한다")
        void shouldReturnEmptyForNonExistentTenantConfig() {
            // given
            String tenantId = "tenant-non-existent";

            // when & then
            StepVerifier.create(tenantConfigRedisRepository.findByTenantId(tenantId))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteByTenantId 메서드")
    class DeleteByTenantIdTest {

        @Test
        @DisplayName("존재하는 Tenant Config를 삭제하면 true를 반환해야 한다")
        void shouldReturnTrueWhenDeletingExistingConfig() {
            // given
            String tenantId = "tenant-to-delete";
            TenantConfigEntity entity = createTestEntity(tenantId);
            tenantConfigRedisRepository.save(tenantId, entity).block();

            // when
            StepVerifier.create(tenantConfigRedisRepository.deleteByTenantId(tenantId))
                    .assertNext(deleted -> assertThat(deleted).isTrue())
                    .verifyComplete();

            // then - 삭제 확인
            StepVerifier.create(tenantConfigRedisRepository.existsByTenantId(tenantId))
                    .assertNext(exists -> assertThat(exists).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 Tenant Config 삭제 시 false를 반환해야 한다")
        void shouldReturnFalseWhenDeletingNonExistentConfig() {
            // given
            String tenantId = "tenant-never-existed";

            // when & then
            StepVerifier.create(tenantConfigRedisRepository.deleteByTenantId(tenantId))
                    .assertNext(deleted -> assertThat(deleted).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("existsByTenantId 메서드")
    class ExistsByTenantIdTest {

        @Test
        @DisplayName("존재하는 Tenant Config는 true를 반환해야 한다")
        void shouldReturnTrueForExistingConfig() {
            // given
            String tenantId = "tenant-exists";
            TenantConfigEntity entity = createTestEntity(tenantId);
            tenantConfigRedisRepository.save(tenantId, entity).block();

            // when & then
            StepVerifier.create(tenantConfigRedisRepository.existsByTenantId(tenantId))
                    .assertNext(exists -> assertThat(exists).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 Tenant Config는 false를 반환해야 한다")
        void shouldReturnFalseForNonExistentConfig() {
            // given
            String tenantId = "tenant-not-exists";

            // when & then
            StepVerifier.create(tenantConfigRedisRepository.existsByTenantId(tenantId))
                    .assertNext(exists -> assertThat(exists).isFalse())
                    .verifyComplete();
        }
    }
}
