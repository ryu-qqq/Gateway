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

import com.ryuqq.gateway.adapter.in.gateway.controller.TenantConfigWebhookController;
import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.dto.response.SyncTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.in.command.SyncTenantConfigUseCase;
import java.time.Instant;
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
 * TenantConfigWebhookController RestDocs 테스트
 *
 * <p>Tenant Config Webhook API 문서화
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
@Tag("restdocs")
@DisplayName("TenantConfigWebhookController RestDocs")
class TenantConfigWebhookControllerRestDocsTest {

    private WebTestClient webTestClient;

    @Mock private SyncTenantConfigUseCase syncTenantConfigUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        TenantConfigWebhookController controller =
                new TenantConfigWebhookController(syncTenantConfigUseCase);

        this.webTestClient =
                WebTestClient.bindToController(controller)
                        .configureClient()
                        .filter(documentationConfiguration(restDocumentation))
                        .build();
    }

    @Test
    @DisplayName("POST /internal/gateway/tenants/config-changed - Tenant Config 캐시 무효화")
    void handleTenantConfigChanged() {
        // given
        String tenantId = "tenant-abc-123";
        SyncTenantConfigResponse response = SyncTenantConfigResponse.success(tenantId);
        when(syncTenantConfigUseCase.execute(any(SyncTenantConfigCommand.class)))
                .thenReturn(Mono.just(response));

        TenantConfigWebhookController.TenantConfigChangedEvent event =
                new TenantConfigWebhookController.TenantConfigChangedEvent(
                        tenantId, Instant.parse("2025-01-15T10:30:00Z"));

        // when & then
        webTestClient
                .post()
                .uri("/internal/gateway/tenants/config-changed")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(
                        document(
                                "internal/tenants/config-changed",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("tenantId")
                                                .type(JsonFieldType.STRING)
                                                .description("변경된 테넌트 ID"),
                                        fieldWithPath("timestamp")
                                                .type(JsonFieldType.NUMBER)
                                                .description("변경 시각 (Unix timestamp)"))));
    }
}
