package com.ryuqq.gateway.domain.authorization.exception;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;

/**
 * AuthorizationErrorCode - Authorization Bounded Context 에러 코드
 *
 * <p>Permission 인가 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 *
 * <p><strong>에러 코드 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 형식: AUTHZ-{3자리 숫자}
 *   <li>✅ HTTP 상태 코드 매핑
 *   <li>✅ 명확한 에러 메시지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum AuthorizationErrorCode implements ErrorCode {

    /**
     * 권한 부족
     *
     * <p>요청한 리소스에 대한 권한이 없는 경우 발생
     */
    PERMISSION_DENIED("AUTHZ-001", 403, "Permission denied"),

    /**
     * Permission Spec 찾을 수 없음
     *
     * <p>엔드포인트에 대한 Permission Spec이 정의되지 않은 경우 발생 (Default Deny)
     */
    PERMISSION_SPEC_NOT_FOUND("AUTHZ-002", 403, "Permission spec not found for endpoint");

    private final String code;
    private final int httpStatus;
    private final String message;

    AuthorizationErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
