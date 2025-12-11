package com.ryuqq.gateway.domain.authentication.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtInvalidException 테스트")
class JwtInvalidExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("이유(reason)로 예외 생성")
        void shouldCreateExceptionWithReason() {
            // given
            String reason = "Invalid signature";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).isEqualTo("JWT token is invalid: " + reason);
        }

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateExceptionWithDefaultMessage() {
            // when
            JwtInvalidException exception = new JwtInvalidException();

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).isEqualTo("JWT token is invalid");
        }

        @Test
        @DisplayName("빈 문자열 이유로 예외 생성")
        void shouldCreateExceptionWithEmptyReason() {
            // given
            String reason = "";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).isEqualTo("JWT token is invalid: ");
        }

        @Test
        @DisplayName("null 이유로 예외 생성")
        void shouldCreateExceptionWithNullReason() {
            // given
            String reason = null;

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            // DomainException은 detail이 null이면 기본 메시지만 사용
            assertThat(exception.getMessage()).isEqualTo("JWT token is invalid");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("AuthenticationErrorCode.JWT_INVALID와 매핑됨")
        void shouldMapToJwtInvalidErrorCode() {
            // given
            JwtInvalidException exception = new JwtInvalidException("test reason");

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthenticationErrorCode.JWT_INVALID.getCode());
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
        }

        @Test
        @DisplayName("기본 생성자도 동일한 ErrorCode 사용")
        void shouldUseSameErrorCodeForDefaultConstructor() {
            // given
            JwtInvalidException exception = new JwtInvalidException();

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthenticationErrorCode.JWT_INVALID.getCode());
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            JwtInvalidException exception = new JwtInvalidException("test");

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            // when & then
            assertThat(java.lang.reflect.Modifier.isFinal(JwtInvalidException.class.getModifiers()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 형식 테스트")
    class MessageFormatTest {

        @Test
        @DisplayName("메시지에 이유가 포함됨")
        void shouldIncludeReasonInMessage() {
            // given
            String reason = "Missing required claim: sub";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getMessage()).contains(reason);
            assertThat(exception.getMessage()).startsWith("JWT token is invalid:");
        }

        @Test
        @DisplayName("메시지 형식이 올바름")
        void shouldHaveCorrectMessageFormat() {
            // given
            String reason = "Invalid format";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            String expectedMessage = "JWT token is invalid: " + reason;
            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("기본 메시지가 ErrorCode 메시지와 동일")
        void shouldHaveDefaultMessageMatchingErrorCode() {
            // when
            JwtInvalidException exception = new JwtInvalidException();

            // then
            assertThat(exception.getMessage())
                    .isEqualTo(AuthenticationErrorCode.JWT_INVALID.getMessage());
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
                throw new JwtInvalidException("Invalid signature");
            } catch (JwtInvalidException e) {
                assertThat(e.getCode()).isEqualTo("AUTH-002");
                assertThat(e.getMessage()).contains("Invalid signature");
            }
        }

        @Test
        @DisplayName("try-catch로 잡을 수 있음")
        void shouldBeCatchable() {
            // given
            boolean caught = false;

            // when
            try {
                throw new JwtInvalidException();
            } catch (JwtInvalidException e) {
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
                throw new JwtInvalidException("test");
            } catch (DomainException e) {
                caught = true;
                caughtCode = e.getCode();
            }

            // then
            assertThat(caught).isTrue();
            assertThat(caughtCode).isEqualTo("AUTH-002");
        }
    }

    @Nested
    @DisplayName("비즈니스 시나리오 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("잘못된 형식 시나리오")
        void invalidFormatScenario() {
            // given
            String reason = "Invalid JWT format: expected 3 parts but got 2";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("format");
        }

        @Test
        @DisplayName("서명 검증 실패 시나리오")
        void signatureVerificationFailureScenario() {
            // given
            String reason = "Signature verification failed";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("Signature");
        }

        @Test
        @DisplayName("필수 클레임 누락 시나리오")
        void missingRequiredClaimScenario() {
            // given
            String reason = "Missing required claim: iss";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("Missing required claim");
        }

        @Test
        @DisplayName("잘못된 알고리즘 시나리오")
        void invalidAlgorithmScenario() {
            // given
            String reason = "Unsupported algorithm: HS256, expected RS256";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("algorithm");
        }

        @Test
        @DisplayName("kid 누락 시나리오")
        void missingKidScenario() {
            // given
            String reason = "JWT Header does not contain 'kid' claim";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("kid");
        }

        @Test
        @DisplayName("null 또는 blank 토큰 시나리오")
        void nullOrBlankTokenScenario() {
            // given
            String reason = "Access token cannot be null or blank";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTH-002");
            assertThat(exception.getMessage()).contains("null or blank");
        }
    }

    @Nested
    @DisplayName("다양한 이유 테스트")
    class VariousReasonsTest {

        @Test
        @DisplayName("긴 이유 메시지 처리")
        void shouldHandleLongReasonMessage() {
            // given
            String reason =
                    "This is a very long reason message that explains the detailed cause of "
                            + "the JWT validation failure including technical details";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getMessage()).contains(reason);
        }

        @Test
        @DisplayName("특수 문자가 포함된 이유 처리")
        void shouldHandleReasonWithSpecialCharacters() {
            // given
            String reason = "Invalid claim: sub=user@example.com";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getMessage()).contains(reason);
        }

        @Test
        @DisplayName("줄바꿈이 포함된 이유 처리")
        void shouldHandleReasonWithNewlines() {
            // given
            String reason = "Multiple issues:\n1. Invalid format\n2. Wrong algorithm";

            // when
            JwtInvalidException exception = new JwtInvalidException(reason);

            // then
            assertThat(exception.getMessage()).contains(reason);
        }
    }
}
