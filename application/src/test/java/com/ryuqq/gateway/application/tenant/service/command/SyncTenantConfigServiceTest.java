package com.ryuqq.gateway.application.tenant.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.dto.response.SyncTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncTenantConfigService 테스트")
class SyncTenantConfigServiceTest {

    @Mock private TenantConfigCommandPort tenantConfigCommandPort;

    private SyncTenantConfigService syncTenantConfigService;

    @BeforeEach
    void setUp() {
        syncTenantConfigService = new SyncTenantConfigService(tenantConfigCommandPort);
    }

    @Nested
    @DisplayName("execute() 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("캐시 무효화 성공 시 success 응답 반환")
        void shouldReturnSuccessWhenCacheInvalidationSucceeds() {
            // given
            String tenantId = "tenant-1";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            when(tenantConfigCommandPort.deleteByTenantId(tenantId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.success()).isTrue();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigCommandPort).deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 failure 응답 반환")
        void shouldReturnFailureWhenCacheInvalidationFails() {
            // given
            String tenantId = "tenant-2";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            when(tenantConfigCommandPort.deleteByTenantId(tenantId))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.success()).isFalse();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigCommandPort).deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("다양한 tenantId로 캐시 무효화")
        void shouldHandleVariousTenantIds() {
            // given
            String tenantId = "tenant-12345";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            when(tenantConfigCommandPort.deleteByTenantId(tenantId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.success()).isTrue();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }
    }
}
