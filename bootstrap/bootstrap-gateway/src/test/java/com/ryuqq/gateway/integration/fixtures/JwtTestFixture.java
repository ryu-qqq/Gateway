package com.ryuqq.gateway.integration.fixtures;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JWT Test Fixture
 *
 * <p>Integration Test를 위한 JWT 생성 유틸리티
 *
 * @author development-team
 * @since 1.0.0
 */
public final class JwtTestFixture {

    private static final KeyPair KEY_PAIR = generateRSAKeyPair();
    private static final KeyPair WRONG_KEY_PAIR = generateRSAKeyPair();
    private static final String DEFAULT_KID = "test-key-2025";
    private static final String DEFAULT_ISSUER = "auth-hub";
    private static final String DEFAULT_TENANT_ID = "tenant-001";
    private static final String DEFAULT_ORGANIZATION_ID = "org-001";
    private static final String DEFAULT_PERMISSION_HASH = "abc123hash";

    private JwtTestFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 유효한 JWT 생성
     *
     * @return 유효한 JWT 문자열
     */
    public static String aValidJwt() {
        return createJwt(
                "user-123",
                List.of("USER"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                DEFAULT_TENANT_ID,
                DEFAULT_PERMISSION_HASH);
    }

    /**
     * 유효한 JWT 생성 (커스텀 subject)
     *
     * @param subject 사용자 ID
     * @return JWT 문자열
     */
    public static String aValidJwt(String subject) {
        return createJwt(
                subject,
                List.of("USER"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                DEFAULT_TENANT_ID,
                DEFAULT_PERMISSION_HASH);
    }

    /**
     * 유효한 JWT 생성 (커스텀 roles)
     *
     * @param subject 사용자 ID
     * @param roles 권한 목록
     * @return JWT 문자열
     */
    public static String aValidJwt(String subject, List<String> roles) {
        return createJwt(
                subject,
                roles,
                Instant.now().plus(1, ChronoUnit.HOURS),
                DEFAULT_TENANT_ID,
                DEFAULT_PERMISSION_HASH);
    }

    /**
     * 유효한 JWT 생성 (커스텀 tenantId)
     *
     * @param subject 사용자 ID
     * @param tenantId 테넌트 ID
     * @return JWT 문자열
     */
    public static String aValidJwtWithTenant(String subject, String tenantId) {
        return createJwt(
                subject,
                List.of("USER"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                tenantId,
                DEFAULT_PERMISSION_HASH,
                false);
    }

    /**
     * 유효한 JWT 생성 (MFA 검증됨)
     *
     * @param subject 사용자 ID
     * @param tenantId 테넌트 ID
     * @param mfaVerified MFA 검증 여부
     * @return JWT 문자열
     */
    public static String aValidJwtWithMfa(String subject, String tenantId, boolean mfaVerified) {
        return createJwt(
                subject,
                List.of("USER"),
                Instant.now().plus(1, ChronoUnit.HOURS),
                tenantId,
                DEFAULT_PERMISSION_HASH,
                mfaVerified);
    }

    /**
     * 만료된 JWT 생성
     *
     * @return 만료된 JWT 문자열
     */
    public static String anExpiredJwt() {
        return createJwt(
                "user-123",
                List.of("USER"),
                Instant.now().minus(1, ChronoUnit.HOURS),
                DEFAULT_TENANT_ID,
                DEFAULT_PERMISSION_HASH,
                false);
    }

    /**
     * 잘못된 서명의 JWT 생성
     *
     * @return 잘못된 서명의 JWT 문자열
     */
    public static String aJwtWithInvalidSignature() {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(DEFAULT_KID).build();

            JWTClaimsSet claimsSet =
                    // AuthHub JWT claim 이름: tid, oid, permission_hash, mfa_verified (snake_case)
                    new JWTClaimsSet.Builder()
                            .subject("user-123")
                            .issuer(DEFAULT_ISSUER)
                            .claim("roles", List.of("USER"))
                            .claim("tid", DEFAULT_TENANT_ID)
                            .claim("oid", DEFAULT_ORGANIZATION_ID)
                            .claim("permission_hash", DEFAULT_PERMISSION_HASH)
                            .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                            .issueTime(Date.from(Instant.now()))
                            .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) WRONG_KEY_PAIR.getPrivate());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT with invalid signature", e);
        }
    }

    /**
     * JWKS 응답 JSON 생성
     *
     * @return JWKS 형식 JSON 문자열
     */
    public static String jwksResponse() {
        RSAPublicKey publicKey = (RSAPublicKey) KEY_PAIR.getPublic();
        String n =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(publicKey.getModulus().toByteArray());
        String e =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(publicKey.getPublicExponent().toByteArray());

        return String.format(
                """
                {
                    "keys": [
                        {
                            "kty": "RSA",
                            "kid": "%s",
                            "use": "sig",
                            "alg": "RS256",
                            "n": "%s",
                            "e": "%s"
                        }
                    ]
                }
                """,
                DEFAULT_KID, n, e);
    }

    /**
     * 기본 kid 반환
     *
     * @return 기본 Key ID
     */
    public static String defaultKid() {
        return DEFAULT_KID;
    }

    /**
     * RSA Public Key 반환
     *
     * @return RSAPublicKey
     */
    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    private static String createJwt(
            String subject,
            List<String> roles,
            Instant expiresAt,
            String tenantId,
            String permissionHash) {
        return createJwt(subject, roles, expiresAt, tenantId, permissionHash, false);
    }

    private static String createJwt(
            String subject,
            List<String> roles,
            Instant expiresAt,
            String tenantId,
            String permissionHash,
            boolean mfaVerified) {
        return createJwt(
                subject,
                roles,
                expiresAt,
                tenantId,
                DEFAULT_ORGANIZATION_ID,
                permissionHash,
                mfaVerified);
    }

    private static String createJwt(
            String subject,
            List<String> roles,
            Instant expiresAt,
            String tenantId,
            String organizationId,
            String permissionHash,
            boolean mfaVerified) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(DEFAULT_KID).build();

            // AuthHub JWT claim 이름: tid, oid, permission_hash, mfa_verified (snake_case)
            JWTClaimsSet claimsSet =
                    new JWTClaimsSet.Builder()
                            .subject(subject)
                            .issuer(DEFAULT_ISSUER)
                            .claim("roles", roles)
                            .claim("tid", tenantId)
                            .claim("oid", organizationId)
                            .claim("permission_hash", permissionHash)
                            .claim("mfa_verified", mfaVerified)
                            .expirationTime(Date.from(expiresAt))
                            .issueTime(Date.from(Instant.now()))
                            .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT", e);
        }
    }

    private static KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
}
