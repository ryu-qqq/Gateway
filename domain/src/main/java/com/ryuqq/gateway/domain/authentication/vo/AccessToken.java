package com.ryuqq.gateway.domain.authentication.vo;

import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import java.util.Base64;

/**
 * JWT Access Token Value Object
 *
 * <p>JWT Access Token 문자열을 파싱하고 검증하는 불변 객체
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>JWT는 Header.Payload.Signature 형식이어야 한다 (3개 파트)
 *   <li>Header는 유효한 JSON이어야 한다
 *   <li>kid (Key ID)는 Header에 존재해야 한다
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>JWT 문자열 파싱 및 구조 검증
 *   <li>Header에서 kid 추출
 *   <li>JWT 원본 값 제공 (서명 검증용)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class AccessToken {

    private static final int JWT_PARTS_COUNT = 3;
    private static final int HEADER_INDEX = 0;

    private final String value;
    private final String kid;

    /** Private Constructor (정적 팩토리 메서드 사용) */
    private AccessToken(String value, String kid) {
        this.value = value;
        this.kid = kid;
    }

    /**
     * JWT Access Token 생성
     *
     * <p>JWT 문자열을 파싱하여 AccessToken을 생성합니다.
     *
     * @param tokenValue JWT Access Token 문자열
     * @return AccessToken
     * @throws JwtInvalidException JWT 형식이 잘못된 경우
     */
    public static AccessToken of(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new JwtInvalidException("Access token cannot be null or blank");
        }

        String[] parts = tokenValue.split("\\.");
        if (parts.length != JWT_PARTS_COUNT) {
            throw new JwtInvalidException(
                    "Invalid JWT format: expected 3 parts but got " + parts.length);
        }

        String kid = extractKidFromHeader(parts[HEADER_INDEX]);
        return new AccessToken(tokenValue, kid);
    }

    /**
     * JWT Header에서 kid 추출
     *
     * @param encodedHeader Base64 URL 인코딩된 Header
     * @return kid (Key ID)
     * @throws JwtInvalidException Header 파싱 실패 또는 kid 누락
     */
    private static String extractKidFromHeader(String encodedHeader) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(encodedHeader));

            if (!headerJson.contains("\"kid\"")) {
                throw new JwtInvalidException("JWT Header does not contain 'kid' claim");
            }

            String kid = headerJson.replaceAll(".*\"kid\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            if (kid.equals(headerJson)) {
                throw new JwtInvalidException("Failed to extract 'kid' from JWT Header");
            }

            return kid;
        } catch (JwtInvalidException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtInvalidException("Failed to decode JWT Header: " + e.getMessage());
        }
    }

    /**
     * Key ID 조회
     *
     * @return kid (JWT Header의 Key ID)
     */
    public String getKid() {
        return kid;
    }

    /**
     * JWT 원본 값 조회
     *
     * <p>서명 검증 시 사용됩니다.
     *
     * @return JWT Access Token 원본 문자열
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccessToken that = (AccessToken) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "AccessToken{kid='" + kid + "'}";
    }
}
