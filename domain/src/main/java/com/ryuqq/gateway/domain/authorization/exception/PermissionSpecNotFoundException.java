package com.ryuqq.gateway.domain.authorization.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * PermissionSpecNotFoundException - Permission Spec 미정의 예외
 *
 * <p>요청한 엔드포인트에 대한 Permission Spec이 정의되지 않았을 때 발생합니다. Default Deny 정책에 따라 403 Forbidden을 반환합니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionSpecNotFoundException extends DomainException {

    private final String path;
    private final String method;

    /**
     * Constructor - Path와 Method로 예외 생성
     *
     * @param path 요청 경로
     * @param method HTTP 메서드
     */
    public PermissionSpecNotFoundException(String path, String method) {
        super(
                AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND,
                buildDetail(path, method));
        this.path = path;
        this.method = method;
    }

    /**
     * Constructor - 기본 예외 생성
     */
    public PermissionSpecNotFoundException() {
        super(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND);
        this.path = null;
        this.method = null;
    }

    /**
     * 요청 경로 반환
     *
     * @return 요청 경로
     */
    public String path() {
        return path;
    }

    /**
     * HTTP 메서드 반환
     *
     * @return HTTP 메서드
     */
    public String method() {
        return method;
    }

    private static String buildDetail(String path, String method) {
        return String.format("%s %s (Default Deny)", method, path);
    }
}
