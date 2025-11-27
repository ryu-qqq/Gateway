package com.ryuqq.gateway.domain.authentication.vo;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * JWT Claims Value Object
 *
 * <p>JWT Payload에서 추출한 Claims를 표현하는 불변 객체
 *
 * <p><strong>도메인 규칙</strong>:
 *
 * <ul>
 *   <li>subject (사용자 ID)는 필수이며 null이거나 빈 문자열일 수 없다
 *   <li>issuer (발급자)는 필수이며 null이거나 빈 문자열일 수 없다
 *   <li>expiresAt (만료 시간)은 필수이다
 *   <li>issuedAt (발급 시간)은 선택적이다
 *   <li>roles는 빈 리스트일 수 있으나 null일 수 없다
 *   <li>tenantId (테넌트 ID)는 선택적이다 (null일 수 있음)
 *   <li>permissionHash (권한 해시)는 선택적이다 (null일 수 있음)
 * </ul>
 *
 * @param subject 사용자 ID (JWT sub claim)
 * @param issuer 발급자 (JWT iss claim)
 * @param expiresAt 만료 시간 (JWT exp claim)
 * @param issuedAt 발급 시간 (JWT iat claim, nullable)
 * @param roles 사용자 권한 목록 (JWT roles claim)
 * @param tenantId 테넌트 ID (JWT tenantId claim, nullable)
 * @param permissionHash 권한 해시 (JWT permissionHash claim, nullable)
 * @param mfaVerified MFA 검증 여부 (JWT mfaVerified claim)
 * @author development-team
 * @since 1.0.0
 */
public record JwtClaims(
        String subject,
        String issuer,
        Instant expiresAt,
        Instant issuedAt,
        List<String> roles,
        String tenantId,
        String permissionHash,
        boolean mfaVerified) {

    /** Compact Constructor (검증 로직) */
    public JwtClaims {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject (sub) cannot be null or blank");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Issuer (iss) cannot be null or blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt (exp) cannot be null");
        }
        // 불변 복사본으로 저장하여 외부 변경 방지
        roles = roles == null ? List.of() : List.copyOf(roles);
        // tenantId와 permissionHash는 null 허용 (선택적)
    }

    /**
     * JWT Claims 생성 (roles 없음)
     *
     * @param subject 사용자 ID
     * @param issuer 발급자
     * @param expiresAt 만료 시간
     * @param issuedAt 발급 시간
     * @return JwtClaims
     */
    public static JwtClaims of(String subject, String issuer, Instant expiresAt, Instant issuedAt) {
        return new JwtClaims(subject, issuer, expiresAt, issuedAt, List.of(), null, null, false);
    }

    /**
     * JWT Claims 생성 (roles 포함)
     *
     * @param subject 사용자 ID
     * @param issuer 발급자
     * @param expiresAt 만료 시간
     * @param issuedAt 발급 시간
     * @param roles 권한 목록
     * @return JwtClaims
     */
    public static JwtClaims of(
            String subject,
            String issuer,
            Instant expiresAt,
            Instant issuedAt,
            List<String> roles) {
        return new JwtClaims(subject, issuer, expiresAt, issuedAt, roles, null, null, false);
    }

    /**
     * JWT Claims 생성 (전체 정보, mfaVerified 제외 - 기본 false)
     *
     * @param subject 사용자 ID
     * @param issuer 발급자
     * @param expiresAt 만료 시간
     * @param issuedAt 발급 시간
     * @param roles 권한 목록
     * @param tenantId 테넌트 ID
     * @param permissionHash 권한 해시
     * @return JwtClaims
     */
    public static JwtClaims of(
            String subject,
            String issuer,
            Instant expiresAt,
            Instant issuedAt,
            List<String> roles,
            String tenantId,
            String permissionHash) {
        return new JwtClaims(
                subject, issuer, expiresAt, issuedAt, roles, tenantId, permissionHash, false);
    }

    /**
     * JWT Claims 생성 (전체 정보, mfaVerified 포함)
     *
     * @param subject 사용자 ID
     * @param issuer 발급자
     * @param expiresAt 만료 시간
     * @param issuedAt 발급 시간
     * @param roles 권한 목록
     * @param tenantId 테넌트 ID
     * @param permissionHash 권한 해시
     * @param mfaVerified MFA 검증 여부
     * @return JwtClaims
     */
    public static JwtClaims of(
            String subject,
            String issuer,
            Instant expiresAt,
            Instant issuedAt,
            List<String> roles,
            String tenantId,
            String permissionHash,
            boolean mfaVerified) {
        return new JwtClaims(
                subject, issuer, expiresAt, issuedAt, roles, tenantId, permissionHash, mfaVerified);
    }

    /**
     * JWT 만료 여부 검증 (시스템 시계 사용)
     *
     * <p>현재 시간(UTC)이 만료 시간을 지났는지 확인합니다.
     *
     * @return 만료되었으면 true, 아니면 false
     * @author development-team
     * @since 1.0.0
     */
    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    /**
     * JWT 만료 여부 검증 (테스트 가능)
     *
     * <p>지정된 Clock으로 현재 시간을 측정하여 만료 여부를 확인합니다.
     *
     * <p>테스트 시 Clock.fixed()를 사용하여 시간을 제어할 수 있습니다.
     *
     * @param clock 시간 측정에 사용할 Clock (null 불가)
     * @return 만료되었으면 true, 아니면 false
     * @throws IllegalArgumentException clock이 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public boolean isExpired(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock cannot be null");
        }
        return Instant.now(clock).isAfter(expiresAt);
    }
}
