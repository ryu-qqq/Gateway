package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtClaims 테스트")
class JwtClaimsTest {

    private static final String VALID_SUBJECT = "user123";
    private static final String VALID_ISSUER = "auth-service";
    private static final Instant FUTURE_TIME = Instant.parse("2025-12-31T23:59:59Z");
    private static final Instant PAST_TIME = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant ISSUED_TIME = Instant.parse("2025-01-01T00:00:00Z");

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 JwtClaims 생성")
        void shouldCreateJwtClaims() {
            // given
            List<String> roles = List.of("USER", "ADMIN");
            String tenantId = "tenant123";
            String organizationId = "org123";
            String permissionHash = "hash123";

            // when
            JwtClaims claims =
                    new JwtClaims(
                            VALID_SUBJECT,
                            VALID_ISSUER,
                            FUTURE_TIME,
                            ISSUED_TIME,
                            roles,
                            tenantId,
                            organizationId,
                            permissionHash,
                            false);

            // then
            assertThat(claims.subject()).isEqualTo(VALID_SUBJECT);
            assertThat(claims.issuer()).isEqualTo(VALID_ISSUER);
            assertThat(claims.expiresAt()).isEqualTo(FUTURE_TIME);
            assertThat(claims.issuedAt()).isEqualTo(ISSUED_TIME);
            assertThat(claims.roles()).isEqualTo(roles);
            assertThat(claims.tenantId()).isEqualTo(tenantId);
            assertThat(claims.organizationId()).isEqualTo(organizationId);
            assertThat(claims.permissionHash()).isEqualTo(permissionHash);
            assertThat(claims.mfaVerified()).isFalse();
        }

        @Test
        @DisplayName("null roles는 빈 리스트로 초기화")
        void shouldInitializeNullRolesToEmptyList() {
            // when
            JwtClaims claims =
                    new JwtClaims(
                            VALID_SUBJECT,
                            VALID_ISSUER,
                            FUTURE_TIME,
                            ISSUED_TIME,
                            null,
                            null,
                            null,
                            null,
                            false);

            // then
            assertThat(claims.roles()).isEmpty();
            assertThat(claims.tenantId()).isNull();
            assertThat(claims.organizationId()).isNull();
            assertThat(claims.permissionHash()).isNull();
        }

        @Test
        @DisplayName("roles는 불변 복사본으로 저장")
        void shouldCreateImmutableCopyOfRoles() {
            // given - mutable List 사용하여 불변성 검증
            java.util.List<String> originalRoles = new java.util.ArrayList<>();
            originalRoles.add("USER");

            // when
            JwtClaims claims =
                    new JwtClaims(
                            VALID_SUBJECT,
                            VALID_ISSUER,
                            FUTURE_TIME,
                            ISSUED_TIME,
                            originalRoles,
                            null,
                            null,
                            null,
                            false);

            // then - 원본 수정해도 내부 상태에 영향 없음
            originalRoles.add("ADMIN");
            assertThat(claims.roles()).hasSize(1);
            assertThat(claims.roles()).containsExactly("USER");
        }

