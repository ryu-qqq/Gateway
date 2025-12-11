package com.ryuqq.gateway.domain.authorization.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import java.util.Set;

/**
 * PermissionDeniedException - 권한 부족 예외
 *
 * <p>사용자가 요청한 리소스에 대한 권한이 없을 때 발생합니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PermissionDeniedException extends DomainException {

    private final Set<String> requiredPermissions;
    private final Set<String> userPermissions;

    /**
     * Constructor - 필요 권한과 사용자 권한으로 예외 생성
     *
     * @param requiredPermissions 필요한 권한 목록
     * @param userPermissions 사용자가 보유한 권한 목록
     */
    public PermissionDeniedException(Set<String> requiredPermissions, Set<String> userPermissions) {
        super(
                AuthorizationErrorCode.PERMISSION_DENIED,
                buildDetail(requiredPermissions, userPermissions));
        this.requiredPermissions = Set.copyOf(requiredPermissions);
        this.userPermissions = Set.copyOf(userPermissions);
    }

    /**
     * Constructor - 상세 정보로 예외 생성
     *
     * @param detail 상세 정보
     */
    public PermissionDeniedException(String detail) {
        super(AuthorizationErrorCode.PERMISSION_DENIED, detail);
        this.requiredPermissions = Set.of();
        this.userPermissions = Set.of();
    }

    /** Constructor - 기본 예외 생성 */
    public PermissionDeniedException() {
        super(AuthorizationErrorCode.PERMISSION_DENIED);
        this.requiredPermissions = Set.of();
        this.userPermissions = Set.of();
    }

    /**
     * 필요한 권한 목록 반환
     *
     * @return 필요한 권한 Set
     */
    public Set<String> requiredPermissions() {
        return requiredPermissions;
    }

    /**
     * 사용자 권한 목록 반환
     *
     * @return 사용자 권한 Set
     */
    public Set<String> userPermissions() {
        return userPermissions;
    }

    private static String buildDetail(Set<String> required, Set<String> user) {
        return String.format("Required: %s, User has: %s", required, user);
    }
}
