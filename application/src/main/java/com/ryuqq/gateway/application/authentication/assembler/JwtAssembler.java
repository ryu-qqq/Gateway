package com.ryuqq.gateway.application.authentication.assembler;

import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.GetPublicKeyResponse;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.domain.authentication.vo.AccessToken;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Component;

/**
 * JWT Assembler
 *
 * <p>Application Layer의 DTO와 Domain Layer의 VO 간 변환을 담당하는 Assembler
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>ValidateJwtCommand → AccessToken 변환
 *   <li>JwtClaims → ValidateJwtResponse 변환
 *   <li>PublicKey → GetPublicKeyResponse 변환
 * </ul>
 *
 * <p><strong>Zero-Tolerance 준수</strong>:
 *
 * <ul>
 *   <li>Component로 등록
 *   <li>Lombok 금지
 * </ul>
 */
@Component
public class JwtAssembler {

    /**
     * ValidateJwtCommand를 AccessToken으로 변환
     *
     * @param command ValidateJwtCommand
     * @return AccessToken (Domain VO)
     * @throws IllegalArgumentException command가 null이거나 accessToken이 유효하지 않은 경우
     */
    public AccessToken toAccessToken(ValidateJwtCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("ValidateJwtCommand cannot be null");
        }
        return AccessToken.of(command.accessToken());
    }

    /**
     * JwtClaims를 ValidateJwtResponse로 변환
     *
     * @param claims JwtClaims (Domain VO)
     * @return ValidateJwtResponse
     */
    public ValidateJwtResponse toValidateJwtResponse(JwtClaims claims) {
        return new ValidateJwtResponse(claims, true);
    }

    /**
     * 검증 실패 시 ValidateJwtResponse 생성
     *
     * @return ValidateJwtResponse (isValid = false)
     */
    public ValidateJwtResponse toFailedValidateJwtResponse() {
        return new ValidateJwtResponse(null, false);
    }

    /**
     * PublicKey를 GetPublicKeyResponse로 변환
     *
     * @param publicKey PublicKey (Domain VO)
     * @return GetPublicKeyResponse
     * @throws IllegalArgumentException publicKey가 null인 경우
     */
    public GetPublicKeyResponse toGetPublicKeyResponse(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey cannot be null");
        }
        return new GetPublicKeyResponse(publicKey);
    }
}