        @Test
        @DisplayName("subject가 null이면 예외 발생")
        void shouldThrowExceptionWhenSubjectIsNull() {
            assertThatThrownBy(
                            () ->
                                    new JwtClaims(
                                            null,
                                            VALID_ISSUER,
                                            FUTURE_TIME,
                                            ISSUED_TIME,
                                            List.of(),
                                            null,
                                            null,
                                            null,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Subject (sub) cannot be null or blank");
        }

        @Test
        @DisplayName("subject가 빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenSubjectIsBlank() {
            assertThatThrownBy(
                            () ->
                                    new JwtClaims(
                                            "   ",
                                            VALID_ISSUER,
                                            FUTURE_TIME,
                                            ISSUED_TIME,
                                            List.of(),
                                            null,
                                            null,
                                            null,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Subject (sub) cannot be null or blank");
        }

        @Test
        @DisplayName("issuer가 null이면 예외 발생")
        void shouldThrowExceptionWhenIssuerIsNull() {
            assertThatThrownBy(
                            () ->
                                    new JwtClaims(
                                            VALID_SUBJECT,
                                            null,
                                            FUTURE_TIME,
                                            ISSUED_TIME,
                                            List.of(),
                                            null,
                                            null,
                                            null,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Issuer (iss) cannot be null or blank");
        }

        @Test
        @DisplayName("issuer가 빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenIssuerIsBlank() {
            assertThatThrownBy(
                            () ->
                                    new JwtClaims(
                                            VALID_SUBJECT,
                                            "",
                                            FUTURE_TIME,
                                            ISSUED_TIME,
                                            List.of(),
                                            null,
                                            null,
                                            null,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Issuer (iss) cannot be null or blank");
        }

        @Test
        @DisplayName("expiresAt이 null이면 예외 발생")
        void shouldThrowExceptionWhenExpiresAtIsNull() {
            assertThatThrownBy(
                            () ->
                                    new JwtClaims(
                                            VALID_SUBJECT,
                                            VALID_ISSUER,
                                            null,
                                            ISSUED_TIME,
                                            List.of(),
                                            null,
                                            null,
                                            null,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ExpiresAt (exp) cannot be null");
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("of() 메서드로 기본 JwtClaims 생성 (roles 없음)")
        void shouldCreateJwtClaimsWithBasicInfo() {
            // when
            JwtClaims claims = JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME);

            // then
            assertThat(claims.subject()).isEqualTo(VALID_SUBJECT);
            assertThat(claims.issuer()).isEqualTo(VALID_ISSUER);
            assertThat(claims.expiresAt()).isEqualTo(FUTURE_TIME);
            assertThat(claims.issuedAt()).isEqualTo(ISSUED_TIME);
            assertThat(claims.roles()).isEmpty();
            assertThat(claims.tenantId()).isNull();
            assertThat(claims.permissionHash()).isNull();
        }

        @Test
        @DisplayName("of() 메서드로 roles 포함 JwtClaims 생성")
        void shouldCreateJwtClaimsWithRoles() {
            // given
            List<String> roles = List.of("USER", "ADMIN");

            // when
            JwtClaims claims =
                    JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME, roles);

            // then
            assertThat(claims.subject()).isEqualTo(VALID_SUBJECT);
            assertThat(claims.issuer()).isEqualTo(VALID_ISSUER);
            assertThat(claims.expiresAt()).isEqualTo(FUTURE_TIME);
            assertThat(claims.issuedAt()).isEqualTo(ISSUED_TIME);
            assertThat(claims.roles()).isEqualTo(roles);
            assertThat(claims.tenantId()).isNull();
            assertThat(claims.permissionHash()).isNull();
        }

        @Test
        @DisplayName("of() 메서드로 전체 정보 포함 JwtClaims 생성")
        void shouldCreateJwtClaimsWithFullInfo() {
            // given
            List<String> roles = List.of("USER", "ADMIN");
            String tenantId = "tenant123";
            String permissionHash = "hash123";

            // when
            JwtClaims claims =
                    JwtClaims.of(
                            VALID_SUBJECT,
                            VALID_ISSUER,
                            FUTURE_TIME,
                            ISSUED_TIME,
                            roles,
                            tenantId,
                            permissionHash);

            // then
            assertThat(claims.subject()).isEqualTo(VALID_SUBJECT);
            assertThat(claims.issuer()).isEqualTo(VALID_ISSUER);
            assertThat(claims.expiresAt()).isEqualTo(FUTURE_TIME);
            assertThat(claims.issuedAt()).isEqualTo(ISSUED_TIME);
            assertThat(claims.roles()).isEqualTo(roles);
            assertThat(claims.tenantId()).isEqualTo(tenantId);
            assertThat(claims.permissionHash()).isEqualTo(permissionHash);
        }
    }

    @Nested
    @DisplayName("isExpired() 테스트")
    class IsExpiredTest {

        @Test
        @DisplayName("만료되지 않은 JWT는 false 반환")
        void shouldReturnFalseWhenNotExpired() {
            // given
            JwtClaims claims = JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME);
            Clock fixedClock = Clock.fixed(ISSUED_TIME, ZoneOffset.UTC);

            // when & then
            assertThat(claims.isExpired(fixedClock)).isFalse();
        }

        @Test
        @DisplayName("만료된 JWT는 true 반환")
        void shouldReturnTrueWhenExpired() {
            // given
            JwtClaims claims = JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, PAST_TIME, ISSUED_TIME);
            Clock fixedClock = Clock.fixed(FUTURE_TIME, ZoneOffset.UTC);

            // when & then
            assertThat(claims.isExpired(fixedClock)).isTrue();
        }

        @Test
        @DisplayName("정확히 만료 시간과 같으면 false 반환")
        void shouldReturnFalseWhenExactlyAtExpirationTime() {
            // given
            Instant expirationTime = Instant.parse("2025-06-15T12:00:00Z");
            JwtClaims claims =
                    JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, expirationTime, ISSUED_TIME);
            Clock fixedClock = Clock.fixed(expirationTime, ZoneOffset.UTC);

            // when & then
            assertThat(claims.isExpired(fixedClock)).isFalse();
        }

        @Test
        @DisplayName("만료 시간보다 1초 후면 true 반환")
        void shouldReturnTrueWhenOneSecondAfterExpiration() {
            // given
            Instant expirationTime = Instant.parse("2025-06-15T12:00:00Z");
            Instant oneSecondAfter = expirationTime.plusSeconds(1);
            JwtClaims claims =
                    JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, expirationTime, ISSUED_TIME);
            Clock fixedClock = Clock.fixed(oneSecondAfter, ZoneOffset.UTC);

            // when & then
            assertThat(claims.isExpired(fixedClock)).isTrue();
        }

        @Test
        @DisplayName("Clock이 null이면 예외 발생")
        void shouldThrowExceptionWhenClockIsNull() {
            // given
            JwtClaims claims = JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME);

            // when & then
            assertThatThrownBy(() -> claims.isExpired(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Clock cannot be null");
        }

        @Test
        @DisplayName("시스템 시계를 사용하는 isExpired() 메서드")
        void shouldUseSystemClockForExpiration() {
            // given - 과거 시간으로 만료된 JWT
            JwtClaims expiredClaims =
                    JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, PAST_TIME, ISSUED_TIME);

            // given - 미래 시간으로 유효한 JWT
            JwtClaims validClaims =
                    JwtClaims.of(VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME);

            // when & then
            assertThat(expiredClaims.isExpired()).isTrue();
            assertThat(validClaims.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("roles 리스트는 수정할 수 없음")
        void shouldNotAllowModificationOfRoles() {
            // given
            JwtClaims claims =
                    JwtClaims.of(
                            VALID_SUBJECT, VALID_ISSUER, FUTURE_TIME, ISSUED_TIME, List.of("USER"));

            // when & then
            assertThatThrownBy(() -> claims.roles().add("ADMIN"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
