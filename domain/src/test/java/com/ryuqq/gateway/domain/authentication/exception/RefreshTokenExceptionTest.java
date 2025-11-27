package com.ryuqq.gateway.domain.authentication.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Refresh Token 예외 테스트")
class RefreshTokenExceptionTest {

    @Nested
    @DisplayName("RefreshTokenInvalidException 테스트")
    class RefreshTokenInvalidExceptionTest {

        @Test
        @DisplayName("예외 생성 및 메시지 확인")
        void shouldCreateExceptionWithMessage() {
            // given
            String message = "Refresh token is invalid";

            // when
            RefreshTokenInvalidException exception = new RefreshTokenInvalidException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.code()).isEqualTo("AUTH-004");
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.REFRESH_TOKEN_INVALID);
        }

        @Test
        @DisplayName("에러 코드가 올바름")
        void shouldHaveCorrectErrorCode() {
            // given
            RefreshTokenInvalidException exception = new RefreshTokenInvalidException("test");

            // when & then
            assertThat(exception.getErrorCode().getCode()).isEqualTo("AUTH-004");
            assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(401);
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("Refresh token is invalid");
        }
    }

    @Nested
    @DisplayName("RefreshTokenExpiredException 테스트")
    class RefreshTokenExpiredExceptionTest {

        @Test
        @DisplayName("예외 생성 및 메시지 확인")
        void shouldCreateExceptionWithMessage() {
            // given
            String message = "Refresh token has expired";

            // when
            RefreshTokenExpiredException exception = new RefreshTokenExpiredException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.code()).isEqualTo("AUTH-005");
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("에러 코드가 올바름")
        void shouldHaveCorrectErrorCode() {
            // given
            RefreshTokenExpiredException exception = new RefreshTokenExpiredException("test");

            // when & then
            assertThat(exception.getErrorCode().getCode()).isEqualTo("AUTH-005");
            assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(401);
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("Refresh token has expired");
        }
    }

    @Nested
    @DisplayName("RefreshTokenReusedException 테스트")
    class RefreshTokenReusedExceptionTest {

        @Test
        @DisplayName("예외 생성 및 메시지 확인")
        void shouldCreateExceptionWithMessage() {
            // given
            String message = "Token reuse detected";

            // when
            RefreshTokenReusedException exception = new RefreshTokenReusedException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.code()).isEqualTo("AUTH-006");
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.REFRESH_TOKEN_REUSED);
        }

        @Test
        @DisplayName("에러 코드가 올바름")
        void shouldHaveCorrectErrorCode() {
            // given
            RefreshTokenReusedException exception = new RefreshTokenReusedException("test");

            // when & then
            assertThat(exception.getErrorCode().getCode()).isEqualTo("AUTH-006");
            assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(401);
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("Refresh token reuse detected");
        }
    }

    @Nested
    @DisplayName("RefreshTokenMissingException 테스트")
    class RefreshTokenMissingExceptionTest {

        @Test
        @DisplayName("예외 생성 및 메시지 확인")
        void shouldCreateExceptionWithMessage() {
            // given
            String message = "Refresh token is missing from cookie";

            // when
            RefreshTokenMissingException exception = new RefreshTokenMissingException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.code()).isEqualTo("AUTH-007");
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.REFRESH_TOKEN_MISSING);
        }

        @Test
        @DisplayName("에러 코드가 올바름")
        void shouldHaveCorrectErrorCode() {
            // given
            RefreshTokenMissingException exception = new RefreshTokenMissingException("test");

            // when & then
            assertThat(exception.getErrorCode().getCode()).isEqualTo("AUTH-007");
            assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(401);
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("Refresh token is missing");
        }
    }

    @Nested
    @DisplayName("TokenRefreshFailedException 테스트")
    class TokenRefreshFailedExceptionTest {

        @Test
        @DisplayName("예외 생성 및 메시지 확인")
        void shouldCreateExceptionWithMessage() {
            // given
            String message = "Lock acquisition failed";

            // when
            TokenRefreshFailedException exception = new TokenRefreshFailedException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.code()).isEqualTo("AUTH-008");
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.TOKEN_REFRESH_FAILED);
        }

        @Test
        @DisplayName("원인과 함께 예외 생성")
        void shouldCreateExceptionWithCause() {
            // given
            String message = "AuthHub call failed";
            RuntimeException cause = new RuntimeException("Connection timeout");

            // when
            TokenRefreshFailedException exception = new TokenRefreshFailedException(message, cause);

            // then
            assertThat(exception.getMessage()).contains(message);
            assertThat(exception.getMessage()).contains("Connection timeout");
        }

        @Test
        @DisplayName("에러 코드가 올바름")
        void shouldHaveCorrectErrorCode() {
            // given
            TokenRefreshFailedException exception = new TokenRefreshFailedException("test");

            // when & then
            assertThat(exception.getErrorCode().getCode()).isEqualTo("AUTH-008");
            assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(500);
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("Token refresh failed");
        }
    }
}
