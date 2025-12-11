package com.ryuqq.gateway.domain.ratelimit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitExceededException 테스트")
class RateLimitExceededExceptionTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("기본 생성자로 생성")
        void shouldCreateWithDefaultConstructor() {
            // when
            RateLimitExceededException exception = new RateLimitExceededException();

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.getCode()).isEqualTo("RATE-001");
            assertThat(exception.getMessage())
                    .isEqualTo("Too many requests. Please try again later.");
        }

        @Test
        @DisplayName("limit, remaining, retryAfterSeconds로 생성")
        void shouldCreateWithDetails() {
            // when
            RateLimitExceededException exception = new RateLimitExceededException(100, 0, 60);

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.getCode()).isEqualTo("RATE-001");
            assertThat(exception.limit()).isEqualTo(100);
            assertThat(exception.remaining()).isEqualTo(0);
            assertThat(exception.retryAfterSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("개별 필드 접근자를 통해 limit, remaining, retryAfterSeconds 확인")
        void shouldAccessFieldsThroughGetters() {
            // when
            RateLimitExceededException exception = new RateLimitExceededException(100, 0, 60);

            // then
            assertThat(exception.limit()).isEqualTo(100);
            assertThat(exception.remaining()).isEqualTo(0);
            assertThat(exception.retryAfterSeconds()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("DomainException 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            RateLimitExceededException exception = new RateLimitExceededException();

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("RuntimeException을 상속함")
        void shouldExtendRuntimeException() {
            // given
            RateLimitExceededException exception = new RateLimitExceededException();

            // when & then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("ErrorCode 연동 테스트")
    class ErrorCodeTest {

        @Test
        @DisplayName("RATE_LIMIT_EXCEEDED ErrorCode와 일치")
        void shouldMatchErrorCode() {
            // given
            RateLimitExceededException exception = new RateLimitExceededException();

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(RateLimitErrorCode.RATE_LIMIT_EXCEEDED.getCode());
            assertThat(exception.getMessage())
                    .isEqualTo(RateLimitErrorCode.RATE_LIMIT_EXCEEDED.getMessage());
        }
    }
}
