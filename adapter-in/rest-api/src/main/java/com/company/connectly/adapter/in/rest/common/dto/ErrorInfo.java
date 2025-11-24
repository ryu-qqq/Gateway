package com.company.connectly.adapter.in.rest.common.dto;

/**
 * ErrorInfo - API 에러 정보
 *
 * <p>API 응답에서 발생한 에러의 상세 정보를 담습니다.</p>
 *
 * <p><strong>사용 예시:</strong></p>
 * <pre>{@code
 * ErrorInfo error = new ErrorInfo(
 *     "TENANT_NOT_FOUND",
 *     "존재하지 않는 테넌트입니다"
 * );
 * }</pre>
 *
 * <p><strong>에러 코드 규칙:</strong></p>
 * <ul>
 *   <li>대문자 스네이크 케이스 사용 (UPPER_SNAKE_CASE)</li>
 *   <li>도메인_상황_상태 형식 권장 (예: USER_NOT_FOUND, ORDER_INVALID_STATUS)</li>
 * </ul>
 *
 * @param errorCode 에러 코드 (예: "TENANT_NOT_FOUND")
 * @param message 에러 메시지 (사용자에게 표시할 메시지)
 * @author ryu-qqq
 * @since 2025-10-23
 */
public record ErrorInfo(
    String errorCode,
    String message
) {
    /**
     * ErrorInfo 생성자
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws IllegalArgumentException errorCode 또는 message가 null이거나 빈 문자열인 경우
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public ErrorInfo {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode는 필수입니다");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message는 필수입니다");
        }
    }
}
