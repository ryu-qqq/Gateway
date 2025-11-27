package com.ryuqq.gateway.application.ratelimit.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CheckRateLimitResponse 단위 테스트")
class CheckRateLimitResponseTest {

    @Nested
    @DisplayName("allowed 팩토리 메서드")
    class AllowedFactory {

        @Test
        @DisplayName("허용 Response 생성 - 카운트가 리밋 미만")
        void shouldCreateAllowedResponseWhenCountBelowLimit() {
            // when
            CheckRateLimitResponse response = CheckRateLimitResponse.allowed(50, 100);

            // then
            assertThat(response.allowed()).isTrue();
            assertThat(response.currentCount()).isEqualTo(50);
            assertThat(response.limit()).isEqualTo(100);
            assertThat(response.remaining()).isEqualTo(50);
            assertThat(response.retryAfterSeconds()).isEqualTo(0);
            assertThat(response.action()).isNull();
        }

        @Test
        @DisplayName("허용 Response 생성 - 카운트가 리밋과 동일")
        void shouldCreateAllowedResponseWhenCountEqualsLimit() {
            // when
            CheckRateLimitResponse response = CheckRateLimitResponse.allowed(100, 100);

            // then
            assertThat(response.allowed()).isTrue();
            assertThat(response.currentCount()).isEqualTo(100);
            assertThat(response.remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("허용 Response 생성 - 남은 카운트 계산")
        void shouldCalculateRemainingCorrectly() {
            // when
            CheckRateLimitResponse response = CheckRateLimitResponse.allowed(75, 100);

            // then
            assertThat(response.remaining()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("denied 팩토리 메서드")
    class DeniedFactory {

        @Test
        @DisplayName("거부 Response 생성 - REJECT 액션")
        void shouldCreateDeniedResponseWithRejectAction() {
            // when
            CheckRateLimitResponse response =
                    CheckRateLimitResponse.denied(101, 100, 60, RateLimitAction.REJECT);

            // then
            assertThat(response.allowed()).isFalse();
            assertThat(response.currentCount()).isEqualTo(101);
            assertThat(response.limit()).isEqualTo(100);
            assertThat(response.remaining()).isEqualTo(0);
            assertThat(response.retryAfterSeconds()).isEqualTo(60);
            assertThat(response.action()).isEqualTo(RateLimitAction.REJECT);
        }

        @Test
        @DisplayName("거부 Response 생성 - BLOCK_IP 액션")
        void shouldCreateDeniedResponseWithBlockIpAction() {
            // when
            CheckRateLimitResponse response =
                    CheckRateLimitResponse.denied(11, 10, 1800, RateLimitAction.BLOCK_IP);

            // then
            assertThat(response.allowed()).isFalse();
            assertThat(response.action()).isEqualTo(RateLimitAction.BLOCK_IP);
            assertThat(response.retryAfterSeconds()).isEqualTo(1800);
        }

        @Test
        @DisplayName("거부 Response 생성 - LOCK_ACCOUNT 액션")
        void shouldCreateDeniedResponseWithLockAccountAction() {
            // when
            CheckRateLimitResponse response =
                    CheckRateLimitResponse.denied(6, 5, 1800, RateLimitAction.LOCK_ACCOUNT);

            // then
            assertThat(response.allowed()).isFalse();
            assertThat(response.action()).isEqualTo(RateLimitAction.LOCK_ACCOUNT);
        }
    }
}
