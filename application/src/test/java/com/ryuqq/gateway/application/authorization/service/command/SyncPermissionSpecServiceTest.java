package com.ryuqq.gateway.application.authorization.service.command;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import com.ryuqq.gateway.application.authorization.manager.PermissionSpecCommandManager;
import java.util.List;
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
@DisplayName("SyncPermissionSpecService 테스트")
class SyncPermissionSpecServiceTest {

    @Mock private PermissionSpecCommandManager permissionSpecCommandManager;

    private SyncPermissionSpecService syncPermissionSpecService;

    @BeforeEach
    void setUp() {
        syncPermissionSpecService = new SyncPermissionSpecService(permissionSpecCommandManager);
    }

    @Nested
    @DisplayName("execute() 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("정상적인 Permission Spec 동기화")
        void shouldSyncPermissionSpecSuccessfully() {
            // given
            Long version = 123L;
            List<String> changedServices = List.of("user-service", "order-service");
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(version, changedServices);

            when(permissionSpecCommandManager.invalidate()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command)).verifyComplete();

            verify(permissionSpecCommandManager).invalidate();
        }

        @Test
        @DisplayName("변경된 서비스가 없는 경우에도 정상 동작")
        void shouldSyncPermissionSpecWithoutChangedServices() {
            // given
            Long version = 456L;
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(version);

            when(permissionSpecCommandManager.invalidate()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command)).verifyComplete();

            verify(permissionSpecCommandManager).invalidate();
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 에러 전파")
        void shouldPropagateErrorWhenInvalidationFails() {
            // given
            Long version = 789L;
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(version);
            RuntimeException expectedException = new RuntimeException("Cache invalidation failed");

            when(permissionSpecCommandManager.invalidate())
                    .thenReturn(Mono.error(expectedException));

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecCommandManager).invalidate();
        }

        @Test
        @DisplayName("빈 변경 서비스 리스트로 동기화")
        void shouldSyncPermissionSpecWithEmptyChangedServices() {
            // given
            Long version = 999L;
            List<String> emptyServices = List.of();
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(version, emptyServices);

            when(permissionSpecCommandManager.invalidate()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command)).verifyComplete();

            verify(permissionSpecCommandManager).invalidate();
        }

        @Test
        @DisplayName("다수의 변경된 서비스가 있는 경우")
        void shouldSyncPermissionSpecWithMultipleChangedServices() {
            // given
            Long version = 555L;
            List<String> multipleServices =
                    List.of(
                            "user-service",
                            "order-service",
                            "payment-service",
                            "notification-service",
                            "inventory-service");
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(version, multipleServices);

            when(permissionSpecCommandManager.invalidate()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command)).verifyComplete();

            verify(permissionSpecCommandManager).invalidate();
        }
    }

    @Nested
    @DisplayName("로깅 테스트")
    class LoggingTest {

        @Test
        @DisplayName("성공 시 로그 출력 확인")
        void shouldLogSuccessfulSync() {
            // given
            Long version = 100L;
            List<String> changedServices = List.of("test-service");
            SyncPermissionSpecCommand command =
                    SyncPermissionSpecCommand.of(version, changedServices);

            when(permissionSpecCommandManager.invalidate()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command)).verifyComplete();

            verify(permissionSpecCommandManager).invalidate();
        }

        @Test
        @DisplayName("실패 시 에러 로그 출력 후 에러 전파")
        void shouldLogErrorAndPropagateException() {
            // given
            Long version = 200L;
            SyncPermissionSpecCommand command = SyncPermissionSpecCommand.of(version);
            RuntimeException expectedException = new RuntimeException("Test error");

            when(permissionSpecCommandManager.invalidate())
                    .thenReturn(Mono.error(expectedException));

            // when & then
            StepVerifier.create(syncPermissionSpecService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecCommandManager).invalidate();
        }
    }
}
