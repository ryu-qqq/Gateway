package com.ryuqq.gateway.domain.authentication.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PublicKeyNotFoundException 테스트")
class PublicKeyNotFoundExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("Key ID (kid)로 예외 생성")
        void shouldCreateExceptionWithKid() {
            // given
            String kid = "test-key-id-123";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).isEqualTo("Public key not found: kid=" + kid);
        }

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateExceptionWithDefaultMessage() {
            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException();

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).isEqualTo("Public key not found");
        }

        @Test
        @DisplayName("빈 문자열 kid로 예외 생성")
        void shouldCreateExceptionWithEmptyKid() {
            // given
            String kid = "";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).isEqualTo("Public key not found: kid=");
        }

        @Test
        @DisplayName("null kid로 예외 생성")
        void shouldCreateExceptionWithNullKid() {
            // given
            String kid = null;

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).isEqualTo("Public key not found: kid=null");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND와 매핑됨")
        void shouldMapToPublicKeyNotFoundErrorCode() {
            // given
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException("test-kid");

            // when & then
            assertThat(exception.code())
                    .isEqualTo(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode());
            assertThat(exception.code()).isEqualTo("AUTH-003");
        }

        @Test
        @DisplayName("기본 생성자도 동일한 ErrorCode 사용")
        void shouldUseSameErrorCodeForDefaultConstructor() {
            // given
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException();

            // when & then
            assertThat(exception.code())
                    .isEqualTo(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException("test-kid");

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            // when & then
            assertThat(
                            java.lang.reflect.Modifier.isFinal(
                                    PublicKeyNotFoundException.class.getModifiers()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 형식 테스트")
    class MessageFormatTest {

        @Test
        @DisplayName("메시지에 kid가 포함됨")
        void shouldIncludeKidInMessage() {
            // given
            String kid = "my-public-key-id";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
            assertThat(exception.getMessage()).contains("kid=");
        }

        @Test
        @DisplayName("메시지 형식이 올바름")
        void shouldHaveCorrectMessageFormat() {
            // given
            String kid = "test-kid";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            String expectedMessage = "Public key not found: kid=" + kid;
            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("기본 메시지가 ErrorCode 메시지와 동일")
        void shouldHaveDefaultMessageMatchingErrorCode() {
            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException();

            // then
            assertThat(exception.getMessage())
                    .isEqualTo(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getMessage());
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
                throw new PublicKeyNotFoundException("unknown-kid");
            } catch (PublicKeyNotFoundException e) {
                assertThat(e.code()).isEqualTo("AUTH-003");
                assertThat(e.getMessage()).contains("unknown-kid");
            }
        }

        @Test
        @DisplayName("try-catch로 잡을 수 있음")
        void shouldBeCatchable() {
            // given
            boolean caught = false;

            // when
            try {
                throw new PublicKeyNotFoundException();
            } catch (PublicKeyNotFoundException e) {
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
                throw new PublicKeyNotFoundException("test");
            } catch (DomainException e) {
                caught = true;
                caughtCode = e.code();
            }

            // then
            assertThat(caught).isTrue();
            assertThat(caughtCode).isEqualTo("AUTH-003");
        }
    }

    @Nested
    @DisplayName("비즈니스 시나리오 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("JWKS에서 키를 찾지 못한 시나리오")
        void jwksKeyNotFoundScenario() {
            // given
            String kid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).contains(kid);
        }

        @Test
        @DisplayName("캐시에서 키를 찾지 못한 시나리오")
        void cacheKeyNotFoundScenario() {
            // given
            String kid = "cached-key-expired";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).contains("kid=" + kid);
        }

        @Test
        @DisplayName("새로운 키 ID로 접근 시도 시나리오")
        void newKidAccessAttemptScenario() {
            // given
            String newKid = "new-rotation-key-2024";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(newKid);

            // then
            assertThat(exception.code()).isEqualTo("AUTH-003");
            assertThat(exception.getMessage()).contains(newKid);
        }
    }

    @Nested
    @DisplayName("다양한 kid 형식 테스트")
    class VariousKidFormatsTest {

        @Test
        @DisplayName("UUID 형식 kid 처리")
        void shouldHandleUuidKid() {
            // given
            String kid = "550e8400-e29b-41d4-a716-446655440000";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
        }

        @Test
        @DisplayName("숫자만 있는 kid 처리")
        void shouldHandleNumericKid() {
            // given
            String kid = "123456789";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
        }

        @Test
        @DisplayName("특수 문자가 포함된 kid 처리")
        void shouldHandleKidWithSpecialCharacters() {
            // given
            String kid = "key_2024-01-prod.v1";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
        }

        @Test
        @DisplayName("긴 kid 처리")
        void shouldHandleLongKid() {
            // given
            String kid =
                    "very-long-key-id-that-might-be-used-in-production-systems-"
                            + "with-detailed-naming-conventions";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
        }

        @Test
        @DisplayName("Base64 인코딩된 형식 kid 처리")
        void shouldHandleBase64Kid() {
            // given
            String kid = "SGVsbG9Xb3JsZA==";

            // when
            PublicKeyNotFoundException exception = new PublicKeyNotFoundException(kid);

            // then
            assertThat(exception.getMessage()).contains(kid);
        }
    }

    @Nested
    @DisplayName("HTTP 상태 코드 테스트")
    class HttpStatusTest {

        @Test
        @DisplayName("404 Not Found 상태 코드가 매핑됨")
        void shouldMapTo404HttpStatus() {
            // when & then
            assertThat(AuthenticationErrorCode.PUBLIC_KEY_NOT_FOUND.getHttpStatus()).isEqualTo(404);
        }
    }
}
