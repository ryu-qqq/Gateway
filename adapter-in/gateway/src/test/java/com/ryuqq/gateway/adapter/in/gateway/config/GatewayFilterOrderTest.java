package com.ryuqq.gateway.adapter.in.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

/**
 * GatewayFilterOrder 상수 정의 테스트
 *
 * <p>Filter Order 상수 값과 순서를 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
class GatewayFilterOrderTest {

    @Test
    @DisplayName("JWT_AUTH_FILTER는 2로 정의되어야 한다")
    void shouldDefineCorrectFilterOrder() {
        // given & when
        int jwtAuthFilterOrder = GatewayFilterOrder.JWT_AUTH_FILTER;

        // then
        assertThat(jwtAuthFilterOrder).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
    }

    @Test
    @DisplayName("Filter 순서는 순차적으로 증가해야 한다 (0 -> 1 -> 2 -> ...)")
    void shouldMaintainFilterSequence() {
        // given & when & then
        assertThat(GatewayFilterOrder.TRACE_ID_FILTER).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(GatewayFilterOrder.RATE_LIMIT_FILTER)
                .isEqualTo(GatewayFilterOrder.TRACE_ID_FILTER + 1);
        assertThat(GatewayFilterOrder.JWT_AUTH_FILTER)
                .isEqualTo(GatewayFilterOrder.RATE_LIMIT_FILTER + 1);
        assertThat(GatewayFilterOrder.TOKEN_REFRESH_FILTER)
                .isEqualTo(GatewayFilterOrder.JWT_AUTH_FILTER + 1);
        assertThat(GatewayFilterOrder.TENANT_ISOLATION_FILTER)
                .isEqualTo(GatewayFilterOrder.TOKEN_REFRESH_FILTER + 1);
        assertThat(GatewayFilterOrder.PERMISSION_FILTER)
                .isEqualTo(GatewayFilterOrder.TENANT_ISOLATION_FILTER + 1);
        assertThat(GatewayFilterOrder.MFA_VERIFICATION_FILTER)
                .isEqualTo(GatewayFilterOrder.PERMISSION_FILTER + 1);
    }

    @Test
    @DisplayName("HIGHEST_PRECEDENCE는 Ordered.HIGHEST_PRECEDENCE와 동일해야 한다")
    void shouldDefineHighestPrecedenceCorrectly() {
        // given & when
        int highestPrecedence = GatewayFilterOrder.HIGHEST_PRECEDENCE;

        // then
        assertThat(highestPrecedence).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("private 생성자는 인스턴스화를 방지해야 한다")
    void shouldPreventInstantiation() throws NoSuchMethodException {
        // given
        Constructor<GatewayFilterOrder> constructor =
                GatewayFilterOrder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // when & then
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .getCause()
                .hasMessage("Utility class cannot be instantiated");
    }
}
