package com.ryuqq.gateway.application.tenant.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ryuqq.gateway.application.tenant.port.out.query.TenantConfigQueryPort;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.fixture.tenant.TenantConfigFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("TenantConfigQueryManager 테스트")
class TenantConfigQueryManagerTest {

    @Mock private TenantConfigQueryPort tenantConfigQueryPort;

    @InjectMocks private TenantConfigQueryManager tenantConfigQueryManager;

    @Nested
    @DisplayName("findByTenantId() 테스트")
    class FindByTenantIdTest {

        @Test
        @DisplayName("Redis Cache Hit 시 TenantConfig 반환")
        void shouldReturnTenantConfigWhenCacheHit() {
            // given
            String tenantId = "tenant-1";
            TenantConfig cachedConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(tenantConfigQueryPort.findByTenantId(tenantId))
                    .willReturn(Mono.just(cachedConfig));

            // when & then
            StepVerifier.create(tenantConfigQueryManager.findByTenantId(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis Cache Miss 시 빈 Mono 반환")
        void shouldReturnEmptyWhenCacheMiss() {
            // given
            String tenantId = "tenant-2";

            given(tenantConfigQueryPort.findByTenantId(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantConfigQueryManager.findByTenantId(tenantId)).verifyComplete();
        }

        @Test
        @DisplayName("Redis 오류 시 에러 전파")
        void shouldPropagateErrorWhenRedisError() {
            // given
            String tenantId = "tenant-3";

            given(tenantConfigQueryPort.findByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(tenantConfigQueryManager.findByTenantId(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
