package com.ryuqq.gateway.application.authentication.component;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ryuqq.gateway.application.authentication.validator.JwtValidator;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

@DisplayName("JwtValidator 단위 테스트")
class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;
    private PublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        jwtValidator = new JwtValidator();

        // RSA 키 쌍 생성
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

        publicKey = PublicKey.fromRSAPublicKey("test-kid", rsaPublicKey);
    }

    @Nested
    @DisplayName("JWT 서명 검증")
    class SignatureVerification {

        @Test
        @DisplayName("유효한 서명이면 true 반환")
        void returnTrueForValidSignature() throws Exception {
            // given
            String accessToken = createValidJwt();

            // when & then
            StepVerifier.create(jwtValidator.verifySignature(accessToken, publicKey))
                    .assertNext(isValid -> assertThat(isValid).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("잘못된 서명이면 false 반환")
        void returnFalseForInvalidSignature() throws Exception {
            // given
            String accessToken = createValidJwt();

            // 다른 키 쌍 생성
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey differentPublicKey = (RSAPublicKey) differentKeyPair.getPublic();

            PublicKey wrongPublicKey = PublicKey.fromRSAPublicKey("wrong-kid", differentPublicKey);

            // when & then
            StepVerifier.create(jwtValidator.verifySignature(accessToken, wrongPublicKey))
                    .assertNext(isValid -> assertThat(isValid).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("잘못된 JWT 형식이면 예외 발생")
        void throwExceptionForInvalidJwtFormat() {
            // given
            String invalidToken = "invalid.jwt.token";

            // when & then
            StepVerifier.create(jwtValidator.verifySignature(invalidToken, publicKey))
                    .expectError(IllegalStateException.class)
                    .verify();
        }

        @Test
        @DisplayName("빈 토큰이면 예외 발생")
        void throwExceptionForEmptyToken() {
            // when & then
            StepVerifier.create(jwtValidator.verifySignature("", publicKey))
                    .expectError(IllegalStateException.class)
                    .verify();
        }

        @Test
        @DisplayName("null 토큰이면 예외 발생")
        void throwExceptionForNullToken() {
            // when & then
            StepVerifier.create(jwtValidator.verifySignature(null, publicKey))
                    .expectError(IllegalStateException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("JWT Claims 추출")
    class ClaimsExtraction {

        @Test
        @DisplayName("유효한 JWT에서 Claims 추출 성공")
        void extractClaimsFromValidJwt() throws Exception {
            // given
            // JWT의 Date 변환 시 밀리초 이하가 손실되므로 초 단위로 truncate
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

            String accessToken =
                    createJwtWithClaims(
                            "user-123",
                            "test-issuer",
                            expiresAt,
                            now,
                            List.of("ADMIN", "USER"),
                            "tenant-456",
                            "org-789",
                            "hash-abc");

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.subject()).isEqualTo("user-123");
                                assertThat(claims.issuer()).isEqualTo("test-issuer");
                                assertThat(claims.expiresAt()).isEqualTo(expiresAt);
                                assertThat(claims.issuedAt()).isEqualTo(now);
                                assertThat(claims.roles()).containsExactly("ADMIN", "USER");
                                assertThat(claims.tenantId()).isEqualTo("tenant-456");
                                assertThat(claims.organizationId()).isEqualTo("org-789");
                                assertThat(claims.permissionHash()).isEqualTo("hash-abc");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("roles가 없는 JWT에서 빈 리스트 반환")
        void returnEmptyListWhenRolesAbsent() throws Exception {
            // given
            Instant now = Instant.now();
            Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

            JWTClaimsSet claimsSet =
                    new JWTClaimsSet.Builder()
                            .subject("user-123")
                            .issuer("test-issuer")
                            .expirationTime(Date.from(expiresAt))
                            .issueTime(Date.from(now))
                            .build();

            String accessToken = signJwt(claimsSet);

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.roles()).isEmpty();
                                assertThat(claims.tenantId()).isNull();
                                assertThat(claims.organizationId()).isNull();
                                assertThat(claims.permissionHash()).isNull();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("tenantId, organizationId, permissionHash가 없는 JWT 처리")
        void handleJwtWithoutOptionalClaims() throws Exception {
            // given
            Instant now = Instant.now();
            Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

            String accessToken =
                    createJwtWithClaims(
                            "user-123",
                            "test-issuer",
                            expiresAt,
                            now,
                            List.of("USER"),
                            null,
                            null,
                            null);

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.subject()).isEqualTo("user-123");
                                assertThat(claims.tenantId()).isNull();
                                assertThat(claims.organizationId()).isNull();
                                assertThat(claims.permissionHash()).isNull();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("issuedAt이 없는 JWT 처리")
        void handleJwtWithoutIssuedAt() throws Exception {
            // given
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

            JWTClaimsSet claimsSet =
                    new JWTClaimsSet.Builder()
                            .subject("user-123")
                            .issuer("test-issuer")
                            .expirationTime(Date.from(expiresAt))
                            .build();

            String accessToken = signJwt(claimsSet);

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.subject()).isEqualTo("user-123");
                                assertThat(claims.issuedAt()).isNull();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("잘못된 JWT 형식이면 예외 발생")
        void throwExceptionForInvalidJwtFormat() {
            // given
            String invalidToken = "invalid.jwt.token";

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(invalidToken))
                    .expectError(IllegalStateException.class)
                    .verify();
        }

        @Test
        @DisplayName("null 토큰이면 예외 발생")
        void throwExceptionForNullToken() {
            // when & then
            StepVerifier.create(jwtValidator.extractClaims(null))
                    .expectError(IllegalStateException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("만료된 JWT 처리")
    class ExpiredJwtHandling {

        @Test
        @DisplayName("만료된 JWT에서도 Claims 추출 가능")
        void extractClaimsFromExpiredJwt() throws Exception {
            // given
            // JWT의 Date 변환 시 밀리초 이하가 손실되므로 초 단위로 truncate
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant expiredAt = now.minus(1, ChronoUnit.HOURS);

            String accessToken =
                    createJwtWithClaims(
                            "user-123",
                            "test-issuer",
                            expiredAt,
                            now.minus(2, ChronoUnit.HOURS),
                            List.of("USER"),
                            "tenant-456",
                            "org-789",
                            "hash-abc");

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.subject()).isEqualTo("user-123");
                                assertThat(claims.expiresAt()).isEqualTo(expiredAt);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("만료된 JWT도 서명 검증 가능")
        void verifySignatureOfExpiredJwt() throws Exception {
            // given
            Instant now = Instant.now();
            Instant expiredAt = now.minus(1, ChronoUnit.HOURS);

            String accessToken =
                    createJwtWithClaims(
                            "user-123",
                            "test-issuer",
                            expiredAt,
                            now.minus(2, ChronoUnit.HOURS),
                            List.of("USER"),
                            null,
                            null,
                            null);

            // when & then
            StepVerifier.create(jwtValidator.verifySignature(accessToken, publicKey))
                    .assertNext(isValid -> assertThat(isValid).isTrue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("특수 케이스")
    class SpecialCases {

        @Test
        @DisplayName("매우 긴 roles 리스트 처리")
        void handleLongRolesList() throws Exception {
            // given
            Instant now = Instant.now();
            Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

            List<String> manyRoles =
                    List.of(
                            "ROLE1", "ROLE2", "ROLE3", "ROLE4", "ROLE5", "ROLE6", "ROLE7", "ROLE8",
                            "ROLE9", "ROLE10");

            String accessToken =
                    createJwtWithClaims(
                            "user-123",
                            "test-issuer",
                            expiresAt,
                            now,
                            manyRoles,
                            "tenant-456",
                            "org-789",
                            "hash-abc");

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.roles()).hasSize(10);
                                assertThat(claims.roles()).containsExactlyElementsOf(manyRoles);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("특수 문자가 포함된 Claims 처리")
        void handleSpecialCharactersInClaims() throws Exception {
            // given
            Instant now = Instant.now();
            Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

            String accessToken =
                    createJwtWithClaims(
                            "user@example.com",
                            "https://auth.example.com",
                            expiresAt,
                            now,
                            List.of("ADMIN", "USER"),
                            "tenant-123-abc",
                            "org-456-def",
                            "hash+abc/123==");

            // when & then
            StepVerifier.create(jwtValidator.extractClaims(accessToken))
                    .assertNext(
                            claims -> {
                                assertThat(claims.subject()).isEqualTo("user@example.com");
                                assertThat(claims.issuer()).isEqualTo("https://auth.example.com");
                                assertThat(claims.tenantId()).isEqualTo("tenant-123-abc");
                                assertThat(claims.organizationId()).isEqualTo("org-456-def");
                                assertThat(claims.permissionHash()).isEqualTo("hash+abc/123==");
                            })
                    .verifyComplete();
        }
    }

    // Helper methods
    private String createValidJwt() throws Exception {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        return createJwtWithClaims(
                "user-123",
                "test-issuer",
                expiresAt,
                now,
                List.of("USER"),
                "tenant-456",
                "org-789",
                "hash-abc");
    }

    private String createJwtWithClaims(
            String subject,
            String issuer,
            Instant expiresAt,
            Instant issuedAt,
            List<String> roles,
            String tenantId,
            String organizationId,
            String permissionHash)
            throws Exception {
        JWTClaimsSet.Builder builder =
                new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer(issuer)
                        .expirationTime(Date.from(expiresAt));

        if (issuedAt != null) {
            builder.issueTime(Date.from(issuedAt));
        }

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        // AuthHub JWT claim 이름: tid, oid, permission_hash (snake_case)
        if (tenantId != null) {
            builder.claim("tid", tenantId);
        }

        if (organizationId != null) {
            builder.claim("oid", organizationId);
        }

        if (permissionHash != null) {
            builder.claim("permission_hash", permissionHash);
        }

        return signJwt(builder.build());
    }

    private String signJwt(JWTClaimsSet claimsSet) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-kid").build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        RSASSASigner signer = new RSASSASigner(rsaPrivateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}
