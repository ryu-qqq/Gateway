package com.ryuqq.gateway.domain.authentication.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthenticationErrorCode 테스트")
class AuthenticationErrorCodeTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 에러 코드가 정의되어 있음")
        void shouldHaveAllErrorCodes() {
            // when
            AuthenticationErrorCode[] errorCodes = AuthenticationErrorCode.values();

            // then
            assertThat(errorCodes).hasSize(8);
            assertThat(errorCodes)
                    .containsExactly(
                            AuthenticationErrorCode.JWT_EXPIRED,
                            AuthenticationErrorCode.JWT_INVALID,
                            AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND,
                            AuthenticationErrorCode.REFRESH_TOKEN_INVALID,
                            AuthenticationErrorCode.REFRESH_TOKEN_EXPIRED,
                            AuthenticationErrorCode.REFRESH_TOKEN_REUSED,
                            AuthenticationErrorCode.REFRESH_TOKEN_MISSING,
                            AuthenticationErrorCode.TOKEN_REFRESH_FAILED);
        }

        @Test
        @DisplayName("각 에러 코드의 이름이 올바름")
        void shouldHaveCorrectErrorCodeNames() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.name()).isEqualTo("JWT_EXPIRED");
            assertThat(AuthenticationErrorCode.JWT_INVALID.name()).isEqualTo("JWT_INVALID");
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.name())
                    .isEqualTo("PUBLIC_KEY_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("JWT_EXPIRED 테스트")
    class JwtExpiredTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getCode()).isEqualTo("AUTH-001");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (401 Unauthorized)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getMessage())
                    .isEqualTo("JWT token has expired");
        }
    }

    @Nested
    @DisplayName("JWT_INVALID 테스트")
    class JwtInvalidTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(AuthenticationErrorCode.JWT_INVALID.getCode()).isEqualTo("AUTH-002");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (401 Unauthorized)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(AuthenticationErrorCode.JWT_INVALID.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(AuthenticationErrorCode.JWT_INVALID.getMessage())
                    .isEqualTo("JWT token is invalid");
        }
    }

    @Nested
    @DisplayName("PUBLIC_KEY_NOT_FOUND 테스트")
    class PublicKeyNotFoundTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode())
                    .isEqualTo("AUTH-003");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (404 Not Found)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getHttpStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getMessage())
                    .isEqualTo("Public key not found");
        }
    }

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 테스트")
    class ErrorCodeInterfaceTest {

        @Test
        @DisplayName("모든 에러 코드가 ErrorCode 인터페이스를 구현함")
        void shouldImplementErrorCodeInterface() {
            for (AuthenticationErrorCode errorCode : AuthenticationErrorCode.values()) {
                assertThat(errorCode.getCode()).isNotNull();
                assertThat(errorCode.getHttpStatus()).isPositive();
                assertThat(errorCode.getMessage()).isNotNull();
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 AUTH- 접두사를 가짐")
        void shouldHaveAuthPrefix() {
            for (AuthenticationErrorCode errorCode : AuthenticationErrorCode.values()) {
                assertThat(errorCode.getCode()).startsWith("AUTH-");
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 3자리 숫자 형식을 가짐")
        void shouldHaveThreeDigitFormat() {
            for (AuthenticationErrorCode errorCode : AuthenticationErrorCode.values()) {
                String code = errorCode.getCode();
                String numberPart = code.substring("AUTH-".length());
                assertThat(numberPart).matches("\\d{3}");
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 비어있지 않은 메시지를 가짐")
        void shouldHaveNonEmptyMessage() {
            for (AuthenticationErrorCode errorCode : AuthenticationErrorCode.values()) {
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
            assertThat(AuthenticationErrorCode.valueOf("JWT_EXPIRED"))
                    .isEqualTo(AuthenticationErrorCode.JWT_EXPIRED);
            assertThat(AuthenticationErrorCode.valueOf("JWT_INVALID"))
                    .isEqualTo(AuthenticationErrorCode.JWT_INVALID);
            assertThat(AuthenticationErrorCode.valueOf("PUBLIC_KEY_NOT_FOUND"))
                    .isEqualTo(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND);
        }

        @Test
        @DisplayName("ordinal() 값이 올바름")
        void shouldHaveCorrectOrdinalValues() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.ordinal()).isEqualTo(0);
            assertThat(AuthenticationErrorCode.JWT_INVALID.ordinal()).isEqualTo(1);
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.ordinal()).isEqualTo(2);
        }

        @Test
        @DisplayName("toString()이 name()과 동일")
        void shouldHaveToStringEqualToName() {
            for (AuthenticationErrorCode errorCode : AuthenticationErrorCode.values()) {
                assertThat(errorCode.toString()).isEqualTo(errorCode.name());
            }
        }

        @Test
        @DisplayName("Enum 비교가 올바르게 동작")
        void shouldCompareEnumsCorrectly() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED)
                    .isEqualTo(AuthenticationErrorCode.JWT_EXPIRED);
            assertThat(AuthenticationErrorCode.JWT_EXPIRED)
                    .isNotEqualTo(AuthenticationErrorCode.JWT_INVALID);
            assertThat(AuthenticationErrorCode.JWT_EXPIRED == AuthenticationErrorCode.JWT_EXPIRED)
                    .isTrue();
            assertThat(AuthenticationErrorCode.JWT_EXPIRED == AuthenticationErrorCode.JWT_INVALID)
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("코드 규칙 검증 테스트")
    class CodeRuleValidationTest {

        @Test
        @DisplayName("에러 코드가 중복되지 않음")
        void shouldHaveUniqueErrorCodes() {
            AuthenticationErrorCode[] errorCodes = AuthenticationErrorCode.values();

            for (int i = 0; i < errorCodes.length; i++) {
                for (int j = i + 1; j < errorCodes.length; j++) {
                    assertThat(errorCodes[i].getCode()).isNotEqualTo(errorCodes[j].getCode());
                }
            }
        }

        @Test
        @DisplayName("에러 코드 번호가 순차적임")
        void shouldHaveSequentialErrorCodeNumbers() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getCode()).isEqualTo("AUTH-001");
            assertThat(AuthenticationErrorCode.JWT_INVALID.getCode()).isEqualTo("AUTH-002");
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode())
                    .isEqualTo("AUTH-003");
        }

        @Test
        @DisplayName("메시지가 의미있는 내용을 포함함")
        void shouldHaveMeaningfulMessages() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getMessage())
                    .containsIgnoringCase("jwt")
                    .containsIgnoringCase("expired");

            assertThat(AuthenticationErrorCode.JWT_INVALID.getMessage())
                    .containsIgnoringCase("jwt")
                    .containsIgnoringCase("invalid");

            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getMessage())
                    .containsIgnoringCase("public key")
                    .containsIgnoringCase("not found");
        }
    }

    @Nested
    @DisplayName("HTTP 상태 코드 매핑 테스트")
    class HttpStatusMappingTest {

        @Test
        @DisplayName("인증 관련 에러는 401 Unauthorized")
        void authenticationErrorsShouldReturn401() {
            assertThat(AuthenticationErrorCode.JWT_EXPIRED.getHttpStatus()).isEqualTo(401);
            assertThat(AuthenticationErrorCode.JWT_INVALID.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("리소스 찾기 실패 에러는 404 Not Found")
        void resourceNotFoundErrorsShouldReturn404() {
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getHttpStatus()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("비즈니스 의미 테스트")
    class BusinessMeaningTest {

        @Test
        @DisplayName("JWT_EXPIRED는 토큰 만료를 의미함")
        void jwtExpiredShouldMeanTokenExpiration() {
            AuthenticationErrorCode errorCode = AuthenticationErrorCode.JWT_EXPIRED;

            assertThat(errorCode.getCode()).contains("001");
            assertThat(errorCode.getMessage()).containsIgnoringCase("expired");
            assertThat(errorCode.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("JWT_INVALID는 토큰 유효성 검증 실패를 의미함")
        void jwtInvalidShouldMeanTokenValidationFailure() {
            AuthenticationErrorCode errorCode = AuthenticationErrorCode.JWT_INVALID;

            assertThat(errorCode.getCode()).contains("002");
            assertThat(errorCode.getMessage()).containsIgnoringCase("invalid");
            assertThat(errorCode.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("PUBLIC_KEY_NOT_FOUND는 키 조회 실패를 의미함")
        void publicKeyNotFoundShouldMeanKeyLookupFailure() {
            AuthenticationErrorCode errorCode = AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND;

            assertThat(errorCode.getCode()).contains("003");
            assertThat(errorCode.getMessage()).containsIgnoringCase("not found");
            assertThat(errorCode.getHttpStatus()).isEqualTo(404);
        }
    }
}
