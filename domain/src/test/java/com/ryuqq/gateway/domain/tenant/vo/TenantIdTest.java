package com.ryuqq.gateway.domain.tenant.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantId VO 테스트")
class TenantIdTest {

    @Nested
    @DisplayName("of() 메서드 테스트 - 형식 검증 포함")
    class OfMethodTest {

        @Test
        @DisplayName("tenant-{숫자} 형식으로 TenantId 생성 성공")
        void shouldCreateWithTenantNumberFormat() {
            // given
            String value = "tenant-123";

            // when
            TenantId tenantId = TenantId.of(value);

            // then
            assertThat(tenantId).isNotNull();
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("UUID 형식으로 TenantId 생성 성공")
        void shouldCreateWithUuidFormat() {
            // given
            String value = "550e8400-e29b-41d4-a716-446655440000";

            // when
            TenantId tenantId = TenantId.of(value);

            // then
            assertThat(tenantId).isNotNull();
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("대문자 UUID 형식도 생성 성공 (case insensitive)")
        void shouldCreateWithUppercaseUuid() {
            // given
            String value = "550E8400-E29B-41D4-A716-446655440000";

            // when
            TenantId tenantId = TenantId.of(value);

            // then
            assertThat(tenantId).isNotNull();
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("tenant-1 최소 숫자 형식 성공")
        void shouldCreateWithMinimalTenantNumber() {
            // given
            String value = "tenant-1";

            // when
            TenantId tenantId = TenantId.of(value);

            // then
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("tenant-999999 큰 숫자 형식 성공")
        void shouldCreateWithLargeTenantNumber() {
            // given
            String value = "tenant-999999";

            // when
            TenantId tenantId = TenantId.of(value);

            // then
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("null 값이면 예외 발생")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> TenantId.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> TenantId.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있으면 예외 발생")
        void shouldThrowExceptionWhenBlank() {
            assertThatThrownBy(() -> TenantId.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("잘못된 형식이면 예외 발생 - 일반 문자열")
        void shouldThrowExceptionWhenInvalidFormat() {
            assertThatThrownBy(() -> TenantId.of("invalid-tenant"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid TenantId format");
        }

        @Test
        @DisplayName("잘못된 형식이면 예외 발생 - tenant 접두사 없음")
        void shouldThrowExceptionWhenMissingTenantPrefix() {
            assertThatThrownBy(() -> TenantId.of("123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid TenantId format");
        }

        @Test
        @DisplayName("잘못된 형식이면 예외 발생 - tenant-문자")
        void shouldThrowExceptionWhenTenantWithLetters() {
            assertThatThrownBy(() -> TenantId.of("tenant-abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid TenantId format");
        }

        @Test
        @DisplayName("잘못된 UUID 형식이면 예외 발생")
        void shouldThrowExceptionWhenInvalidUuidFormat() {
            assertThatThrownBy(() -> TenantId.of("550e8400-e29b-41d4-a716"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid TenantId format");
        }
    }

    @Nested
    @DisplayName("from() 메서드 테스트 - 형식 검증 없이")
    class FromMethodTest {

        @Test
        @DisplayName("tenant-{숫자} 형식으로 생성 성공")
        void shouldCreateFromTenantNumberFormat() {
            // given
            String value = "tenant-456";

            // when
            TenantId tenantId = TenantId.from(value);

            // then
            assertThat(tenantId).isNotNull();
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("형식 검증 없이 임의의 값으로 생성 가능")
        void shouldCreateFromAnyValue() {
            // given - 형식 검증 없이 생성
            String value = "custom-tenant-value";

            // when
            TenantId tenantId = TenantId.from(value);

            // then
            assertThat(tenantId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("null 값이면 예외 발생")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> TenantId.from(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> TenantId.from(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있으면 예외 발생")
        void shouldThrowExceptionWhenBlank() {
            assertThatThrownBy(() -> TenantId.from("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }
    }

    @Nested
    @DisplayName("isUuidFormat() 메서드 테스트")
    class IsUuidFormatTest {

        @Test
        @DisplayName("UUID 형식이면 true 반환")
        void shouldReturnTrueForUuidFormat() {
            // given
            TenantId tenantId = TenantId.of("550e8400-e29b-41d4-a716-446655440000");

            // when & then
            assertThat(tenantId.isUuidFormat()).isTrue();
        }

        @Test
        @DisplayName("대문자 UUID 형식이면 true 반환")
        void shouldReturnTrueForUppercaseUuidFormat() {
            // given
            TenantId tenantId = TenantId.of("550E8400-E29B-41D4-A716-446655440000");

            // when & then
            assertThat(tenantId.isUuidFormat()).isTrue();
        }

        @Test
        @DisplayName("tenant-{숫자} 형식이면 false 반환")
        void shouldReturnFalseForTenantNumberFormat() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId.isUuidFormat()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTenantNumberFormat() 메서드 테스트")
    class IsTenantNumberFormatTest {

        @Test
        @DisplayName("tenant-{숫자} 형식이면 true 반환")
        void shouldReturnTrueForTenantNumberFormat() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId.isTenantNumberFormat()).isTrue();
        }

        @Test
        @DisplayName("UUID 형식이면 false 반환")
        void shouldReturnFalseForUuidFormat() {
            // given
            TenantId tenantId = TenantId.of("550e8400-e29b-41d4-a716-446655440000");

            // when & then
            assertThat(tenantId.isTenantNumberFormat()).isFalse();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValue() {
            // given
            TenantId tenantId1 = TenantId.of("tenant-123");
            TenantId tenantId2 = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId1).isEqualTo(tenantId2);
            assertThat(tenantId1.hashCode()).isEqualTo(tenantId2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValue() {
            // given
            TenantId tenantId1 = TenantId.of("tenant-123");
            TenantId tenantId2 = TenantId.of("tenant-456");

            // when & then
            assertThat(tenantId1).isNotEqualTo(tenantId2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when & then
            assertThat(tenantId).isNotEqualTo("tenant-123");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 TenantId 정보 포함")
        void shouldIncludeTenantIdInToString() {
            // given
            TenantId tenantId = TenantId.of("tenant-123");

            // when
            String result = tenantId.toString();

            // then
            assertThat(result).contains("TenantId");
            assertThat(result).contains("tenant-123");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("record 클래스는 final임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(TenantId.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = TenantId.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.isSynthetic()
                        && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                            .as("Field '%s' should be final", field.getName())
                            .isTrue();
                }
            }
        }
    }
}
