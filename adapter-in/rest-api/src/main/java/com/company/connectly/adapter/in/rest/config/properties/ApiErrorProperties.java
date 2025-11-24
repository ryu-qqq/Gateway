package com.company.connectly.adapter.in.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API 에러 응답 설정 Properties
 *
 * <p>RFC 7807 Problem Details의 type URI를 관리합니다.</p>
 *
 * <p><strong>설정 예시 (application.yml):</strong></p>
 * <pre>{@code
 * api:
 *   error:
 *     base-url: https://api.example.com/problems
 *     use-about-blank: false
 * }</pre>
 *
 * <p><strong>사용 방법:</strong></p>
 * <pre>{@code
 * @Component
 * public class ExampleErrorMapper implements ErrorMapper {
 *     private final ApiErrorProperties errorProperties;
 *
 *     public MappedError map(DomainException ex, Locale locale) {
 *         URI typeUri = URI.create(
 *             errorProperties.getBaseUrl() + "/example-not-found"
 *         );
 *         return new MappedError(HttpStatus.NOT_FOUND, "Not Found", "...", typeUri);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>환경별 설정:</strong></p>
 * <ul>
 *   <li>로컬/개발: about:blank (문서 URL 불필요)</li>
 *   <li>스테이징: https://staging-api.example.com/problems</li>
 *   <li>프로덕션: https://api.example.com/problems</li>
 * </ul>
 *
 * @author windsurf
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "api.error")
public class ApiErrorProperties {

    /**
     * 에러 타입 문서의 베이스 URL
     *
     * <p>RFC 7807 표준에 따라 각 에러 타입은 문서 URL을 가져야 합니다.</p>
     *
     * <p><strong>예시:</strong></p>
     * <ul>
     *   <li>baseUrl = "https://api.example.com/problems"</li>
     *   <li>최종 URL = "https://api.example.com/problems/example-not-found"</li>
     * </ul>
     *
     * <p>기본값: about:blank (문서 URL이 없을 때 사용)</p>
     */
    private String baseUrl = "about:blank";

    /**
     * about:blank 사용 여부
     *
     * <p>true면 모든 에러에 대해 about:blank를 사용 (문서 URL 무시)</p>
     * <p>false면 baseUrl 기반으로 실제 문서 URL 생성</p>
     *
     * <p>기본값: true (로컬 개발 환경에서 편의를 위해)</p>
     */
    private boolean useAboutBlank = true;

    /**
     * 에러 타입 URI를 생성합니다.
     *
     * @param path 에러 타입별 경로 (예: "example-not-found")
     * @return 완전한 에러 타입 URI 문자열
     */
    public String buildTypeUri(String path) {
        if (useAboutBlank || "about:blank".equals(baseUrl)) {
            return "about:blank";
        }
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + path;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isUseAboutBlank() {
        return useAboutBlank;
    }

    public void setUseAboutBlank(boolean useAboutBlank) {
        this.useAboutBlank = useAboutBlank;
    }
}
