package com.ryuqq.gateway.adapter.in.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PublicPathsProperties 테스트
 *
 * <p>Host 기반 서비스의 public-paths 분리 로직을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
class PublicPathsPropertiesTest {

    private PublicPathsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PublicPathsProperties();
    }

    @Nested
    @DisplayName("getAllPublicPaths() 테스트")
    class GetAllPublicPathsTest {

        @Test
        @DisplayName("기본 public paths가 포함되어야 한다")
        void shouldIncludeDefaultPublicPaths() {
            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result).contains("/actuator/**", "/**/system/**");
        }

        @Test
        @DisplayName("글로벌 패턴이 포함되어야 한다")
        void shouldIncludeGlobalPublicPatterns() {
            // given
            properties.setGlobalPublicPatterns(
                    List.of("/**/public/**", "/**/api-docs/**", "/**/swagger"));

            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result).contains("/**/public/**", "/**/api-docs/**", "/**/swagger");
        }

        @Test
        @DisplayName("글로벌 패턴이 비어있으면 기존 동작을 유지해야 한다")
        void shouldWorkWithEmptyGlobalPatterns() {
            // given
            PublicPathsProperties.ServiceConfig authService =
                    new PublicPathsProperties.ServiceConfig();
            authService.setId("authhub");
            authService.setPublicPaths(List.of("/api/v1/auth/login"));

            properties.setServices(List.of(authService));
            // globalPublicPatterns는 기본값 빈 리스트

            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result)
                    .contains("/actuator/**", "/**/system/**", "/api/v1/auth/login")
                    .hasSize(3);
        }

        @Test
        @DisplayName("hosts가 없는 서비스의 public-paths가 포함되어야 한다")
        void shouldIncludePublicPathsFromServicesWithoutHosts() {
            // given
            PublicPathsProperties.ServiceConfig authService =
                    new PublicPathsProperties.ServiceConfig();
            authService.setId("authhub");
            authService.setPublicPaths(List.of("/api/v1/auth/login", "/api/v1/auth/register"));
            // hosts 설정 안함 (null)

            properties.setServices(List.of(authService));

            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result).contains("/api/v1/auth/login", "/api/v1/auth/register");
        }

        @Test
        @DisplayName("hosts가 정의된 서비스의 public-paths는 제외되어야 한다")
        void shouldExcludePublicPathsFromServicesWithHosts() {
            // given
            PublicPathsProperties.ServiceConfig legacyService =
                    new PublicPathsProperties.ServiceConfig();
            legacyService.setId("legacy-web");
            legacyService.setPublicPaths(List.of("/**"));
            legacyService.setHosts(List.of("stage.set-of.com", "set-of.com"));

            properties.setServices(List.of(legacyService));

            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result).doesNotContain("/**");
        }

        @Test
        @DisplayName("hosts가 있는 서비스와 없는 서비스가 혼합된 경우 올바르게 필터링해야 한다")
        void shouldCorrectlyFilterMixedServices() {
            // given
            PublicPathsProperties.ServiceConfig authService =
                    new PublicPathsProperties.ServiceConfig();
            authService.setId("authhub");
            authService.setPublicPaths(List.of("/api/v1/auth/login"));
            // hosts 없음

            PublicPathsProperties.ServiceConfig legacyService =
                    new PublicPathsProperties.ServiceConfig();
            legacyService.setId("legacy-web");
            legacyService.setPublicPaths(List.of("/**"));
            legacyService.setHosts(List.of("stage.set-of.com"));

            properties.setServices(List.of(authService, legacyService));

            // when
            List<String> result = properties.getAllPublicPaths();

            // then
            assertThat(result)
                    .contains("/api/v1/auth/login") // hosts 없는 서비스의 path는 포함
                    .doesNotContain("/**"); // hosts 있는 서비스의 path는 제외
        }
    }

    @Nested
    @DisplayName("getPublicPathsForHost() 테스트")
    class GetPublicPathsForHostTest {

        @Test
        @DisplayName("매칭되는 host의 public-paths를 반환해야 한다")
        void shouldReturnPublicPathsForMatchingHost() {
            // given
            PublicPathsProperties.ServiceConfig legacyService =
                    new PublicPathsProperties.ServiceConfig();
            legacyService.setId("legacy-web");
            legacyService.setPublicPaths(List.of("/**"));
            legacyService.setHosts(List.of("stage.set-of.com", "set-of.com"));

            properties.setServices(List.of(legacyService));

            // when
            List<String> result = properties.getPublicPathsForHost("stage.set-of.com");

            // then
            assertThat(result).containsExactly("/**");
        }

        @Test
        @DisplayName("매칭되지 않는 host는 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyListForNonMatchingHost() {
            // given
            PublicPathsProperties.ServiceConfig legacyService =
                    new PublicPathsProperties.ServiceConfig();
            legacyService.setId("legacy-web");
            legacyService.setPublicPaths(List.of("/**"));
            legacyService.setHosts(List.of("stage.set-of.com"));

            properties.setServices(List.of(legacyService));

            // when
            List<String> result = properties.getPublicPathsForHost("api.set-of.com");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null host는 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyListForNullHost() {
            // when
            List<String> result = properties.getPublicPathsForHost(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 host는 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyListForEmptyHost() {
            // when
            List<String> result = properties.getPublicPathsForHost("");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("host 매칭은 대소문자를 구분하지 않아야 한다")
        void shouldMatchHostCaseInsensitively() {
            // given
            PublicPathsProperties.ServiceConfig service = new PublicPathsProperties.ServiceConfig();
            service.setId("legacy-web");
            service.setPublicPaths(List.of("/**"));
            service.setHosts(List.of("Stage.Set-Of.Com"));

            properties.setServices(List.of(service));

            // when
            List<String> result = properties.getPublicPathsForHost("stage.set-of.com");

            // then
            assertThat(result).containsExactly("/**");
        }
    }

    @Nested
    @DisplayName("ServiceConfig 테스트")
    class ServiceConfigTest {

        @Test
        @DisplayName("hasHosts()는 hosts가 비어있으면 false를 반환해야 한다")
        void hasHostsShouldReturnFalseWhenHostsEmpty() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();
            config.setHosts(List.of());

            // then
            assertThat(config.hasHosts()).isFalse();
        }

        @Test
        @DisplayName("hasHosts()는 hosts가 null이면 false를 반환해야 한다")
        void hasHostsShouldReturnFalseWhenHostsNull() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();
            config.setHosts(null);

            // then
            assertThat(config.hasHosts()).isFalse();
        }

        @Test
        @DisplayName("hasHosts()는 hosts가 있으면 true를 반환해야 한다")
        void hasHostsShouldReturnTrueWhenHostsExist() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();
            config.setHosts(List.of("example.com"));

            // then
            assertThat(config.hasHosts()).isTrue();
        }

        @Test
        @DisplayName("matchesHost()는 매칭되는 host가 있으면 true를 반환해야 한다")
        void matchesHostShouldReturnTrueForMatchingHost() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();
            config.setHosts(List.of("example.com", "test.com"));

            // then
            assertThat(config.matchesHost("example.com")).isTrue();
            assertThat(config.matchesHost("test.com")).isTrue();
        }

        @Test
        @DisplayName("matchesHost()는 매칭되는 host가 없으면 false를 반환해야 한다")
        void matchesHostShouldReturnFalseForNonMatchingHost() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();
            config.setHosts(List.of("example.com"));

            // then
            assertThat(config.matchesHost("other.com")).isFalse();
        }

        @Test
        @DisplayName("matchesHost()는 hosts가 없으면 false를 반환해야 한다")
        void matchesHostShouldReturnFalseWhenNoHosts() {
            // given
            PublicPathsProperties.ServiceConfig config = new PublicPathsProperties.ServiceConfig();

            // then
            assertThat(config.matchesHost("any.com")).isFalse();
        }
    }
}
