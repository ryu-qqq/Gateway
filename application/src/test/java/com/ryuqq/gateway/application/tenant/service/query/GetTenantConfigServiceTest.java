package com.ryuqq.gateway.application.tenant.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.internal.TenantConfigCoordinator;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.TenantConfigPersistenceException;
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
@DisplayName("GetTenantConfigService 테스트")
class GetTenantConfigServiceTest {

    @Mock private TenantConfigCoordinator tenantConfigCoordinator;

    @InjectMocks private GetTenantConfigService getTenantConfigService;

    @Nested
    @DisplayName("execute() 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("TenantConfig 조회 성공 시 Response 반환")
        void shouldReturnResponseWhenFound() {
            // given
            String tenantId = "tenant-1";
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfig(tenantId);
            GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

            given(tenantConfigCoordinator.findByTenantId(tenantId))
                    .willReturn(Mono.just(tenantConfig));

            // when & then
            StepVerifier.create(getTenantConfigService.execute(query))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.tenantConfig()).isNotNull();
                                assertThat(response.tenantConfig().getTenantIdValue())
                                        .isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("MFA 활성화된 TenantConfig 조회")
        void shouldReturnResponseWithMfa() {
            // given
            String tenantId = "tenant-2";
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfigWithMfa(tenantId);
            GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

            given(tenantConfigCoordinator.findByTenantId(tenantId))
                    .willReturn(Mono.just(tenantConfig));

            // when & then
            StepVerifier.create(getTenantConfigService.execute(query))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.tenantConfig().isMfaRequired()).isTrue();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("조회 오류 시 TenantConfigPersistenceException 발생")
        void shouldThrowExceptionWhenError() {
            // given
            String tenantId = "tenant-3";
            GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

            given(tenantConfigCoordinator.findByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Connection failed")));

            // when & then
            StepVerifier.create(getTenantConfigService.execute(query))
                    .expectError(TenantConfigPersistenceException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getTenantConfig() 테스트")
    class GetTenantConfigTest {

        @Test
        @DisplayName("tenantId로 직접 TenantConfig 조회")
        void shouldReturnTenantConfigDirectly() {
            // given
            String tenantId = "tenant-4";
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfig(tenantId);

            given(tenantConfigCoordinator.findByTenantId(tenantId))
                    .willReturn(Mono.just(tenantConfig));

            // when & then
            StepVerifier.create(getTenantConfigService.getTenantConfig(tenantId))
                    .assertNext(
                            result -> {
                                assertThat(result).isNotNull();
                                assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("조회 오류 시 TenantConfigPersistenceException으로 래핑")
        void shouldWrapErrorWithPersistenceException() {
            // given
            String tenantId = "tenant-5";

            given(tenantConfigCoordinator.findByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Redis error")));

            // when & then
            StepVerifier.create(getTenantConfigService.getTenantConfig(tenantId))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(TenantConfigPersistenceException.class);
                                assertThat(error.getCause()).isInstanceOf(RuntimeException.class);
                            })
                    .verify();
        }
    }
}
