package com.ryuqq.gateway.domain.ratelimit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitErrorCode 테스트")
class RateLimitErrorCodeTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 에러 코드가 정의되어 있음")
        void shouldHaveAllErrorCodes() {
            // when
            RateLimitErrorCode[] errorCodes = RateLimitErrorCode.values();

            // then
            assertThat(errorCodes).hasSize(3);
            assertThat(errorCodes)
                    .containsExactly(
                            RateLimitErrorCode.RATE_LIMIT_EXCEEDED,
                            RateLimitErrorCode.IP_BLOCKED,
                            RateLimitErrorCode.ACCOUNT_LOCKED);
        }
    }

    @Nested
    @DisplayName("RATE_LIMIT_EXCEEDED 테스트")
    class RateLimitExceededTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(RateLimitErrorCode.RATE_LIMIT_EXCEEDED.getCode()).isEqualTo("RATE-001");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (429 Too Many Requests)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(RateLimitErrorCode.RATE_LIMIT_EXCEEDED.getMessage())
                    .isEqualTo("Too many requests. Please try again later.");
        }
    }

    @Nested
    @DisplayName("IP_BLOCKED 테스트")
    class IpBlockedTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(RateLimitErrorCode.IP_BLOCKED.getCode()).isEqualTo("RATE-002");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (403 Forbidden)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitErrorCode.IP_BLOCKED.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(RateLimitErrorCode.IP_BLOCKED.getMessage())
                    .isEqualTo("IP blocked due to abuse. Please try again later.");
        }
    }

    @Nested
    @DisplayName("ACCOUNT_LOCKED 테스트")
    class AccountLockedTest {

        @Test
        @DisplayName("올바른 코드 값을 가짐")
        void shouldHaveCorrectCode() {
            assertThat(RateLimitErrorCode.ACCOUNT_LOCKED.getCode()).isEqualTo("RATE-003");
        }

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (403 Forbidden)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitErrorCode.ACCOUNT_LOCKED.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 메시지를 가짐")
        void shouldHaveCorrectMessage() {
            assertThat(RateLimitErrorCode.ACCOUNT_LOCKED.getMessage())
                    .isEqualTo("Account locked due to too many failures.");
        }
    }

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 테스트")
    class ErrorCodeInterfaceTest {

        @Test
        @DisplayName("모든 에러 코드가 ErrorCode 인터페이스를 구현함")
        void shouldImplementErrorCodeInterface() {
            for (RateLimitErrorCode errorCode : RateLimitErrorCode.values()) {
                assertThat(errorCode.getCode()).isNotNull();
                assertThat(errorCode.getHttpStatus()).isPositive();
                assertThat(errorCode.getMessage()).isNotNull();
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 RATE- 접두사를 가짐")
        void shouldHaveRatePrefix() {
            for (RateLimitErrorCode errorCode : RateLimitErrorCode.values()) {
                assertThat(errorCode.getCode()).startsWith("RATE-");
            }
        }

        @Test
        @DisplayName("모든 에러 코드가 3자리 숫자 형식을 가짐")
        void shouldHaveThreeDigitFormat() {
            for (RateLimitErrorCode errorCode : RateLimitErrorCode.values()) {
                String code = errorCode.getCode();
                String numberPart = code.substring("RATE-".length());
                assertThat(numberPart).matches("\\d{3}");
            }
        }

        @Test
        @DisplayName("에러 코드가 중복되지 않음")
        void shouldHaveUniqueErrorCodes() {
            RateLimitErrorCode[] errorCodes = RateLimitErrorCode.values();

            for (int i = 0; i < errorCodes.length; i++) {
                for (int j = i + 1; j < errorCodes.length; j++) {
                    assertThat(errorCodes[i].getCode()).isNotEqualTo(errorCodes[j].getCode());
                }
            }
        }
    }
}
