package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.entity.SessionConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantRateLimitConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.TenantConfigMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.TenantConfigPersistenceException;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
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
 * TenantConfigCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantConfigCommandAdapter 단위 테스트")
class TenantConfigCommandAdapterTest {

    @Mock private TenantConfigRedisRepository tenantConfigRedisRepository;

    @Mock private TenantConfigMapper tenantConfigMapper;

    private TenantConfigCommandAdapter tenantConfigCommandAdapter;

    @BeforeEach
    void setUp() {
        tenantConfigCommandAdapter =
                new TenantConfigCommandAdapter(tenantConfigRedisRepository, tenantConfigMapper);
    }

    private TenantConfig createTestTenantConfig(String tenantIdStr) {
        TenantId tenantId = TenantId.from(tenantIdStr);
        SessionConfig sessionConfig =
                SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
        TenantRateLimitConfig rateLimitConfig = TenantRateLimitConfig.of(10, 5);
        Set<SocialProvider> allowedSocialLogins =
                Set.of(SocialProvider.KAKAO, SocialProvider.GOOGLE);
        Map<String, Set<String>> roleHierarchy =
                Map.of(
                        "ADMIN", Set.of("READ", "WRITE", "DELETE"),
                        "USER", Set.of("READ"));

        return TenantConfig.of(
                tenantId, true, allowedSocialLogins, roleHierarchy, sessionConfig, rateLimitConfig);
    }

    private TenantConfigEntity createTestTenantConfigEntity(String tenantIdStr) {
        SessionConfigEntity sessionConfigEntity = new SessionConfigEntity(5, 900L, 604800L);
        TenantRateLimitConfigEntity rateLimitConfigEntity = new TenantRateLimitConfigEntity(10, 5);
        return new TenantConfigEntity(
                tenantIdStr,
                true,
                Set.of("KAKAO", "GOOGLE"),
                Map.of("ADMIN", Set.of("READ", "WRITE", "DELETE"), "USER", Set.of("READ")),
                sessionConfigEntity,
                rateLimitConfigEntity);
    }

    @Nested
    @DisplayName("save 메서드")
    class SaveTest {

        @Test
        @DisplayName("Tenant Config를 Redis에 저장해야 한다")
        void shouldSaveTenantConfigToRedis() {
            // given
            TenantConfig tenantConfig = createTestTenantConfig("tenant-123");
            TenantConfigEntity entity = createTestTenantConfigEntity("tenant-123");

            given(tenantConfigMapper.toTenantConfigEntity(tenantConfig)).willReturn(entity);
            given(tenantConfigRedisRepository.save(eq("tenant-123"), eq(entity)))
                    .willReturn(Mono.empty());

            // when
            Mono<Void> result = tenantConfigCommandAdapter.save(tenantConfig);

            // then
            StepVerifier.create(result).verifyComplete();

            then(tenantConfigMapper).should().toTenantConfigEntity(tenantConfig);
            then(tenantConfigRedisRepository).should().save("tenant-123", entity);
        }

        @Test
        @DisplayName("Redis 에러 발생 시 TenantConfigPersistenceException으로 래핑해야 한다")
        void shouldWrapRedisExceptionInTenantConfigPersistenceException() {
            // given
            TenantConfig tenantConfig = createTestTenantConfig("tenant-456");
            TenantConfigEntity entity = createTestTenantConfigEntity("tenant-456");

            given(tenantConfigMapper.toTenantConfigEntity(tenantConfig)).willReturn(entity);
            given(tenantConfigRedisRepository.save(eq("tenant-456"), any()))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result = tenantConfigCommandAdapter.save(tenantConfig);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof TenantConfigPersistenceException
                                            && throwable.getMessage().contains("tenant-456")
                                            && throwable.getMessage().contains("save"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteByTenantId 메서드")
    class DeleteByTenantIdTest {

        @Test
        @DisplayName("Tenant Config를 Redis에서 삭제해야 한다")
        void shouldDeleteTenantConfigFromRedis() {
            // given
            String tenantId = "tenant-123";

            given(tenantConfigRedisRepository.deleteByTenantId(eq(tenantId)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = tenantConfigCommandAdapter.deleteByTenantId(tenantId);

            // then
            StepVerifier.create(result).verifyComplete();

            then(tenantConfigRedisRepository).should().deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("존재하지 않는 Tenant 삭제 시도 시 정상 완료해야 한다")
        void shouldCompleteSuccessfullyWhenTenantNotExists() {
            // given
            String tenantId = "non-existent-tenant";

            given(tenantConfigRedisRepository.deleteByTenantId(eq(tenantId)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Void> result = tenantConfigCommandAdapter.deleteByTenantId(tenantId);

            // then
            StepVerifier.create(result).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 TenantConfigPersistenceException으로 래핑해야 한다")
        void shouldWrapRedisExceptionInTenantConfigPersistenceException() {
            // given
            String tenantId = "tenant-error";

            given(tenantConfigRedisRepository.deleteByTenantId(eq(tenantId)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result = tenantConfigCommandAdapter.deleteByTenantId(tenantId);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof TenantConfigPersistenceException
                                            && throwable.getMessage().contains(tenantId)
                                            && throwable.getMessage().contains("delete"))
                    .verify();
        }
    }
}
