package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.entity.SessionConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantRateLimitConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.TenantConfigMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
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
 * TenantConfigQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantConfigQueryAdapter 단위 테스트")
class TenantConfigQueryAdapterTest {

    @Mock private TenantConfigRedisRepository tenantConfigRedisRepository;

    @Mock private TenantConfigMapper tenantConfigMapper;

    private TenantConfigQueryAdapter tenantConfigQueryAdapter;

    @BeforeEach
    void setUp() {
        tenantConfigQueryAdapter =
                new TenantConfigQueryAdapter(tenantConfigRedisRepository, tenantConfigMapper);
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
    @DisplayName("findByTenantId 메서드")
    class FindByTenantIdTest {

        @Test
        @DisplayName("Redis에서 Tenant Config를 조회해야 한다")
        void shouldFindTenantConfigFromRedis() {
            // given
            String tenantId = "tenant-123";
            TenantConfigEntity entity = createTestTenantConfigEntity(tenantId);
            TenantConfig tenantConfig = createTestTenantConfig(tenantId);

            given(tenantConfigRedisRepository.findByTenantId(eq(tenantId)))
                    .willReturn(Mono.just(entity));
            given(tenantConfigMapper.toTenantConfig(entity)).willReturn(tenantConfig);

            // when
            Mono<TenantConfig> result = tenantConfigQueryAdapter.findByTenantId(tenantId);

            // then
            StepVerifier.create(result).expectNext(tenantConfig).verifyComplete();

            then(tenantConfigRedisRepository).should().findByTenantId(tenantId);
            then(tenantConfigMapper).should().toTenantConfig(entity);
        }

        @Test
        @DisplayName("Cache Miss 시 empty Mono를 반환해야 한다")
        void shouldReturnEmptyMonoWhenCacheMiss() {
            // given
            String tenantId = "non-existent-tenant";

            given(tenantConfigRedisRepository.findByTenantId(eq(tenantId)))
                    .willReturn(Mono.empty());

            // when
            Mono<TenantConfig> result = tenantConfigQueryAdapter.findByTenantId(tenantId);

            // then
            StepVerifier.create(result).verifyComplete();

            then(tenantConfigMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapRedisExceptionInRuntimeException() {
            // given
            String tenantId = "tenant-error";

            given(tenantConfigRedisRepository.findByTenantId(eq(tenantId)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<TenantConfig> result = tenantConfigQueryAdapter.findByTenantId(tenantId);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains(
                                                            "Failed to get tenant config from"
                                                                    + " Redis")
                                            && throwable.getMessage().contains(tenantId))
                    .verify();
        }
    }
}
