package com.ryuqq.gateway.application.tenant.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ryuqq.gateway.application.tenant.port.out.client.AuthClient;
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
@DisplayName("AuthClientManager 테스트")
class AuthClientManagerTest {

    @Mock private AuthClient authClient;

    @InjectMocks private AuthClientManager authClientManager;

    @Nested
    @DisplayName("fetchTenantConfig() 테스트")
    class FetchTenantConfigTest {

        @Test
        @DisplayName("AuthHub API 호출 성공 시 TenantConfig 반환")
        void shouldReturnTenantConfigWhenApiCallSucceeds() {
            // given
            String tenantId = "tenant-1";
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(authClient.fetchTenantConfig(tenantId)).willReturn(Mono.just(tenantConfig));

            // when & then
            StepVerifier.create(authClientManager.fetchTenantConfig(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("AuthHub API 호출 실패 시 에러 전파")
        void shouldPropagateErrorWhenApiCallFails() {
            // given
            String tenantId = "tenant-2";

            given(authClient.fetchTenantConfig(tenantId))
                    .willReturn(Mono.error(new RuntimeException("AuthHub unavailable")));

            // when & then
            StepVerifier.create(authClientManager.fetchTenantConfig(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("존재하지 않는 테넌트 조회 시 빈 Mono 반환")
        void shouldReturnEmptyWhenTenantNotFound() {
            // given
            String tenantId = "non-existent-tenant";

            given(authClient.fetchTenantConfig(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(authClientManager.fetchTenantConfig(tenantId)).verifyComplete();
        }
    }
}
