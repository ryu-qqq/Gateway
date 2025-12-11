package com.ryuqq.gateway.adapter.in.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * DocsController 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("DocsController 테스트")
class DocsControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        DocsController controller = new DocsController();
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("GET /docs - HTML 문서 조회")
    void shouldReturnDocsHtml() {
        webTestClient
                .get()
                .uri("/docs")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.TEXT_HTML);
    }

    @Test
    @DisplayName("GET /docs/index.html - HTML 문서 조회")
    void shouldReturnDocsIndexHtml() {
        webTestClient
                .get()
                .uri("/docs/index.html")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.TEXT_HTML);
    }
}
