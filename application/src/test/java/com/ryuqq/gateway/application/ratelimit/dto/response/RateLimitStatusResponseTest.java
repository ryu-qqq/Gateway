package com.ryuqq.gateway.application.ratelimit.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitStatusResponse 단위 테스트")
class RateLimitStatusResponseTest {

    @Nested
    @DisplayName("of 팩토리 메서드")
    class OfFactory {

        @Test
        @DisplayName("상태 Response 생성 - 정상 상태")
        void shouldCreateStatusResponse() {
            // when
            RateLimitStatusResponse response =
                    RateLimitStatusResponse.of(LimitType.IP, "192.168.1.1", 50, 100, 30);

            // then
            assertThat(response.limitType()).isEqualTo(LimitType.IP);
            assertThat(response.identifier()).isEqualTo("192.168.1.1");
            assertThat(response.currentCount()).isEqualTo(50);
            assertThat(response.limit()).isEqualTo(100);
            assertThat(response.remaining()).isEqualTo(50);
            assertThat(response.ttlSeconds()).isEqualTo(30);
            assertThat(response.blocked()).isFalse();
        }

        @Test
        @DisplayName("상태 Response 생성 - 차단된 상태")
        void shouldCreateBlockedStatusResponse() {
            // when
            RateLimitStatusResponse response =
                    RateLimitStatusResponse.of(LimitType.LOGIN, "192.168.1.1", 10, 5, 1500);

            // then
            assertThat(response.currentCount()).isEqualTo(10);
            assertThat(response.limit()).isEqualTo(5);
            assertThat(response.remaining()).isEqualTo(0);
            assertThat(response.blocked()).isTrue();
        }

        @Test
        @DisplayName("상태 Response 생성 - 남은 카운트 계산")
        void shouldCalculateRemainingCorrectly() {
            // when
            RateLimitStatusResponse response =
                    RateLimitStatusResponse.of(LimitType.USER, "user-123", 75, 100, 45);

            // then
            assertThat(response.remaining()).isEqualTo(25);
        }

        @Test
        @DisplayName("상태 Response 생성 - 남은 카운트 음수 방지")
        void shouldPreventNegativeRemaining() {
            // when
            RateLimitStatusResponse response =
                    RateLimitStatusResponse.of(LimitType.IP, "192.168.1.1", 150, 100, 30);

            // then
            assertThat(response.remaining()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("notFound 팩토리 메서드")
    class NotFoundFactory {

        @Test
        @DisplayName("존재하지 않는 키 Response 생성")
        void shouldCreateNotFoundResponse() {
            // when
            RateLimitStatusResponse response =
                    RateLimitStatusResponse.notFound(LimitType.IP, "192.168.1.1", 100);

            // then
            assertThat(response.limitType()).isEqualTo(LimitType.IP);
            assertThat(response.identifier()).isEqualTo("192.168.1.1");
            assertThat(response.currentCount()).isEqualTo(0);
            assertThat(response.limit()).isEqualTo(100);
            assertThat(response.remaining()).isEqualTo(100);
            assertThat(response.ttlSeconds()).isEqualTo(0);
            assertThat(response.blocked()).isFalse();
        }
    }
}
