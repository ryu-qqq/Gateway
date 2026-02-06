package com.ryuqq.gateway.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.MfaRequiredException;
import com.ryuqq.gateway.domain.tenant.exception.SocialLoginNotAllowedException;
import com.ryuqq.gateway.domain.tenant.id.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantConfig Aggregate Root 테스트")
class TenantConfigTest {

    @Nested
    @DisplayName("of() 팩토리 메서드 테스트 - 전체 필드")
    class OfFullMethodTest {

        @Test
        @DisplayName("모든 필드로 TenantConfig 생성 성공")
        void shouldCreateWithAllFields() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");
            boolean mfaRequired = true;
            Set<SocialProvider> allowedSocialLogins =
                    Set.of(SocialProvider.KAKAO, SocialProvider.GOOGLE);
            Map<String, Set<String>> roleHierarchy = Map.of("ADMIN", Set.of("READ", "WRITE"));
            SessionConfig sessionConfig =
                    SessionConfig.of(10, Duration.ofMinutes(30), Duration.ofDays(14));
            TenantRateLimitConfig rateLimitConfig = TenantRateLimitConfig.of(20, 10);

            // when
            TenantConfig config =
                    TenantConfig.of(
                            tenantId,
                            mfaRequired,
                            allowedSocialLogins,
                            roleHierarchy,
                            sessionConfig,
                            rateLimitConfig);

            // then
            assertThat(config).isNotNull();
            assertThat(config.getTenantId()).isEqualTo(tenantId);
            assertThat(config.isMfaRequired()).isTrue();
            assertThat(config.getAllowedSocialLogins())
                    .containsExactlyInAnyOrder(SocialProvider.KAKAO, SocialProvider.GOOGLE);
            assertThat(config.getRoleHierarchy()).containsKey("ADMIN");
            assertThat(config.getSessionConfig()).isEqualTo(sessionConfig);
            assertThat(config.getRateLimitConfig()).isEqualTo(rateLimitConfig);
        }

        @Test
        @DisplayName("tenantId가 null이면 예외 발생")
        void shouldThrowExceptionWhenTenantIdIsNull() {
            assertThatThrownBy(
                            () ->
                                    TenantConfig.of(
                                            (TenantId) null, false, Set.of(), Map.of(), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tenantId cannot be null");
        }

        @Test
        @DisplayName("allowedSocialLogins가 null이면 빈 Set 사용")
        void shouldUseEmptySetWhenAllowedSocialLoginsIsNull() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");

            // when
            TenantConfig config = TenantConfig.of(tenantId, false, null, null, null, null);

            // then
            assertThat(config.getAllowedSocialLogins()).isEmpty();
        }

        @Test
        @DisplayName("roleHierarchy가 null이면 빈 Map 사용")
        void shouldUseEmptyMapWhenRoleHierarchyIsNull() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");

            // when
            TenantConfig config = TenantConfig.of(tenantId, false, null, null, null, null);

            // then
            assertThat(config.getRoleHierarchy()).isEmpty();
        }

        @Test
        @DisplayName("sessionConfig가 null이면 기본값 사용")
        void shouldUseDefaultSessionConfigWhenNull() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");

            // when
            TenantConfig config = TenantConfig.of(tenantId, false, null, null, null, null);

