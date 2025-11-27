package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ExpiredTokenInfo VO Test
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("ExpiredTokenInfo VO 테스트")
class ExpiredTokenInfoTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("만료된 토큰 정보를 올바르게 생성해야 한다")
        void shouldCreateExpiredTokenInfo() {
            // given
            boolean expired = true;
            Long userId = 123L;
            String tenantId = "tenant-001";

            // when
            ExpiredTokenInfo tokenInfo = ExpiredTokenInfo.of(expired, userId, tenantId);

            // then
            assertThat(tokenInfo.isExpired()).isTrue();
            assertThat(tokenInfo.userId()).isEqualTo(123L);
            assertThat(tokenInfo.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("만료되지 않은 토큰 정보를 올바르게 생성해야 한다")
        void shouldCreateNonExpiredTokenInfo() {
            // given
            boolean expired = false;
            Long userId = 456L;
            String tenantId = "tenant-002";

            // when
            ExpiredTokenInfo tokenInfo = ExpiredTokenInfo.of(expired, userId, tenantId);

            // then
            assertThat(tokenInfo.isExpired()).isFalse();
            assertThat(tokenInfo.userId()).isEqualTo(456L);
            assertThat(tokenInfo.tenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("null userId와 tenantId도 허용해야 한다")
        void shouldAllowNullValues() {
            // given
            boolean expired = true;
            Long userId = null;
            String tenantId = null;

            // when
            ExpiredTokenInfo tokenInfo = ExpiredTokenInfo.of(expired, userId, tenantId);

            // then
            assertThat(tokenInfo.isExpired()).isTrue();
            assertThat(tokenInfo.userId()).isNull();
            assertThat(tokenInfo.tenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("동등성 테스트")
    class EqualityTest {

        @Test
        @DisplayName("같은 값을 가진 객체는 동등해야 한다")
        void shouldBeEqualWithSameValues() {
            // given
            ExpiredTokenInfo info1 = ExpiredTokenInfo.of(true, 123L, "tenant-001");
            ExpiredTokenInfo info2 = ExpiredTokenInfo.of(true, 123L, "tenant-001");

            // when & then
            assertThat(info1).isEqualTo(info2);
            assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 객체는 동등하지 않아야 한다")
        void shouldNotBeEqualWithDifferentValues() {
            // given
            ExpiredTokenInfo info1 = ExpiredTokenInfo.of(true, 123L, "tenant-001");
            ExpiredTokenInfo info2 = ExpiredTokenInfo.of(false, 123L, "tenant-001");
            ExpiredTokenInfo info3 = ExpiredTokenInfo.of(true, 456L, "tenant-001");
            ExpiredTokenInfo info4 = ExpiredTokenInfo.of(true, 123L, "tenant-002");

            // when & then
            assertThat(info1).isNotEqualTo(info2);
            assertThat(info1).isNotEqualTo(info3);
            assertThat(info1).isNotEqualTo(info4);
        }

        @Test
        @DisplayName("null과 비교하면 동등하지 않아야 한다")
        void shouldNotBeEqualToNull() {
            // given
            ExpiredTokenInfo info = ExpiredTokenInfo.of(true, 123L, "tenant-001");

            // when & then
            assertThat(info).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString은 모든 필드를 포함해야 한다")
        void shouldIncludeAllFieldsInToString() {
            // given
            ExpiredTokenInfo tokenInfo = ExpiredTokenInfo.of(true, 123L, "tenant-001");

            // when
            String result = tokenInfo.toString();

            // then
            assertThat(result).contains("expired=true");
            assertThat(result).contains("userId=123");
            assertThat(result).contains("tenantId='tenant-001'");
        }
    }
}
