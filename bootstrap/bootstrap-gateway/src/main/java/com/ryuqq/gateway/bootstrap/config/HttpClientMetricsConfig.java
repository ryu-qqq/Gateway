package com.ryuqq.gateway.bootstrap.config;

import java.net.URI;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP Client 메트릭 활성화 설정
 *
 * <p>Spring Cloud Gateway의 Reactor Netty HTTP Client에 Micrometer 메트릭을 활성화합니다.
 *
 * <p><strong>활성화되는 메트릭:</strong>
 *
 * <ul>
 *   <li>reactor.netty.http.client.* - HTTP Client 요청/응답 메트릭
 *   <li>reactor.netty.connection.provider.* - Connection Pool 메트릭
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
public class HttpClientMetricsConfig {

    /**
     * HTTP Client 메트릭 활성화
     *
     * <p>Gateway → Backend 통신에 사용되는 HTTP Client에 메트릭을 추가합니다.
     *
     * @return HttpClientCustomizer
     */
    @Bean
    public HttpClientCustomizer httpClientMetricsCustomizer() {
        return httpClient ->
                httpClient.metrics(
                        true,
                        uriStr -> {
                            // URI에서 path만 추출하여 태그로 사용 (쿼리스트링 제거, 카디널리티 제어)
                            try {
                                String path = URI.create(uriStr).getPath();
                                if (path == null || path.isEmpty()) {
                                    return "/";
                                }
                                // path parameter를 일반화 (예: /api/v1/orders/123 ->
                                // /api/v1/orders/{id})
                                return normalizeUriPath(path);
                            } catch (Exception e) {
                                return "/unknown";
                            }
                        });
    }

    /**
     * URI Path 정규화
     *
     * <p>숫자로 된 path segment를 {id}로 치환하여 메트릭 카디널리티를 제어합니다.
     *
     * @param path 원본 URI path
     * @return 정규화된 path
     */
    private String normalizeUriPath(String path) {
        // 숫자로만 이루어진 path segment를 {id}로 치환
        // 예: /api/v1/orders/12345 -> /api/v1/orders/{id}
        return path.replaceAll("/\\d+", "/{id}");
    }
}
