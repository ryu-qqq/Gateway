package com.ryuqq.gateway.domain.authentication.vo;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * JWT Public Key Value Object
 *
 * <p>RS256 서명 검증에 사용되는 RSA Public Key를 표현하는 불변 객체
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>kid (Key ID)는 null이거나 빈 문자열일 수 없다
 *   <li>modulus와 exponent는 Base64 URL 인코딩된 문자열이다
 *   <li>kty는 "RSA"만 허용된다
 *   <li>alg는 "RS256"만 허용된다
 * </ul>
 *
 * @param kid Key ID (JWKS에서 Public Key를 식별하는 고유 ID)
 * @param modulus RSA Public Key Modulus (Base64 URL 인코딩)
 * @param exponent RSA Public Key Exponent (Base64 URL 인코딩)
 * @param kty Key Type (RSA 고정)
 * @param use Public Key Use (sig: 서명 검증용)
 * @param alg Algorithm (RS256 고정)
 * @author development-team
 * @since 1.0.0
 */
public record PublicKey(
        String kid, String modulus, String exponent, String kty, String use, String alg) {

    /** Compact Constructor (검증 로직) */
    public PublicKey {
        if (kid == null || kid.isBlank()) {
            throw new IllegalArgumentException("Key ID (kid) cannot be null or blank");
        }
        if (modulus == null || modulus.isBlank()) {
            throw new IllegalArgumentException("Modulus cannot be null or blank");
        }
        if (exponent == null || exponent.isBlank()) {
            throw new IllegalArgumentException("Exponent cannot be null or blank");
        }
        if (!"RSA".equalsIgnoreCase(kty)) {
            throw new IllegalArgumentException("Key Type (kty) must be 'RSA'");
        }
        if (!"RS256".equalsIgnoreCase(alg)) {
            throw new IllegalArgumentException("Algorithm (alg) must be 'RS256'");
        }
    }

    /**
     * RSAPublicKey로 변환
     *
     * <p>Base64 URL 인코딩된 modulus와 exponent를 Java RSAPublicKey로 변환합니다.
     *
     * @return Java RSAPublicKey
     * @throws IllegalStateException RSA Public Key 생성 실패 시
     */
    public RSAPublicKey toRSAPublicKey() {
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            BigInteger modulusBigInt = new BigInteger(1, decoder.decode(modulus));
            BigInteger exponentBigInt = new BigInteger(1, decoder.decode(exponent));

            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulusBigInt, exponentBigInt);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create RSA Public Key from modulus and exponent", e);
        }
    }

    /**
     * 값 기반 생성
     *
     * @param kid Key ID (JWKS에서 Public Key를 식별하는 고유 ID)
     * @param modulus RSA Public Key Modulus (Base64 URL 인코딩)
     * @param exponent RSA Public Key Exponent (Base64 URL 인코딩)
     * @param kty Key Type (RSA 고정)
     * @param use Public Key Use (sig: 서명 검증용)
     * @param alg Algorithm (RS256 고정)
     * @return PublicKey
     * @throws IllegalArgumentException 필수 값이 null이거나 형식이 잘못된 경우
     * @author development-team
     * @since 1.0.0
     */
    public static PublicKey of(
            String kid, String modulus, String exponent, String kty, String use, String alg) {
        return new PublicKey(kid, modulus, exponent, kty, use, alg);
    }

    /**
     * RSAPublicKey로부터 PublicKey 생성
     *
     * @param kid Key ID
     * @param rsaPublicKey Java RSAPublicKey
     * @return PublicKey
     * @author development-team
     * @since 1.0.0
     */
    public static PublicKey fromRSAPublicKey(String kid, RSAPublicKey rsaPublicKey) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String modulusBase64 = encoder.encodeToString(rsaPublicKey.getModulus().toByteArray());
        String exponentBase64 =
                encoder.encodeToString(rsaPublicKey.getPublicExponent().toByteArray());

        return new PublicKey(kid, modulusBase64, exponentBase64, "RSA", "sig", "RS256");
    }
}
