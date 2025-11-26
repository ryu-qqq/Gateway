package com.ryuqq.gateway.domain.authorization.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthorizationErrorCode 테스트")
class AuthorizationErrorCodeTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 에러 코드가 정의되어 있음")
        void shouldHaveAllErrorCodes() {
            // when
            AuthorizationErrorCode[] errorCodes = AuthorizationErrorCode.values();

            // then
            assertThat(errorCodes).hasSize(2);
            assertThat(errorCodes)
                    .containsExactly(
                            AuthorizationErrorCode.PERMISSION_DENIED,
                            AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND);
        }

        @Test
        @DisplayName("각 에러 코드의 이름이 올바름")
        void shouldHaveCorrectErrorCodeNames() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.name())
                    .isEqualTo("PERMISSION_DENIED");
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.name())
                    .isEqualTo("PERMISSION_SPEC_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("PERMISSION_DENIED 테스트")
    class PermissionDeniedTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.getCode()).isEqualTo("AUTHZ-001");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐")
        void shouldHaveCorrectHttpStatus() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.getMessage())
                    .isEqualTo("Permission denied");
        }
    }

    @Nested
    @DisplayName("PERMISSION_SPEC_NOT_FOUND 테스트")
    class PermissionSpecNotFoundTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getCode())
                    .isEqualTo("AUTHZ-002");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐")
        void shouldHaveCorrectHttpStatus() {
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getHttpStatus())
                    .isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getMessage())
                    .isEqualTo("Permission spec not found for endpoint");
        }
    }

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 테스트")
    class ErrorCodeInterfaceTest {

        @Test
        @DisplayName("모든 에러 코드가 ErrorCode 인터페이스를 구현함")
        void shouldImplementErrorCodeInterface() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                assertThat(errorCode.getCode()).isNotNull();
                assertThat(errorCode.getHttpStatus()).isPositive();
                assertThat(errorCode.getMessage()).isNotNull();
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 AUTHZ- 접두사를 가짐")
        void shouldHaveAuthzPrefix() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                assertThat(errorCode.getCode()).startsWith("AUTHZ-");
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 3자리 숫자 형식을 가짐")
        void shouldHaveThreeDigitFormat() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                String code = errorCode.getCode();
                String numberPart = code.substring("AUTHZ-".length());
                assertThat(numberPart).matches("\\d{3}");
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 403 Forbidden 상태 코드를 가짐")
        void shouldHaveForbiddenHttpStatus() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                assertThat(errorCode.getHttpStatus()).isEqualTo(403);
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 비어있지 않은 메시지를 가짐")
        void shouldHaveNonEmptyMessage() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                assertThat(errorCode.getMessage()).isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("Enum 동작 테스트")
    class EnumBehaviorTest {

        @Test
        @DisplayName("valueOf()로 Enum 값 조회")
        void shouldGetEnumValueUsingValueOf() {
            assertThat(AuthorizationErrorCode.valueOf("PERMISSION_DENIED"))
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED);
            assertThat(AuthorizationErrorCode.valueOf("PERMISSION_SPEC_NOT_FOUND"))
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND);
        }

        @Test
        @DisplayName("ordinal() 값이 올바름")
        void shouldHaveCorrectOrdinalValues() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.ordinal()).isEqualTo(0);
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.ordinal()).isEqualTo(1);
        }

        @Test
        @DisplayName("toString()이 name()과 동일")
        void shouldHaveToStringEqualToName() {
            for (AuthorizationErrorCode errorCode : AuthorizationErrorCode.values()) {
                assertThat(errorCode.toString()).isEqualTo(errorCode.name());
            }
        }

        @Test
        @DisplayName("Enum 비교가 올바르게 동작")
        void shouldCompareEnumsCorrectly() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED)
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED);
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED)
                    .isNotEqualTo(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND);
            assertThat(
                            AuthorizationErrorCode.PERMISSION_DENIED
                                    == AuthorizationErrorCode.PERMISSION_DENIED)
                    .isTrue();
            assertThat(
                            AuthorizationErrorCode.PERMISSION_DENIED
                                    == AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND)
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("코드 규칙 검증 테스트")
    class CodeRuleValidationTest {

        @Test
        @DisplayName("에러 코드가 중복되지 않음")
        void shouldHaveUniqueErrorCodes() {
            AuthorizationErrorCode[] errorCodes = AuthorizationErrorCode.values();

            for (int i = 0; i < errorCodes.length; i++) {
                for (int j = i + 1; j < errorCodes.length; j++) {
                    assertThat(errorCodes[i].getCode()).isNotEqualTo(errorCodes[j].getCode());
                }
            }
        }

        @Test
        @DisplayName("에러 코드 번호가 순차적임")
        void shouldHaveSequentialErrorCodeNumbers() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.getCode()).isEqualTo("AUTHZ-001");
            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getCode())
                    .isEqualTo("AUTHZ-002");
        }

        @Test
        @DisplayName("메시지가 의미있는 내용을 포함함")
        void shouldHaveMeaningfulMessages() {
            assertThat(AuthorizationErrorCode.PERMISSION_DENIED.getMessage())
                    .containsIgnoringCase("permission")
                    .containsIgnoringCase("denied");

            assertThat(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getMessage())
                    .containsIgnoringCase("permission")
                    .containsIgnoringCase("spec")
                    .containsIgnoringCase("not found");
        }
    }

    @Nested
    @DisplayName("비즈니스 의미 테스트")
    class BusinessMeaningTest {

        @Test
        @DisplayName("PERMISSION_DENIED는 권한 부족을 의미함")
        void permissionDeniedShouldMeanInsufficientPermission() {
            AuthorizationErrorCode errorCode = AuthorizationErrorCode.PERMISSION_DENIED;

            assertThat(errorCode.getCode()).contains("001"); // 첫 번째 에러
            assertThat(errorCode.getMessage()).containsIgnoringCase("denied");
            assertThat(errorCode.getHttpStatus()).isEqualTo(403); // Forbidden
        }

        @Test
        @DisplayName("PERMISSION_SPEC_NOT_FOUND는 엔드포인트 미정의를 의미함")
        void permissionSpecNotFoundShouldMeanEndpointNotDefined() {
            AuthorizationErrorCode errorCode = AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND;

            assertThat(errorCode.getCode()).contains("002"); // 두 번째 에러
            assertThat(errorCode.getMessage()).containsIgnoringCase("not found");
            assertThat(errorCode.getMessage()).containsIgnoringCase("endpoint");
            assertThat(errorCode.getHttpStatus()).isEqualTo(403); // Default Deny로 Forbidden
        }
    }
}
