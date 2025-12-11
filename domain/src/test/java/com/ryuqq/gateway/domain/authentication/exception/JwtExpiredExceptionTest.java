package com.ryuqq.gateway.domain.authentication.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtExpiredException 테스트")
class JwtExpiredExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("Access Token으로 예외 생성")
        void shouldCreateExceptionWithAccessToken() {
            // given
            String accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";

            // when
            JwtExpiredException exception = new JwtExpiredException(accessToken);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
            assertThat(exception.getMessage())
                    .isEqualTo("JWT token has expired: accessToken: " + accessToken);
        }

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateExceptionWithDefaultMessage() {
            // when
            JwtExpiredException exception = new JwtExpiredException();

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
            assertThat(exception.getMessage()).isEqualTo("JWT token has expired");
        }

        @Test
        @DisplayName("빈 문자열 Access Token으로 예외 생성")
        void shouldCreateExceptionWithEmptyAccessToken() {
            // given
            String accessToken = "";

            // when
            JwtExpiredException exception = new JwtExpiredException(accessToken);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
            assertThat(exception.getMessage()).isEqualTo("JWT token has expired: accessToken: ");
        }

        @Test
        @DisplayName("null Access Token으로 예외 생성")
        void shouldCreateExceptionWithNullAccessToken() {
            // given
            String accessToken = null;

            // when
            JwtExpiredException exception = new JwtExpiredException(accessToken);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
            assertThat(exception.getMessage())
                    .isEqualTo("JWT token has expired: accessToken: null");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("AuthenticationErrorCode.JWT_EXPIRED와 매핑됨")
        void shouldMapToJwtExpiredErrorCode() {
            // given
            JwtExpiredException exception = new JwtExpiredException("test-token");

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthenticationErrorCode.JWT_EXPIRED.getCode());
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
        }

        @Test
        @DisplayName("기본 생성자도 동일한 ErrorCode 사용")
        void shouldUseSameErrorCodeForDefaultConstructor() {
            // given
            JwtExpiredException exception = new JwtExpiredException();

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthenticationErrorCode.JWT_EXPIRED.getCode());
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            JwtExpiredException exception = new JwtExpiredException("test-token");

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            // when & then
            assertThat(java.lang.reflect.Modifier.isFinal(JwtExpiredException.class.getModifiers()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 형식 테스트")
    class MessageFormatTest {

        @Test
        @DisplayName("메시지에 Access Token이 포함됨")
        void shouldIncludeAccessTokenInMessage() {
            // given
            String accessToken = "my-jwt-token-value";

            // when
            JwtExpiredException exception = new JwtExpiredException(accessToken);

            // then
            assertThat(exception.getMessage()).contains(accessToken);
            assertThat(exception.getMessage()).startsWith("JWT token has expired:");
        }

        @Test
        @DisplayName("메시지 형식이 올바름")
        void shouldHaveCorrectMessageFormat() {
            // given
            String accessToken = "test-token";

            // when
            JwtExpiredException exception = new JwtExpiredException(accessToken);

            // then
            String expectedMessage = "JWT token has expired: accessToken: " + accessToken;
            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("기본 메시지가 ErrorCode 메시지와 동일")
        void shouldHaveDefaultMessageMatchingErrorCode() {
            // when
            JwtExpiredException exception = new JwtExpiredException();

            // then
            assertThat(exception.getMessage())
                    .isEqualTo(AuthenticationErrorCode.JWT_EXPIRED.getMessage());
        }
    }

    @Nested
    @DisplayName("예외 던지기 테스트")
    class ThrowExceptionTest {

        @Test
        @DisplayName("예외가 정상적으로 던져짐")
        void shouldThrowException() {
            // when & then
            try {
                throw new JwtExpiredException("expired-token");
            } catch (JwtExpiredException e) {
                assertThat(e.getCode()).isEqualTo("AUTH-001");
                assertThat(e.getMessage()).contains("expired-token");
            }
        }

        @Test
        @DisplayName("try-catch로 잡을 수 있음")
        void shouldBeCatchable() {
            // given
            boolean caught = false;

            // when
            try {
                throw new JwtExpiredException();
            } catch (JwtExpiredException e) {
                caught = true;
            }

            // then
            assertThat(caught).isTrue();
        }

        @Test
        @DisplayName("DomainException으로 잡을 수 있음")
        void shouldBeCatchableAsDomainException() {
            // given
            boolean caught = false;
            String caughtCode = null;

            // when
            try {
                throw new JwtExpiredException("test");
            } catch (DomainException e) {
                caught = true;
                caughtCode = e.getCode();
            }

            // then
            assertThat(caught).isTrue();
            assertThat(caughtCode).isEqualTo("AUTH-001");
        }
    }

    @Nested
    @DisplayName("비즈니스 시나리오 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("만료된 토큰 시나리오")
        void expiredTokenScenario() {
            // given
            String expiredToken =
                    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2V5In0"
                            + ".eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjAwMDAwMDAwfQ"
                            + ".signature";

            // when
            JwtExpiredException exception = new JwtExpiredException(expiredToken);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-001");
            assertThat(exception.getMessage()).contains(expiredToken);
        }
    }
}
