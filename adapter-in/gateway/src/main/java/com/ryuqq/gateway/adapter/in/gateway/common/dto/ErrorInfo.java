package com.ryuqq.gateway.adapter.in.gateway.common.dto;

/**
 * ErrorInfo - 표준 에러 정보 (Gateway 전용)
 *
 * <p>모든 Gateway 에러 응답에 포함되는 에러 정보입니다.
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 에러 정보 생성
 * ErrorInfo error = new ErrorInfo("JWT_EXPIRED", "토큰이 만료되었습니다");
 *
 * // ApiResponse와 함께 사용
 * ApiResponse<Void> response = ApiResponse.ofFailure(error);
 * }</pre>
 *
 * <p><strong>에러 코드 예시:</strong>
 *
 * <ul>
 *   <li>JWT_EXPIRED - JWT 토큰 만료
 *   <li>JWT_INVALID - JWT 토큰 검증 실패
 *   <li>UNAUTHORIZED - 인증 실패
 *   <li>FORBIDDEN - 권한 부족
 *   <li>INTERNAL_ERROR - 내부 서버 오류
 * </ul>
 *
 * @param errorCode 에러 코드 (필수, 대문자 스네이크 케이스)
 * @param message 에러 메시지 (필수, 사용자에게 표시될 메시지)
 * @author development-team
 * @since 1.0.0
 */
public record ErrorInfo(String errorCode, String message) {

    /**
     * Compact Constructor - Validation
     *
     * @throws IllegalArgumentException errorCode 또는 message가 null이거나 빈 문자열인 경우
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
