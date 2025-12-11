package com.ryuqq.gateway.adapter.in.gateway.restdocs;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

import com.ryuqq.gateway.adapter.in.gateway.controller.DocsController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * DocsController RestDocs 테스트
 *
 * <p>API 문서 서빙 엔드포인트 문서화
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(RestDocumentationExtension.class)
@Tag("restdocs")
@DisplayName("DocsController RestDocs")
class DocsControllerRestDocsTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        DocsController controller = new DocsController();

        this.webTestClient =
                WebTestClient.bindToController(controller)
                        .configureClient()
                        .filter(documentationConfiguration(restDocumentation))
                        .build();
    }

    @Test
    @DisplayName("GET /docs - Gateway API 문서 조회")
    void getDocs() {
        webTestClient
                .get()
                .uri("/docs")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(
                        document(
                                "docs/index",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));
    }
}
