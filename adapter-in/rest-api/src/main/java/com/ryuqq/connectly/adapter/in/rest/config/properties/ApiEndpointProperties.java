package com.ryuqq.connectly.adapter.in.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API 엔드포인트 경로 설정 Properties
 *
 * <p>REST API 엔드포인트 경로를 application.yml에서 중앙 관리합니다.
 *
 * <p><strong>설정 예시 (application.yml):</strong>
 *
 * <pre>{@code
 * api:
 *   endpoints:
 *     base-v1: /api/v1
 *     example:
 *       base: /examples
 *       by-id: /{id}
 *       admin-search: /admin/examples/search
 * }</pre>
 *
 * <p><strong>사용 방법:</strong>
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("${api.endpoints.base-v1}")
 * public class ExampleController {
 *     private final ApiEndpointProperties endpoints;
 *
 *     @GetMapping("${api.endpoints.example.base}")
 *     public ResponseEntity<?> searchExamples() { ... }
 * }
 * }</pre>
 *
 * @author windsurf
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "api.endpoints")
public class ApiEndpointProperties {

    /** API v1 베이스 경로 (기본값: /api/v1) */
    private String baseV1 = "/api/v1";

    /** Example 도메인 엔드포인트 설정 */
    private ExampleEndpoints example = new ExampleEndpoints();

    /** Example 도메인 엔드포인트 경로 */
    public static class ExampleEndpoints {
        /** Example 기본 경로 (기본값: /examples) */
        private String base = "/examples";

        /** Example ID 조회 경로 (기본값: /{id}) */
        private String byId = "/{id}";

        /** Example 관리자 검색 경로 (기본값: /admin/examples/search) */
        private String adminSearch = "/admin/examples/search";

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public String getById() {
            return byId;
        }

        public void setById(String byId) {
            this.byId = byId;
        }

        public String getAdminSearch() {
            return adminSearch;
        }

        public void setAdminSearch(String adminSearch) {
            this.adminSearch = adminSearch;
        }
    }

    public String getBaseV1() {
        return baseV1;
    }

    public void setBaseV1(String baseV1) {
        this.baseV1 = baseV1;
    }

    public ExampleEndpoints getExample() {
        return example;
    }

    public void setExample(ExampleEndpoints example) {
        this.example = example;
    }
}
