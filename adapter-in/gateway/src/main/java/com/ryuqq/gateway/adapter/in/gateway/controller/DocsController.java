package com.ryuqq.gateway.adapter.in.gateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Gateway Documentation Controller
 *
 * <p>Gateway 내부 API 문서를 서빙하는 Controller
 *
 * <p><strong>엔드포인트</strong>:
 *
 * <ul>
 *   <li>GET /docs - API 문서 (HTML)
 *   <li>GET /docs/index.html - API 문서 (HTML)
 * </ul>
 *
 * <p><strong>빌드 방법</strong>:
 *
 * <pre>{@code
 * ./gradlew :adapter-in:gateway:asciidoctor
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/docs")
public class DocsController {

    private static final String DOCS_PATH = "static/docs/index.html";

    /**
     * API 문서 조회 (루트)
     *
     * @return Mono&lt;ResponseEntity&lt;Resource&gt;&gt; HTML 문서
     */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<Resource>> getDocs() {
        return serveDocsHtml();
    }

    /**
     * API 문서 조회 (index.html)
     *
     * @return Mono&lt;ResponseEntity&lt;Resource&gt;&gt; HTML 문서
     */
    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<Resource>> getDocsIndex() {
        return serveDocsHtml();
    }

    private Mono<ResponseEntity<Resource>> serveDocsHtml() {
        Resource resource = new ClassPathResource(DOCS_PATH);

        return Mono.just(resource)
                .filter(Resource::exists)
                .map(r -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(r))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().<Resource>build()));
    }
}
