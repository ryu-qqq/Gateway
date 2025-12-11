package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * RefreshTokenReusedException - Refresh Token 재사용이 감지된 경우 발생하는 예외
 *
 * <p>이미 사용되어 Blacklist에 등록된 Refresh Token이 다시 사용된 경우 발생합니다. 이는 Token 탈취 가능성을 나타내므로 보안 경고입니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>Blacklist에 등록된 Refresh Token 재사용 시
 *   <li>Refresh Token Rotation 후 기존 토큰 사용 시
 * </ul>
 *
 * <p><strong>보안 조치:</strong>
 *
 * <ul>
 *   <li>해당 사용자의 모든 세션 강제 종료 권고
 *   <li>탈취 의심으로 보안 알림 발송
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class RefreshTokenReusedException extends DomainException {

    /**
     * Constructor - 상세 정보로 예외 생성
     *
     * @param detail 상세 정보
     */
    public RefreshTokenReusedException(String detail) {
        super(AuthenticationErrorCode.REFRESH_TOKEN_REUSED, detail);
    }

    /** Constructor - 기본 예외 생성 */
    public RefreshTokenReusedException() {
        super(AuthenticationErrorCode.REFRESH_TOKEN_REUSED);
    }
}
