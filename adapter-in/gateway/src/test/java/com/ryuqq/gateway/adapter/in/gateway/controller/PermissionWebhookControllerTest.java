package com.ryuqq.gateway.adapter.in.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import com.ryuqq.gateway.application.authorization.port.in.command.InvalidateUserPermissionUseCase;
import com.ryuqq.gateway.application.authorization.port.in.command.SyncPermissionSpecUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionWebhookController 테스트")
class PermissionWebhookControllerTest {

    @Mock private SyncPermissionSpecUseCase syncPermissionSpecUseCase;

    @Mock private InvalidateUserPermissionUseCase invalidateUserPermissionUseCase;

    private PermissionWebhookController controller;

    @BeforeEach
    void setUp() {
        controller =
                new PermissionWebhookController(
                        syncPermissionSpecUseCase, invalidateUserPermissionUseCase);
    }

    @Nested
    @DisplayName("syncPermissionSpec() 테스트")
    class SyncPermissionSpecTest {

        @Test
        @DisplayName("Permission Spec 동기화 성공")
        void shouldSyncPermissionSpecSuccessfully() {
            // given
            Long version = 123L;
            List<String> changedServices = List.of("user-service", "order-service");
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, changedServices);

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(
                            response ->
                                    response.getStatusCode() == HttpStatus.OK
                                            && response.getBody() == null)
                    .verifyComplete();

            verify(syncPermissionSpecUseCase).execute(any(SyncPermissionSpecCommand.class));
        }

