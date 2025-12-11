package com.ryuqq.gateway.adapter.in.gateway.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

import com.ryuqq.gateway.adapter.in.gateway.controller.PermissionWebhookController;
import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import com.ryuqq.gateway.application.authorization.port.in.command.InvalidateUserPermissionUseCase;
import com.ryuqq.gateway.application.authorization.port.in.command.SyncPermissionSpecUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * PermissionWebhookController RestDocs 테스트
 *
 * <p>Permission Webhook API 문서화
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
@Tag("restdocs")
@DisplayName("PermissionWebhookController RestDocs")
class PermissionWebhookControllerRestDocsTest {

    private WebTestClient webTestClient;

    @Mock private SyncPermissionSpecUseCase syncPermissionSpecUseCase;

    @Mock private InvalidateUserPermissionUseCase invalidateUserPermissionUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        PermissionWebhookController controller =
                new PermissionWebhookController(
                        syncPermissionSpecUseCase, invalidateUserPermissionUseCase);

        this.webTestClient =
                WebTestClient.bindToController(controller)
                        .configureClient()
                        .filter(documentationConfiguration(restDocumentation))
                        .build();
    }

    @Test
    @DisplayName("POST /webhooks/permission/spec-sync - Permission Spec 캐시 동기화")
    void syncPermissionSpec() {
        // given
        when(syncPermissionSpecUseCase.execute(any(SyncPermissionSpecCommand.class)))
                .thenReturn(Mono.empty());

        PermissionWebhookController.SpecSyncRequest request =
                new PermissionWebhookController.SpecSyncRequest(
                        123L, List.of("user-service", "order-service"));

        // when & then
        webTestClient
                .post()
                .uri("/webhooks/permission/spec-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(
                        document(
                                "webhooks/permission/spec-sync",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("version")
                                                .type(JsonFieldType.NUMBER)
                                                .description("Permission Spec 버전"),
                                        fieldWithPath("changedServices")
                                                .type(JsonFieldType.ARRAY)
                                                .description("변경된 서비스 목록"))));
    }

    @Test
    @DisplayName("POST /webhooks/permission/user-invalidate - 사용자 권한 캐시 무효화")
    void invalidateUserPermission() {
        // given
        when(invalidateUserPermissionUseCase.execute(any(InvalidateUserPermissionCommand.class)))
                .thenReturn(Mono.empty());

        PermissionWebhookController.UserInvalidateRequest request =
                new PermissionWebhookController.UserInvalidateRequest("tenant-123", "user-456");

        // when & then
        webTestClient
                .post()
                .uri("/webhooks/permission/user-invalidate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(
                        document(
                                "webhooks/permission/user-invalidate",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("tenantId")
                                                .type(JsonFieldType.STRING)
                                                .description("테넌트 ID"),
                                        fieldWithPath("userId")
                                                .type(JsonFieldType.STRING)
                                                .description("사용자 ID"))));
    }
}
