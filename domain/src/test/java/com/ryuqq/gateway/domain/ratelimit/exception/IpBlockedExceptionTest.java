package com.ryuqq.gateway.domain.ratelimit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IpBlockedException 테스트")
class IpBlockedExceptionTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("기본 생성자로 생성")
        void shouldCreateWithDefaultConstructor() {
            // when
            IpBlockedException exception = new IpBlockedException();

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.code()).isEqualTo("RATE-002");
            assertThat(exception.getMessage())
                    .isEqualTo("IP blocked due to abuse. Please try again later.");
        }

        @Test
        @DisplayName("IP 주소로 생성")
        void shouldCreateWithIpAddress() {
            // when
            IpBlockedException exception = new IpBlockedException("192.168.1.1");

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.code()).isEqualTo("RATE-002");
            assertThat(exception.getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("IP 주소와 차단 해제 시간으로 생성")
        void shouldCreateWithIpAddressAndUnblockTime() {
            // when
            IpBlockedException exception = new IpBlockedException("192.168.1.1", 1800);

            // then
            assertThat(exception.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(exception.getRetryAfterSeconds()).isEqualTo(1800);
        }

        @Test
        @DisplayName("args에 ipAddress, retryAfterSeconds 포함")
        void shouldIncludeArgsInException() {
            // when
            IpBlockedException exception = new IpBlockedException("192.168.1.1", 1800);

            // then
            assertThat(exception.args()).containsEntry("ipAddress", "192.168.1.1");
            assertThat(exception.args()).containsEntry("retryAfterSeconds", 1800);
        }
    }

    @Nested
    @DisplayName("DomainException 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            IpBlockedException exception = new IpBlockedException();

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("RuntimeException을 상속함")
        void shouldExtendRuntimeException() {
            // given
            IpBlockedException exception = new IpBlockedException();

            // when & then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("ErrorCode 연동 테스트")
    class ErrorCodeTest {

        @Test
        @DisplayName("IP_BLOCKED ErrorCode와 일치")
        void shouldMatchErrorCode() {
            // given
            IpBlockedException exception = new IpBlockedException();

            // when & then
            assertThat(exception.code()).isEqualTo(RateLimitErrorCode.IP_BLOCKED.getCode());
            assertThat(exception.getMessage())
                    .isEqualTo(RateLimitErrorCode.IP_BLOCKED.getMessage());
        }
    }
}