        @Test
        @DisplayName("빈 변경 서비스 리스트로 동기화")
        void shouldSyncWithEmptyChangedServices() {
            // given
            Long version = 456L;
            List<String> emptyServices = List.of();
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, emptyServices);

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(syncPermissionSpecUseCase).execute(any(SyncPermissionSpecCommand.class));
        }

        @Test
        @DisplayName("null 변경 서비스로 동기화")
        void shouldSyncWithNullChangedServices() {
            // given
            Long version = 789L;
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, null);

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(syncPermissionSpecUseCase).execute(any(SyncPermissionSpecCommand.class));
        }

        @Test
        @DisplayName("SyncPermissionSpecCommand가 올바르게 생성됨")
        void shouldCreateCorrectSyncPermissionSpecCommand() {
            // given
            Long version = 999L;
            List<String> changedServices = List.of("payment-service", "notification-service");
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, changedServices);

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());

            // when
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            // then
            ArgumentCaptor<SyncPermissionSpecCommand> commandCaptor =
                    ArgumentCaptor.forClass(SyncPermissionSpecCommand.class);
            verify(syncPermissionSpecUseCase).execute(commandCaptor.capture());

            SyncPermissionSpecCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.version()).isEqualTo(version);
            assertThat(capturedCommand.changedServices()).isEqualTo(changedServices);
        }

        @Test
        @DisplayName("UseCase 실행 중 에러 발생 시 500 Internal Server Error 응답")
        void shouldReturnInternalServerErrorWhenUseCaseFails() {
            // given
            Long version = 111L;
            List<String> changedServices = List.of("failing-service");
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, changedServices);

            RuntimeException exception = new RuntimeException("Sync failed");
            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(
                            response ->
                                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                    .verifyComplete();

            verify(syncPermissionSpecUseCase).execute(any(SyncPermissionSpecCommand.class));
        }

        @Test
        @DisplayName("다수의 변경된 서비스가 있는 경우")
        void shouldHandleMultipleChangedServices() {
            // given
            Long version = 555L;
            List<String> multipleServices =
                    List.of(
                            "user-service",
                            "order-service",
                            "payment-service",
                            "notification-service",
                            "inventory-service");
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, multipleServices);

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.syncPermissionSpec(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            ArgumentCaptor<SyncPermissionSpecCommand> commandCaptor =
                    ArgumentCaptor.forClass(SyncPermissionSpecCommand.class);
            verify(syncPermissionSpecUseCase).execute(commandCaptor.capture());

            SyncPermissionSpecCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.changedServices()).hasSize(5);
            assertThat(capturedCommand.changedServices())
                    .containsExactlyElementsOf(multipleServices);
        }
    }

    @Nested
    @DisplayName("invalidateUserPermission() 테스트")
    class InvalidateUserPermissionTest {

        @Test
        @DisplayName("사용자 권한 무효화 성공")
        void shouldInvalidateUserPermissionSuccessfully() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(
                            response ->
                                    response.getStatusCode() == HttpStatus.OK
                                            && response.getBody() == null)
                    .verifyComplete();

            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }

        @Test
        @DisplayName("특수 문자가 포함된 tenantId와 userId로 무효화")
        void shouldInvalidateWithSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-with-dash_789";
            String userId = "user@domain.com";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }

        @Test
        @DisplayName("긴 ID 값들로 무효화")
        void shouldInvalidateWithLongIds() {
            // given
            String tenantId = "very-long-tenant-id-with-many-characters-1234567890";
            String userId = "very-long-user-id-with-many-characters-abcdefghijklmnopqrstuvwxyz";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }

        @Test
        @DisplayName("InvalidateUserPermissionCommand가 올바르게 생성됨")
        void shouldCreateCorrectInvalidateUserPermissionCommand() {
            // given
            String tenantId = "tenant999";
            String userId = "user888";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            // then
            ArgumentCaptor<InvalidateUserPermissionCommand> commandCaptor =
                    ArgumentCaptor.forClass(InvalidateUserPermissionCommand.class);
            verify(invalidateUserPermissionUseCase).execute(commandCaptor.capture());

            InvalidateUserPermissionCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.tenantId()).isEqualTo(tenantId);
            assertThat(capturedCommand.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("UseCase 실행 중 에러 발생 시 500 Internal Server Error 응답")
        void shouldReturnInternalServerErrorWhenUseCaseFails() {
            // given
            String tenantId = "tenant111";
            String userId = "user222";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            RuntimeException exception = new RuntimeException("Invalidation failed");
            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(
                            response ->
                                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                    .verifyComplete();

            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }

        @Test
        @DisplayName("숫자로만 구성된 ID로 무효화")
        void shouldInvalidateWithNumericIds() {
            // given
            String tenantId = "123456789";
            String userId = "987654321";
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(controller.invalidateUserPermission(request))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }
    }

    @Nested
    @DisplayName("DTO 테스트")
    class DtoTest {

        @Test
        @DisplayName("SpecSyncRequest 생성 및 접근")
        void shouldCreateAndAccessSpecSyncRequest() {
            // given
            Long version = 777L;
            List<String> changedServices = List.of("test-service");

            // when
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(version, changedServices);

            // then
            assertThat(request.version()).isEqualTo(version);
            assertThat(request.changedServices()).isEqualTo(changedServices);
        }

        @Test
        @DisplayName("UserInvalidateRequest 생성 및 접근")
        void shouldCreateAndAccessUserInvalidateRequest() {
            // given
            String tenantId = "tenant333";
            String userId = "user444";

            // when
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(tenantId, userId);

            // then
            assertThat(request.tenantId()).isEqualTo(tenantId);
            assertThat(request.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("SpecSyncRequest null 값 처리")
        void shouldHandleNullValuesInSpecSyncRequest() {
            // when
            PermissionWebhookController.SpecSyncRequest request =
                    new PermissionWebhookController.SpecSyncRequest(null, null);

            // then
            assertThat(request.version()).isNull();
            assertThat(request.changedServices()).isNull();
        }

        @Test
        @DisplayName("UserInvalidateRequest null 값 처리")
        void shouldHandleNullValuesInUserInvalidateRequest() {
            // when
            PermissionWebhookController.UserInvalidateRequest request =
                    new PermissionWebhookController.UserInvalidateRequest(null, null);

            // then
            assertThat(request.tenantId()).isNull();
            assertThat(request.userId()).isNull();
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("Spec 동기화 후 사용자 무효화 시나리오")
        void shouldHandleSpecSyncFollowedByUserInvalidation() {
            // given
            PermissionWebhookController.SpecSyncRequest specRequest =
                    new PermissionWebhookController.SpecSyncRequest(100L, List.of("user-service"));
            PermissionWebhookController.UserInvalidateRequest userRequest =
                    new PermissionWebhookController.UserInvalidateRequest("tenant100", "user100");

            when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                    .thenReturn(Mono.empty());
            when(invalidateUserPermissionUseCase.execute(
                            any(InvalidateUserPermissionCommand.class)))
                    .thenReturn(Mono.empty());

            // when & then - Spec 동기화
            StepVerifier.create(controller.syncPermissionSpec(specRequest))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            // when & then - 사용자 무효화
            StepVerifier.create(controller.invalidateUserPermission(userRequest))
                    .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                    .verifyComplete();

            verify(syncPermissionSpecUseCase).execute(any(SyncPermissionSpecCommand.class));
            verify(invalidateUserPermissionUseCase)
                    .execute(any(InvalidateUserPermissionCommand.class));
        }
    }
}
