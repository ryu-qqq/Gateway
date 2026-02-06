package com.ryuqq.gateway.application.authentication.validator;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * JWT Validation Service
 *
 * <p>JWT 서명 검증 및 Claims 추출을 담당하는 Application 내부 서비스
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>JWT 서명 검증 (RS256)
 *   <li>JWT Claims 추출
 * </ul>
 *
 * <p><strong>설계 결정</strong>:
 *
 * <ul>
 *   <li>Port가 아닌 Application 내부 서비스
 *   <li>nimbus-jose-jwt 라이브러리 사용
 *   <li>외부 시스템 연동 없음 (순수 로직)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class JwtValidator {

    /**
     * JWT 서명 검증
     *
     * @param accessToken JWT Access Token
     * @param publicKey RSA Public Key
     * @return Mono&lt;Boolean&gt; 서명이 유효하면 true, 아니면 false
     */
    public Mono<Boolean> verifySignature(String accessToken, PublicKey publicKey) {
        return Mono.fromCallable(
                        () -> {
                            SignedJWT signedJWT = SignedJWT.parse(accessToken);
                            RSAPublicKey rsaPublicKey = publicKey.toRSAPublicKey();
                            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
                            return signedJWT.verify(verifier);
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IllegalStateException("Failed to verify JWT signature", e));
    }

    /**
     * JWT Claims 추출
     *
     * @param accessToken JWT Access Token
     * @return Mono&lt;JwtClaims&gt;
     */
    public Mono<JwtClaims> extractClaims(String accessToken) {
        return Mono.fromCallable(
                        () -> {
                            SignedJWT signedJWT = SignedJWT.parse(accessToken);
                            var claims = signedJWT.getJWTClaimsSet();

                            String subject = claims.getSubject();
                            String issuer = claims.getIssuer();
                            Instant expiresAt =
                                    claims.getExpirationTime() != null
                                            ? claims.getExpirationTime().toInstant()
                                            : null;
                            Instant issuedAt =
                                    claims.getIssueTime() != null
                                            ? claims.getIssueTime().toInstant()
                                            : null;
                            List<String> roles = claims.getStringListClaim("roles");
                            List<String> permissions = claims.getStringListClaim("permissions");
                            // AuthHub JWT claim 이름: tid, oid, permission_hash, mfa_verified
                            // (snake_case)
                            String tenantId = claims.getStringClaim("tid");
                            String organizationId = claims.getStringClaim("oid");
                            String permissionHash = claims.getStringClaim("permission_hash");
                            Boolean mfaVerifiedClaim = claims.getBooleanClaim("mfa_verified");
                            boolean mfaVerified = mfaVerifiedClaim != null && mfaVerifiedClaim;

                            return JwtClaims.of(
                                    subject,
                                    issuer,
                                    expiresAt,
                                    issuedAt,
                                    roles != null ? roles : List.of(),
                                    permissions != null ? permissions : List.of(),
                                    tenantId,
                                    organizationId,
                                    permissionHash,
                                    mfaVerified);
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IllegalStateException("Failed to extract JWT claims", e));
    }
}