            // then
            assertThat(config.getSessionConfig()).isEqualTo(SessionConfig.defaultConfig());
        }

        @Test
        @DisplayName("rateLimitConfig가 null이면 기본값 사용")
        void shouldUseDefaultRateLimitConfigWhenNull() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");

            // when
            TenantConfig config = TenantConfig.of(tenantId, false, null, null, null, null);

            // then
            assertThat(config.getRateLimitConfig())
                    .isEqualTo(TenantRateLimitConfig.defaultConfig());
        }
    }

    @Nested
    @DisplayName("of() 팩토리 메서드 테스트 - 필수 필드만")
    class OfMinimalMethodTest {

        @Test
        @DisplayName("필수 필드만으로 TenantConfig 생성 성공")
        void shouldCreateWithMinimalFields() {
            // given
            TenantId tenantId = TenantId.of("tenant-1");

            // when
            TenantConfig config = TenantConfig.of(tenantId, true);

            // then
            assertThat(config.getTenantId()).isEqualTo(tenantId);
            assertThat(config.isMfaRequired()).isTrue();
            assertThat(config.getAllowedSocialLogins()).isEmpty();
            assertThat(config.getRoleHierarchy()).isEmpty();
        }
    }

    @Nested
    @DisplayName("of() 팩토리 메서드 테스트 - 문자열 tenantId")
    class OfStringMethodTest {

        @Test
        @DisplayName("문자열 tenantId로 TenantConfig 생성 성공")
        void shouldCreateWithStringTenantId() {
            // when
            TenantConfig config =
                    TenantConfig.of(
                            "tenant-1", false, Set.of(SocialProvider.NAVER), Map.of(), null, null);

            // then
            assertThat(config.getTenantIdValue()).isEqualTo("tenant-1");
        }
    }

    @Nested
    @DisplayName("validateMfa() 메서드 테스트")
    class ValidateMfaTest {

        @Test
        @DisplayName("MFA 필수 + mfaVerified=true이면 통과")
        void shouldPassWhenMfaRequiredAndVerified() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), true);

            // when & then - 예외가 발생하지 않아야 함
            config.validateMfa(true);
        }

        @Test
        @DisplayName("MFA 필수 + mfaVerified=false이면 예외 발생")
        void shouldThrowExceptionWhenMfaRequiredAndNotVerified() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), true);

            // when & then
            assertThatThrownBy(() -> config.validateMfa(false))
                    .isInstanceOf(MfaRequiredException.class);
        }

        @Test
        @DisplayName("MFA 필수 + mfaVerified=null이면 예외 발생")
        void shouldThrowExceptionWhenMfaRequiredAndNull() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), true);

            // when & then
            assertThatThrownBy(() -> config.validateMfa(null))
                    .isInstanceOf(MfaRequiredException.class);
        }

        @Test
        @DisplayName("MFA 미필수면 mfaVerified 값과 관계없이 통과")
        void shouldPassWhenMfaNotRequired() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), false);

            // when & then - 예외가 발생하지 않아야 함
            config.validateMfa(false);
            config.validateMfa(null);
            config.validateMfa(true);
        }
    }

    @Nested
    @DisplayName("validateSocialLoginProvider() 메서드 테스트")
    class ValidateSocialLoginProviderTest {

        @Test
        @DisplayName("허용된 제공자면 통과")
        void shouldPassWhenProviderAllowed() {
            // given
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"),
                            false,
                            Set.of(SocialProvider.KAKAO),
                            Map.of(),
                            null,
                            null);

            // when & then - 예외가 발생하지 않아야 함
            config.validateSocialLoginProvider(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("허용되지 않은 제공자면 예외 발생")
        void shouldThrowExceptionWhenProviderNotAllowed() {
            // given
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"),
                            false,
                            Set.of(SocialProvider.KAKAO),
                            Map.of(),
                            null,
                            null);

            // when & then
            assertThatThrownBy(() -> config.validateSocialLoginProvider(SocialProvider.NAVER))
                    .isInstanceOf(SocialLoginNotAllowedException.class);
        }

        @Test
        @DisplayName("문자열 코드로 검증 - 허용된 제공자")
        void shouldPassWhenProviderCodeAllowed() {
            // given
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"),
                            false,
                            Set.of(SocialProvider.GOOGLE),
                            Map.of(),
                            null,
                            null);

            // when & then - 예외가 발생하지 않아야 함
            config.validateSocialLoginProvider("google");
        }

        @Test
        @DisplayName("문자열 코드로 검증 - 허용되지 않은 제공자")
        void shouldThrowExceptionWhenProviderCodeNotAllowed() {
            // given
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"),
                            false,
                            Set.of(SocialProvider.GOOGLE),
                            Map.of(),
                            null,
                            null);

            // when & then
            assertThatThrownBy(() -> config.validateSocialLoginProvider("kakao"))
                    .isInstanceOf(SocialLoginNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("isSocialLoginAllowed() 메서드 테스트")
    class IsSocialLoginAllowedTest {

        @Test
        @DisplayName("빈 Set이면 모든 제공자 허용")
        void shouldAllowAllWhenEmptySet() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), false);

            // when & then
            assertThat(config.isSocialLoginAllowed(SocialProvider.KAKAO)).isTrue();
            assertThat(config.isSocialLoginAllowed(SocialProvider.NAVER)).isTrue();
            assertThat(config.isSocialLoginAllowed(SocialProvider.GOOGLE)).isTrue();
        }

        @Test
        @DisplayName("Set에 포함된 제공자만 허용")
        void shouldAllowOnlyProvidersInSet() {
            // given
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"),
                            false,
                            Set.of(SocialProvider.KAKAO, SocialProvider.NAVER),
                            Map.of(),
                            null,
                            null);

            // when & then
            assertThat(config.isSocialLoginAllowed(SocialProvider.KAKAO)).isTrue();
            assertThat(config.isSocialLoginAllowed(SocialProvider.NAVER)).isTrue();
            assertThat(config.isSocialLoginAllowed(SocialProvider.GOOGLE)).isFalse();
        }
    }

    @Nested
    @DisplayName("getPermissionsForRole() 메서드 테스트")
    class GetPermissionsForRoleTest {

        @Test
        @DisplayName("존재하는 역할의 권한 반환")
        void shouldReturnPermissionsForExistingRole() {
            // given
            Map<String, Set<String>> roleHierarchy =
                    Map.of(
                            "ADMIN", Set.of("READ", "WRITE", "DELETE"),
                            "USER", Set.of("READ"));
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"), false, Set.of(), roleHierarchy, null, null);

            // when
            Set<String> adminPermissions = config.getPermissionsForRole("ADMIN");
            Set<String> userPermissions = config.getPermissionsForRole("USER");

            // then
            assertThat(adminPermissions).containsExactlyInAnyOrder("READ", "WRITE", "DELETE");
            assertThat(userPermissions).containsExactly("READ");
        }

        @Test
        @DisplayName("존재하지 않는 역할이면 빈 Set 반환")
        void shouldReturnEmptySetForNonExistentRole() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), false);

            // when
            Set<String> permissions = config.getPermissionsForRole("UNKNOWN_ROLE");

            // then
            assertThat(permissions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Getter 메서드 테스트")
    class GetterTest {

        @Test
        @DisplayName("getTenantIdValue()가 문자열 반환")
        void shouldReturnTenantIdValue() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-123"), false);

            // when & then
            assertThat(config.getTenantIdValue()).isEqualTo("tenant-123");
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 tenantId면 equals true")
        void shouldBeEqualWhenSameTenantId() {
            // given
            TenantConfig config1 = TenantConfig.of(TenantId.of("tenant-1"), true);
            TenantConfig config2 =
                    TenantConfig.of(TenantId.of("tenant-1"), false); // mfaRequired 다름

            // when & then - tenantId만으로 동등성 판단
            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("다른 tenantId면 equals false")
        void shouldNotBeEqualWhenDifferentTenantId() {
            // given
            TenantConfig config1 = TenantConfig.of(TenantId.of("tenant-1"), false);
            TenantConfig config2 = TenantConfig.of(TenantId.of("tenant-2"), false);

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), false);

            // when & then
            assertThat(config).isEqualTo(config);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), false);

            // when & then
            assertThat(config).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 TenantConfig 정보 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            TenantConfig config = TenantConfig.of(TenantId.of("tenant-1"), true);

            // when
            String result = config.toString();

            // then
            assertThat(result).contains("TenantConfig");
            assertThat(result).contains("tenantId");
            assertThat(result).contains("mfaRequired=true");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(TenantConfig.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("allowedSocialLogins는 불변 Set")
        void shouldHaveImmutableAllowedSocialLogins() {
            // given
            Set<SocialProvider> mutableSet = new java.util.HashSet<>();
            mutableSet.add(SocialProvider.KAKAO);
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"), false, mutableSet, Map.of(), null, null);

            // when - 원본 Set 수정
            mutableSet.add(SocialProvider.NAVER);

            // then - TenantConfig의 Set은 변경되지 않음
            assertThat(config.getAllowedSocialLogins()).containsExactly(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("roleHierarchy는 불변 Map")
        void shouldHaveImmutableRoleHierarchy() {
            // given
            Map<String, Set<String>> mutableMap = new java.util.HashMap<>();
            mutableMap.put("ADMIN", Set.of("READ"));
            TenantConfig config =
                    TenantConfig.of(
                            TenantId.of("tenant-1"), false, Set.of(), mutableMap, null, null);

            // when - 원본 Map 수정
            mutableMap.put("USER", Set.of("WRITE"));

            // then - TenantConfig의 Map은 변경되지 않음
            assertThat(config.getRoleHierarchy()).containsOnlyKeys("ADMIN");
        }
    }
}
