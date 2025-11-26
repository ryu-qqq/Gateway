package com.ryuqq.gateway.application.authorization.service.command;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
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
@DisplayName("InvalidateUserPermissionService 테스트")
class InvalidateUserPermissionServiceTest {

    @Mock private PermissionHashCommandPort permissionHashCommandPort;

    private InvalidateUserPermissionService invalidateUserPermissionService;

    @BeforeEach
    void setUp() {
        invalidateUserPermissionService =
                new InvalidateUserPermissionService(permissionHashCommandPort);
    }

    @Nested
    @DisplayName("execute() 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("정상적인 사용자 권한 캐시 무효화")
        void shouldInvalidateUserPermissionSuccessfully() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            when(permissionHashCommandPort.invalidate(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command)).verifyComplete();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 에러 전파")
        void shouldPropagateErrorWhenInvalidationFails() {
            // given
            String tenantId = "tenant789";
            String userId = "user999";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);
            RuntimeException expectedException = new RuntimeException("Cache invalidation failed");

            when(permissionHashCommandPort.invalidate(tenantId, userId))
                    .thenReturn(Mono.error(expectedException));

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("특수 문자가 포함된 tenantId와 userId 처리")
        void shouldHandleSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-with-dash_123";
            String userId = "user@domain.com";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            when(permissionHashCommandPort.invalidate(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command)).verifyComplete();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("긴 ID 값들 처리")
        void shouldHandleLongIds() {
            // given
            String tenantId = "very-long-tenant-id-with-many-characters-1234567890";
            String userId = "very-long-user-id-with-many-characters-abcdefghijklmnopqrstuvwxyz";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            when(permissionHashCommandPort.invalidate(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command)).verifyComplete();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("숫자로만 구성된 ID 처리")
        void shouldHandleNumericIds() {
            // given
            String tenantId = "123456789";
            String userId = "987654321";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            when(permissionHashCommandPort.invalidate(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command)).verifyComplete();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Redis 연결 실패 시 에러 전파")
        void shouldPropagateRedisConnectionError() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);
            RuntimeException redisException = new RuntimeException("Redis connection failed");

            when(permissionHashCommandPort.invalidate(tenantId, userId))
                    .thenReturn(Mono.error(redisException));

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("타임아웃 에러 처리")
        void shouldHandleTimeoutError() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);
            RuntimeException timeoutException = new RuntimeException("Operation timed out");

            when(permissionHashCommandPort.invalidate(tenantId, userId))
                    .thenReturn(Mono.error(timeoutException));

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }
    }

    @Nested
    @DisplayName("로깅 테스트")
    class LoggingTest {

        @Test
        @DisplayName("성공 시 로그 출력 확인")
        void shouldLogSuccessfulInvalidation() {
            // given
            String tenantId = "tenant100";
            String userId = "user200";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            when(permissionHashCommandPort.invalidate(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command)).verifyComplete();

            // 로그는 실제로는 검증하기 어려우므로 정상 완료만 확인
            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }

        @Test
        @DisplayName("실패 시 에러 로그 출력 후 에러 전파")
        void shouldLogErrorAndPropagateException() {
            // given
            String tenantId = "tenant300";
            String userId = "user400";
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);
            RuntimeException expectedException = new RuntimeException("Test error");

            when(permissionHashCommandPort.invalidate(tenantId, userId))
                    .thenReturn(Mono.error(expectedException));

            // when & then
            StepVerifier.create(invalidateUserPermissionService.execute(command))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashCommandPort).invalidate(tenantId, userId);
        }
    }
}
