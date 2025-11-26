package com.ryuqq.gateway.domain.authorization.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HttpMethod 테스트")
class HttpMethodTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 HTTP 메서드가 정의되어 있음")
        void shouldHaveAllHttpMethods() {
            // when
            HttpMethod[] methods = HttpMethod.values();

            // then
            assertThat(methods).hasSize(5);
            assertThat(methods)
                    .containsExactly(
                            HttpMethod.GET,
                            HttpMethod.POST,
                            HttpMethod.PUT,
                            HttpMethod.PATCH,
                            HttpMethod.DELETE);
        }

        @Test
        @DisplayName("각 HTTP 메서드의 이름이 올바름")
        void shouldHaveCorrectMethodNames() {
            assertThat(HttpMethod.GET.name()).isEqualTo("GET");
            assertThat(HttpMethod.POST.name()).isEqualTo("POST");
            assertThat(HttpMethod.PUT.name()).isEqualTo("PUT");
            assertThat(HttpMethod.PATCH.name()).isEqualTo("PATCH");
            assertThat(HttpMethod.DELETE.name()).isEqualTo("DELETE");
        }
    }

    @Nested
    @DisplayName("from() 메서드 테스트")
    class FromMethodTest {

        @ParameterizedTest
        @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
        @DisplayName("유효한 HTTP 메서드 문자열을 Enum으로 변환")
        void shouldConvertValidHttpMethodString(String methodString) {
            // when
            HttpMethod method = HttpMethod.from(methodString);

            // then
            assertThat(method.name()).isEqualTo(methodString);
        }

        @ParameterizedTest
        @ValueSource(strings = {"get", "post", "put", "patch", "delete"})
        @DisplayName("소문자 HTTP 메서드 문자열을 Enum으로 변환")
        void shouldConvertLowercaseHttpMethodString(String methodString) {
            // when
            HttpMethod method = HttpMethod.from(methodString);

            // then
            assertThat(method.name()).isEqualTo(methodString.toUpperCase());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Get", "Post", "Put", "Patch", "Delete"})
        @DisplayName("대소문자 혼합 HTTP 메서드 문자열을 Enum으로 변환")
        void shouldConvertMixedCaseHttpMethodString(String methodString) {
            // when
            HttpMethod method = HttpMethod.from(methodString);

            // then
            assertThat(method.name()).isEqualTo(methodString.toUpperCase());
        }

        @Test
        @DisplayName("null 입력 시 예외 발생")
        void shouldThrowExceptionForNullInput() {
            assertThatThrownBy(() -> HttpMethod.from(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("HTTP method cannot be null or blank");
        }

        @Test
        @DisplayName("빈 문자열 입력 시 예외 발생")
        void shouldThrowExceptionForEmptyString() {
            assertThatThrownBy(() -> HttpMethod.from(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("HTTP method cannot be null or blank");
        }

        @Test
        @DisplayName("공백만 있는 문자열 입력 시 예외 발생")
        void shouldThrowExceptionForBlankString() {
            assertThatThrownBy(() -> HttpMethod.from("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("HTTP method cannot be null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "HEAD", "OPTIONS", "TRACE", "CONNECT"})
        @DisplayName("유효하지 않은 HTTP 메서드 문자열 시 예외 발생")
        void shouldThrowExceptionForInvalidHttpMethod(String invalidMethod) {
            assertThatThrownBy(() -> HttpMethod.from(invalidMethod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid HTTP method: " + invalidMethod);
        }

        @Test
        @DisplayName("숫자가 포함된 문자열 시 예외 발생")
        void shouldThrowExceptionForMethodWithNumbers() {
            String invalidMethod = "GET123";
            assertThatThrownBy(() -> HttpMethod.from(invalidMethod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid HTTP method: " + invalidMethod);
        }

        @Test
        @DisplayName("특수문자가 포함된 문자열 시 예외 발생")
        void shouldThrowExceptionForMethodWithSpecialCharacters() {
            String invalidMethod = "GET-POST";
            assertThatThrownBy(() -> HttpMethod.from(invalidMethod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid HTTP method: " + invalidMethod);
        }

        @Test
        @DisplayName("공백이 포함된 문자열 시 예외 발생")
        void shouldThrowExceptionForMethodWithSpaces() {
            String invalidMethod = "G ET";
            assertThatThrownBy(() -> HttpMethod.from(invalidMethod))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid HTTP method: " + invalidMethod);
        }
    }

    @Nested
    @DisplayName("Enum 동작 테스트")
    class EnumBehaviorTest {

        @Test
        @DisplayName("valueOf()로 Enum 값 조회")
        void shouldGetEnumValueUsingValueOf() {
            assertThat(HttpMethod.valueOf("GET")).isEqualTo(HttpMethod.GET);
            assertThat(HttpMethod.valueOf("POST")).isEqualTo(HttpMethod.POST);
            assertThat(HttpMethod.valueOf("PUT")).isEqualTo(HttpMethod.PUT);
            assertThat(HttpMethod.valueOf("PATCH")).isEqualTo(HttpMethod.PATCH);
            assertThat(HttpMethod.valueOf("DELETE")).isEqualTo(HttpMethod.DELETE);
        }

        @Test
        @DisplayName("ordinal() 값이 올바름")
        void shouldHaveCorrectOrdinalValues() {
            assertThat(HttpMethod.GET.ordinal()).isEqualTo(0);
            assertThat(HttpMethod.POST.ordinal()).isEqualTo(1);
            assertThat(HttpMethod.PUT.ordinal()).isEqualTo(2);
            assertThat(HttpMethod.PATCH.ordinal()).isEqualTo(3);
            assertThat(HttpMethod.DELETE.ordinal()).isEqualTo(4);
        }

        @Test
        @DisplayName("toString()이 name()과 동일")
        void shouldHaveToStringEqualToName() {
            for (HttpMethod method : HttpMethod.values()) {
                assertThat(method.toString()).isEqualTo(method.name());
            }
        }

        @Test
        @DisplayName("Enum 비교가 올바르게 동작")
        void shouldCompareEnumsCorrectly() {
            assertThat(HttpMethod.GET).isEqualTo(HttpMethod.GET);
            assertThat(HttpMethod.GET).isNotEqualTo(HttpMethod.POST);
            assertThat(HttpMethod.GET == HttpMethod.GET).isTrue();
            assertThat(HttpMethod.GET == HttpMethod.POST).isFalse();
        }
    }
}
