package com.ryuqq.gateway.application.tenant.manager.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryuqq.gateway.application.tenant.internal.TenantConfigCoordinator;
import com.ryuqq.gateway.application.tenant.manager.AuthClientManager;
import com.ryuqq.gateway.application.tenant.manager.TenantConfigCommandManager;
import com.ryuqq.gateway.application.tenant.manager.TenantConfigQueryManager;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.fixture.tenant.TenantConfigFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("unit")
@DisplayName("TenantConfigCoordinator 테스트")
class TenantConfigCoordinatorTest {

    private TenantConfigQueryManager tenantConfigQueryManager;
    private AuthClientManager authClientManager;
    private TenantConfigCommandManager tenantConfigCommandManager;
    private TenantConfigCoordinator tenantConfigCoordinator;

    @BeforeEach
    void setUp() {
        tenantConfigQueryManager = mock(TenantConfigQueryManager.class);
        authClientManager = mock(AuthClientManager.class);
        tenantConfigCommandManager = mock(TenantConfigCommandManager.class);
        tenantConfigCoordinator =
                new TenantConfigCoordinator(
                        tenantConfigQueryManager, authClientManager, tenantConfigCommandManager);
    }

    @Nested
    @DisplayName("findByTenantId() 테스트")
    class FindByTenantIdTest {

        @Test
        @DisplayName("Cache Hit - Redis에서 조회 성공 시 AuthHub 호출하지 않음")
        void shouldReturnFromCacheWhenHit() {
            // given
            String tenantId = "tenant-1";
            TenantConfig cachedConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(tenantConfigQueryManager.findByTenantId(tenantId))
                    .willReturn(Mono.just(cachedConfig));

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigQueryManager).findByTenantId(tenantId);
            verify(authClientManager, never()).fetchTenantConfig(any());
            verify(tenantConfigCommandManager, never()).save(any());
        }

        @Test
        @DisplayName("Cache Miss - AuthHub에서 조회 후 캐싱")
        void shouldFetchFromAuthHubAndCacheWhenMiss() {
            // given
            String tenantId = "tenant-2";
            TenantConfig fetchedConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(tenantConfigQueryManager.findByTenantId(tenantId)).willReturn(Mono.empty());
            given(authClientManager.fetchTenantConfig(tenantId))
                    .willReturn(Mono.just(fetchedConfig));
            given(tenantConfigCommandManager.save(any(TenantConfig.class)))
                    .willReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigQueryManager).findByTenantId(tenantId);
            verify(authClientManager).fetchTenantConfig(tenantId);
            verify(tenantConfigCommandManager).save(any(TenantConfig.class));
        }

        @Test
        @DisplayName("Cache Miss + AuthHub에서도 없음 - 빈 Mono 반환")
        void shouldReturnEmptyWhenNotFoundAnywhere() {
            // given
            String tenantId = "non-existent-tenant";

            given(tenantConfigQueryManager.findByTenantId(tenantId)).willReturn(Mono.empty());
            given(authClientManager.fetchTenantConfig(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId)).verifyComplete();

            verify(tenantConfigQueryManager).findByTenantId(tenantId);
            verify(authClientManager).fetchTenantConfig(tenantId);
            verify(tenantConfigCommandManager, never()).save(any());
        }

        @Test
        @DisplayName("Redis 조회 오류 시 에러 전파")
        void shouldPropagateErrorWhenRedisError() {
            // given
            String tenantId = "tenant-3";

            given(tenantConfigQueryManager.findByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("AuthHub 호출 오류 시 에러 전파")
        void shouldPropagateErrorWhenAuthHubError() {
            // given
            String tenantId = "tenant-4";

            given(tenantConfigQueryManager.findByTenantId(tenantId)).willReturn(Mono.empty());
            given(authClientManager.fetchTenantConfig(tenantId))
                    .willReturn(Mono.error(new RuntimeException("AuthHub unavailable")));

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("캐시 저장 실패 시 에러 전파")
        void shouldPropagateErrorWhenCacheSaveFails() {
            // given
            String tenantId = "tenant-5";
            TenantConfig fetchedConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(tenantConfigQueryManager.findByTenantId(tenantId)).willReturn(Mono.empty());
            given(authClientManager.fetchTenantConfig(tenantId))
                    .willReturn(Mono.just(fetchedConfig));
            given(tenantConfigCommandManager.save(any(TenantConfig.class)))
                    .willReturn(Mono.error(new RuntimeException("Cache save failed")));

            // when & then
            StepVerifier.create(tenantConfigCoordinator.findByTenantId(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
