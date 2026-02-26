package com.ryuqq.gateway.application.tenant.manager;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
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
@DisplayName("TenantConfigCommandManager 테스트")
class TenantConfigCommandManagerTest {

    @Mock private TenantConfigCommandPort tenantConfigCommandPort;

    @InjectMocks private TenantConfigCommandManager tenantConfigCommandManager;

    @Nested
    @DisplayName("save() 테스트")
    class SaveTest {

        @Test
        @DisplayName("TenantConfig 저장 성공")
        void shouldSaveTenantConfig() {
            // given
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfig("tenant-1");

            given(tenantConfigCommandPort.save(tenantConfig)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantConfigCommandManager.save(tenantConfig)).verifyComplete();

            verify(tenantConfigCommandPort).save(tenantConfig);
        }

        @Test
        @DisplayName("저장 실패 시 에러 전파")
        void shouldPropagateErrorWhenSaveFails() {
            // given
            TenantConfig tenantConfig = TenantConfigFixture.aTenantConfig("tenant-2");

            given(tenantConfigCommandPort.save(tenantConfig))
                    .willReturn(Mono.error(new RuntimeException("Redis write failed")));

            // when & then
            StepVerifier.create(tenantConfigCommandManager.save(tenantConfig))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteByTenantId() 테스트")
    class DeleteByTenantIdTest {

        @Test
        @DisplayName("캐시 무효화 성공")
        void shouldDeleteTenantConfig() {
            // given
            String tenantId = "tenant-1";

            given(tenantConfigCommandPort.deleteByTenantId(tenantId)).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantConfigCommandManager.deleteByTenantId(tenantId))
                    .verifyComplete();

            verify(tenantConfigCommandPort).deleteByTenantId(tenantId);
        }

        @Test
        @DisplayName("삭제 실패 시 에러 전파")
        void shouldPropagateErrorWhenDeleteFails() {
            // given
            String tenantId = "tenant-2";

            given(tenantConfigCommandPort.deleteByTenantId(tenantId))
                    .willReturn(Mono.error(new RuntimeException("Redis delete failed")));

            // when & then
            StepVerifier.create(tenantConfigCommandManager.deleteByTenantId(tenantId))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
