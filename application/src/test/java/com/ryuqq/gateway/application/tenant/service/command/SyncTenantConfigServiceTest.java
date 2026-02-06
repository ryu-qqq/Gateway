package com.ryuqq.gateway.application.tenant.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.manager.TenantConfigCommandManager;
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
@DisplayName("SyncTenantConfigService 테스트")
class SyncTenantConfigServiceTest {

    @Mock private TenantConfigCommandManager tenantConfigCommandManager;

    @InjectMocks private SyncTenantConfigService syncTenantConfigService;

    @Nested
    @DisplayName("execute() 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("캐시 무효화 성공 시 success 응답 반환")
        void shouldReturnSuccessWhenCacheInvalidationSucceeds() {
            // given
            String tenantId = "tenant-1";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            given(tenantConfigCommandManager.deleteByTenantId(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.success()).isTrue();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigCommandManager).deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 failure 응답 반환")
        void shouldReturnFailureWhenCacheInvalidationFails() {
            // given
            String tenantId = "tenant-2";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            given(tenantConfigCommandManager.deleteByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.success()).isFalse();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();

            verify(tenantConfigCommandManager).deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("다양한 tenantId로 캐시 무효화")
        void shouldHandleVariousTenantIds() {
            // given
            String tenantId = "tenant-12345-abcde";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            given(tenantConfigCommandManager.deleteByTenantId(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncTenantConfigService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.success()).isTrue();
                                assertThat(response.tenantId()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("특수문자 포함 tenantId로 캐시 무효화")
        void shouldHandleSpecialCharacterTenantId() {
            // given
            String tenantId = "tenant_test-123.abc";
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            given(tenantConfigCommandManager.deleteByTenantId(tenantId)).willReturn(Mono.empty());

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
