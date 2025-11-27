package com.ryuqq.gateway.domain.tenant.exception;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;

/**
 * TenantErrorCode - Tenant Bounded Context 에러 코드
 *
 * <p>멀티테넌트 격리 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 *
 * <p><strong>에러 코드 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 형식: TENANT-{3자리 숫자}
 *   <li>✅ HTTP 상태 코드 매핑
 *   <li>✅ 명확한 에러 메시지
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * throw new TenantMismatchException("tenant-1", "tenant-2");
 * // → ErrorCode: TENANT-001, HTTP Status: 403
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum TenantErrorCode implements ErrorCode {

    /**
     * Tenant ID 불일치
     *
     * <p>JWT의 tenantId와 요청의 tenantId가 일치하지 않는 경우 발생
     */
    TENANT_MISMATCH("TENANT-001", 403, "Tenant ID mismatch. Access denied."),

    /**
     * MFA 인증 필수
     *
     * <p>Tenant Config에서 MFA 필수로 설정되었으나, 사용자가 MFA 인증을 완료하지 않은 경우 발생
     */
    MFA_REQUIRED("TENANT-002", 403, "Multi-Factor Authentication is required for this tenant."),

    /**
     * 소셜 로그인 제공자 불허용
     *
     * <p>Tenant Config에서 허용되지 않은 소셜 로그인 제공자로 로그인 시도한 경우 발생
     */
    SOCIAL_LOGIN_NOT_ALLOWED("TENANT-003", 403, "Social login provider is not allowed for this tenant.");

    private final String code;
    private final int httpStatus;
    private final String message;

    /**
     * Constructor - ErrorCode 생성
     *
     * @param code 에러 코드 (TENANT-XXX)
     * @param httpStatus HTTP 상태 코드
     * @param message 에러 메시지
     * @author development-team
     * @since 1.0.0
     */
    TenantErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 문자열 (예: TENANT-001)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드 (예: 403)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 에러 메시지 반환
     *
     * @return 에러 메시지 문자열
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getMessage() {
        return message;
    }
}
