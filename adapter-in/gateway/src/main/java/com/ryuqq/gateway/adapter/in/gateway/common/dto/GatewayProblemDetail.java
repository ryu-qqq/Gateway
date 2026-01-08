package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.time.Instant;

/**
 * RFC 7807 Problem Detail 구현
 *
 * <p>Gateway 에러 응답을 위한 표준화된 에러 형식입니다.
 *
 * <p><strong>RFC 7807 표준 필드</strong>:
 *
 * <ul>
 *   <li>type - 문제 유형을 식별하는 URI (기본: "about:blank")
 *   <li>title - HTTP 상태에 해당하는 짧은 요약
 *   <li>status - HTTP 상태 코드
 *   <li>detail - 문제에 대한 상세 설명
 *   <li>instance - 문제가 발생한 리소스 URI
 * </ul>
 *
 * <p><strong>확장 필드</strong>:
 *
 * <ul>
 *   <li>code - 애플리케이션별 에러 코드 (예: JWT_EXPIRED)
 *   <li>timestamp - 에러 발생 시각 (ISO 8601)
 *   <li>requestId - 요청 추적 ID
 * </ul>
 *
 * <p><strong>응답 예시</strong>:
 *
 * <pre>{@code
 * {
 *   "type": "about:blank",
 *   "title": "Forbidden",
 *   "status": 403,
 *   "detail": "IP가 차단되었습니다",
 *   "instance": "/api/v1/users",
 *   "code": "IP_BLOCKED",
 *   "timestamp": "2025-01-08T10:30:00Z",
 *   "requestId": "abc123"
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayProblemDetail(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        String code,
        Instant timestamp,
        String requestId) {

    private static final URI DEFAULT_TYPE = URI.create("about:blank");

    /**
     * 기본 ProblemDetail 생성
     *
     * @param status HTTP 상태 코드
     * @param title HTTP 상태 제목
     * @param detail 상세 메시지
     * @param code 에러 코드
     * @return GatewayProblemDetail
     */
    public static GatewayProblemDetail of(int status, String title, String detail, String code) {
        return new GatewayProblemDetail(
                DEFAULT_TYPE, title, status, detail, null, code, Instant.now(), null);
    }

    /**
     * 요청 정보를 포함한 ProblemDetail 생성
     *
     * @param status HTTP 상태 코드
     * @param title HTTP 상태 제목
     * @param detail 상세 메시지
     * @param code 에러 코드
     * @param instance 요청 경로
     * @param requestId 요청 추적 ID
     * @return GatewayProblemDetail
     */
    public static GatewayProblemDetail of(
            int status,
            String title,
            String detail,
            String code,
            String instance,
            String requestId) {
        return new GatewayProblemDetail(
                DEFAULT_TYPE,
                title,
                status,
                detail,
                parseInstanceUri(instance),
                code,
                Instant.now(),
                requestId);
    }

    /**
     * instance 문자열을 안전하게 URI로 파싱
     *
     * <p>잘못된 URI 문자열이 입력되면 null을 반환합니다.
     *
     * @param instance URI 문자열
     * @return 파싱된 URI 또는 null
     */
    private static URI parseInstanceUri(String instance) {
        if (instance == null || instance.isBlank()) {
            return null;
        }
        try {
            return URI.create(instance);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Builder 패턴 지원 */
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("PMD.TooManyFields")
    public static class Builder {

        private URI type = DEFAULT_TYPE;
        private String title;
        private int status;
        private String detail;
        private URI instance;
        private String code;
        private Instant timestamp = Instant.now();
        private String requestId;

        public Builder type(URI type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = parseInstanceUri(instance);
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public GatewayProblemDetail build() {
            return new GatewayProblemDetail(
                    type, title, status, detail, instance, code, timestamp, requestId);
        }
    }
}
