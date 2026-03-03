package com.ryuqq.gateway.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.bootstrap.config.GatewayRoutingConfig.ServiceRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GatewayRoutingConfig 단위 테스트
 *
 * <p>ServiceRoute의 RewritePath 설정 로직 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("GatewayRoutingConfig 단위 테스트")
class GatewayRoutingConfigTest {

    @Nested
    @DisplayName("ServiceRoute RewritePath 설정 테스트")
    class ServiceRouteRewritePathTest {

        @Test
        @DisplayName("pattern과 replacement 모두 설정 시 hasRewritePath()는 true를 반환한다")
        void shouldReturnTrueWhenBothPatternAndReplacementAreSet() {
            // given
            ServiceRoute route = new ServiceRoute();
            route.setRewritePathPattern("/api/v1/(?<remaining>.*)");
            route.setRewritePathReplacement("/api/v1/legacy/${remaining}");

            // when
            boolean result = route.hasRewritePath();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("pattern이 null이면 hasRewritePath()는 false를 반환한다")
        void shouldReturnFalseWhenPatternIsNull() {
            // given
            ServiceRoute route = new ServiceRoute();
            route.setRewritePathPattern(null);
            route.setRewritePathReplacement("/api/v1/legacy/${remaining}");

            // when
            boolean result = route.hasRewritePath();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("pattern이 빈 문자열이면 hasRewritePath()는 false를 반환한다")
        void shouldReturnFalseWhenPatternIsBlank() {
            // given
            ServiceRoute route = new ServiceRoute();
            route.setRewritePathPattern("   ");
            route.setRewritePathReplacement("/api/v1/legacy/${remaining}");

            // when
            boolean result = route.hasRewritePath();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("replacement가 null이면 hasRewritePath()는 false를 반환한다")
        void shouldReturnFalseWhenReplacementIsNull() {
            // given
            ServiceRoute route = new ServiceRoute();
            route.setRewritePathPattern("/api/v1/(?<remaining>.*)");
            route.setRewritePathReplacement(null);

            // when
            boolean result = route.hasRewritePath();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("replacement가 빈 문자열이면 hasRewritePath()는 false를 반환한다")
        void shouldReturnFalseWhenReplacementIsBlank() {
            // given
            ServiceRoute route = new ServiceRoute();
            route.setRewritePathPattern("/api/v1/(?<remaining>.*)");
            route.setRewritePathReplacement("");

            // when
            boolean result = route.hasRewritePath();

            // then
            assertThat(result).isFalse();
        }
    }
}
