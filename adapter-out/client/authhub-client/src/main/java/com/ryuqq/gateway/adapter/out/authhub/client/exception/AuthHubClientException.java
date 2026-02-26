package com.ryuqq.gateway.adapter.out.authhub.client.exception;

/**
 * AuthHub Client 공통 예외
 *
 * <p>AuthHub 외부 시스템 호출 시 발생하는 모든 예외의 기본 클래스
 *
 * @author development-team
 * @since 1.0.0
 */
public class AuthHubClientException extends RuntimeException {

    public AuthHubClientException(String message) {
        super(message);
    }

    public AuthHubClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 인증 관련 예외 (JWKS, Token Refresh) */
    public static class AuthException extends AuthHubClientException {
        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Tenant 관련 예외 */
    public static class TenantException extends AuthHubClientException {
        public TenantException(String message) {
            super(message);
        }

        public TenantException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Permission 관련 예외 */
    public static class PermissionException extends AuthHubClientException {
        public PermissionException(String message) {
            super(message);
        }

        public PermissionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
