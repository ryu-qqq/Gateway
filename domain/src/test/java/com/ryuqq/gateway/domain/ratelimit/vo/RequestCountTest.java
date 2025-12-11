package com.ryuqq.gateway.domain.ratelimit.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RequestCount VO 테스트")
class RequestCountTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 카운트로 생성")
        void shouldCreateValidCount() {
            // when
            RequestCount count = RequestCount.of(50);

            // then
            assertThat(count).isNotNull();
            assertThat(count.value()).isEqualTo(50);
        }

        @Test
        @DisplayName("0으로 생성")
        void shouldCreateWithZero() {
            // when
            RequestCount count = RequestCount.of(0);

            // then
            assertThat(count.value()).isEqualTo(0);
        }

        @Test
        @DisplayName("음수이면 예외 발생")
        void shouldThrowExceptionWhenNegative() {
            assertThatThrownBy(() -> RequestCount.of(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count cannot be negative");
        }
    }

    @Nested
    @DisplayName("zero 팩토리 메서드 테스트")
    class ZeroTest {

        @Test
        @DisplayName("zero()로 0 카운트 생성")
        void shouldCreateZeroCount() {
            // when
            RequestCount count = RequestCount.zero();

            // then
            assertThat(count.value()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("increment 메서드 테스트")
    class IncrementTest {

        @Test
        @DisplayName("1 증가")
        void shouldIncrementByOne() {
            // given
            RequestCount count = RequestCount.of(50);

            // when
            RequestCount incremented = count.increment();

            // then
            assertThat(incremented.value()).isEqualTo(51);
            // 원본은 불변
            assertThat(count.value()).isEqualTo(50);
        }

        @Test
        @DisplayName("0에서 1로 증가")
        void shouldIncrementFromZero() {
            // given
            RequestCount count = RequestCount.zero();

            // when
            RequestCount incremented = count.increment();

            // then
            assertThat(incremented.value()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isExceeded 메서드 테스트")
    class IsExceededTest {

        @Test
        @DisplayName("limit 미만이면 false")
        void shouldReturnFalseWhenBelowLimit() {
            // given
            RequestCount count = RequestCount.of(99);

            // when & then
            assertThat(count.isExceeded(100)).isFalse();
        }

        @Test
        @DisplayName("limit 이상이면 true (경계값 포함)")
        void shouldReturnTrueWhenReachesOrExceedsLimit() {
            // given
            RequestCount countAtLimit = RequestCount.of(100);
            RequestCount countOverLimit = RequestCount.of(101);

            // when & then - limit=100일 때 100번째 요청부터 차단
            assertThat(countAtLimit.isExceeded(100)).isTrue();
            assertThat(countOverLimit.isExceeded(100)).isTrue();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValue() {
            // given
            RequestCount count1 = RequestCount.of(50);
            RequestCount count2 = RequestCount.of(50);

            // when & then
            assertThat(count1).isEqualTo(count2);
            assertThat(count1.hashCode()).isEqualTo(count2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValue() {
            // given
            RequestCount count1 = RequestCount.of(50);
            RequestCount count2 = RequestCount.of(51);

            // when & then
            assertThat(count1).isNotEqualTo(count2);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            RequestCount count = RequestCount.of(50);

            // when & then
            assertThat(count).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            RequestCount count = RequestCount.of(50);

            // when & then
            assertThat(count).isNotEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 값 정보 포함")
        void shouldIncludeValueInToString() {
            // given
            RequestCount count = RequestCount.of(50);

            // when
            String result = count.toString();

            // then
            assertThat(result).contains("RequestCount");
            assertThat(result).contains("50");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(RequestCount.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = RequestCount.class.getDeclaredFields();
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
