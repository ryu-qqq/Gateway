package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * Public Key 찾을 수 없음 예외
 *
 * <p>JWT 서명 검증에 필요한 Public Key를 찾을 수 없을 때 발생하는 예외
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>JWT Header의 kid (Key ID)에 해당하는 Public Key가 JWKS에 없는 경우
 *   <li>Public Key 저장소(Redis, DB)에서 kid로 조회 실패한 경우
 *   <li>외부 JWKS Endpoint에서 Public Key 조회 실패한 경우
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 404 NOT FOUND
 *   <li>Error Code: AUTH-003
 *   <li>Message: "Public key not found: kid={kid}"
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PublicKeyNotFoundException extends DomainException {

    /**
     * Key ID를 포함한 예외 생성
     *
     * @param kid 찾을 수 없는 Public Key의 Key ID
     * @author development-team
     * @since 1.0.0
     */
    public PublicKeyNotFoundException(String kid) {
        super(
                AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode(),
                "Public key not found: kid=" + kid);
    }

    /**
     * 기본 에러 메시지 사용
     *
     * <p>AuthenticationErrorCode의 기본 메시지를 사용합니다.
     *
     * @author development-team
     * @since 1.0.0
     */
    public PublicKeyNotFoundException() {
        super(
                AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode(),
                AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getMessage());
    }
}
