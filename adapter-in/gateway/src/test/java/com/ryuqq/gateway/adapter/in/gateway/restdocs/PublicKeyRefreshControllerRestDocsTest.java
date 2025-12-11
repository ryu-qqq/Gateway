package com.ryuqq.gateway.adapter.in.gateway.restdocs;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

import com.ryuqq.gateway.adapter.in.gateway.controller.PublicKeyRefreshController;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshPublicKeysUseCase;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * PublicKeyRefreshController RestDocs 테스트
 *
 * <p>Public Key Cache 갱신 API 문서화
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
@Tag("restdocs")
@DisplayName("PublicKeyRefreshController RestDocs")
class PublicKeyRefreshControllerRestDocsTest {

    private WebTestClient webTestClient;

    @Mock
    private RefreshPublicKeysUseCase refreshPublicKeysUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        PublicKeyRefreshController controller =
                new PublicKeyRefreshController(refreshPublicKeysUseCase);

        this.webTestClient =
                WebTestClient.bindToController(controller)
                        .configureClient()
                        .filter(documentationConfiguration(restDocumentation))
                        .build();
    }

    @Test
    @DisplayName("POST /actuator/refresh-public-keys - Public Key Cache 갱신")
    void refreshPublicKeys() {
        // given
        when(refreshPublicKeysUseCase.execute()).thenReturn(Mono.empty());

        // when & then
        webTestClient
                .post()
                .uri("/actuator/refresh-public-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(
                        document(
                                "actuator/refresh-public-keys",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));
    }
}
