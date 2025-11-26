package com.ryuqq.gateway.adapter.out.authhub.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * AuthHubPermissionAdapter 단위 테스트
 *
 * <p>WebClient Mock 기반 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AuthHubPermissionAdapter 단위 테스트")
class AuthHubPermissionAdapterTest {

    private WebClient webClient;
    private AuthHubPermissionAdapter adapter;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        adapter = new AuthHubPermissionAdapter(webClient);
    }

    @Nested
    @DisplayName("fetchPermissionSpec 메서드")
    class FetchPermissionSpecTest {

        @Test
        @DisplayName("Permission Spec 응답이 정상일 때 PermissionSpec을 반환한다")
        @SuppressWarnings("unchecked")
        void shouldReturnPermissionSpecWhenResponseIsValid() {
            // given
            AuthHubPermissionAdapter.EndpointPermissionResponse epResponse =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "order-service",
                            "/api/v1/orders",
                            "GET",
                            List.of("order:read"),
                            List.of("USER"),
                            false);

            AuthHubPermissionAdapter.PermissionSpecResponse specResponse =
                    new AuthHubPermissionAdapter.PermissionSpecResponse(
                            1L, Instant.parse("2024-01-01T00:00:00Z"), List.of(epResponse));

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubPermissionAdapter.PermissionSpecResponse.class))
                    .willReturn(Mono.just(specResponse));

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .assertNext(
                            spec -> {
                                assertThat(spec.version()).isEqualTo(1L);
                                assertThat(spec.permissions()).hasSize(1);
                                assertThat(spec.permissions().getFirst().serviceName())
                                        .isEqualTo("order-service");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("WebClient 호출 시 예외가 발생하면 AuthHubPermissionException으로 변환한다")
        @SuppressWarnings("unchecked")
        void shouldWrapExceptionIntoAuthHubPermissionException() {
            // given
            RuntimeException originalException = new RuntimeException("Connection failed");

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubPermissionAdapter.PermissionSpecResponse.class))
                    .willReturn(Mono.error(originalException));

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(
                                                AuthHubPermissionAdapter.AuthHubPermissionException
                                                        .class);
                                assertThat(error.getMessage())
                                        .contains("Failed to fetch Permission Spec");
                                assertThat(error.getCause()).isEqualTo(originalException);
                            })
                    .verify();
        }

        @Test
        @DisplayName("AuthHubPermissionException이 발생하면 그대로 전파한다")
        @SuppressWarnings("unchecked")
        void shouldPropagateAuthHubPermissionExceptionAsIs() {
            // given
            AuthHubPermissionAdapter.AuthHubPermissionException authHubException =
                    new AuthHubPermissionAdapter.AuthHubPermissionException("Original error");

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubPermissionAdapter.PermissionSpecResponse.class))
                    .willReturn(Mono.error(authHubException));

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(
                                                AuthHubPermissionAdapter.AuthHubPermissionException
                                                        .class);
                                assertThat(error.getMessage()).isEqualTo("Original error");
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("fetchUserPermissions 메서드")
    class FetchUserPermissionsTest {

        @Test
        @DisplayName("사용자 권한 응답이 정상일 때 PermissionHash를 반환한다")
        @SuppressWarnings("unchecked")
        void shouldReturnPermissionHashWhenResponseIsValid() {
            // given
            AuthHubPermissionAdapter.PermissionHashResponse hashResponse =
                    new AuthHubPermissionAdapter.PermissionHashResponse(
                            "hash-value",
                            List.of("order:read", "order:write"),
                            List.of("USER", "ADMIN"),
                            Instant.parse("2024-01-01T00:00:00Z"));

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), anyString()))
                    .willReturn(requestHeadersSpec);
            given(requestHeadersSpec.header(anyString(), anyString()))
                    .willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubPermissionAdapter.PermissionHashResponse.class))
                    .willReturn(Mono.just(hashResponse));

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions("tenant-1", "user-1"))
                    .assertNext(
                            hash -> {
                                assertThat(hash.hash()).isEqualTo("hash-value");
                                assertThat(hash.permissionStrings())
                                        .containsExactlyInAnyOrder("order:read", "order:write");
                                assertThat(hash.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("WebClient 호출 시 예외가 발생하면 AuthHubPermissionException으로 변환한다")
        @SuppressWarnings("unchecked")
        void shouldWrapExceptionIntoAuthHubPermissionException() {
            // given
            RuntimeException originalException = new RuntimeException("Connection failed");

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), anyString()))
                    .willReturn(requestHeadersSpec);
            given(requestHeadersSpec.header(anyString(), anyString()))
                    .willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubPermissionAdapter.PermissionHashResponse.class))
                    .willReturn(Mono.error(originalException));

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions("tenant-1", "user-1"))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(
                                                AuthHubPermissionAdapter.AuthHubPermissionException
                                                        .class);
                                assertThat(error.getMessage())
                                        .contains("Failed to fetch User Permissions: user-1");
                                assertThat(error.getCause()).isEqualTo(originalException);
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("toPermissionSpec 메서드")
    class ToPermissionSpecTest {

        @Test
        @DisplayName("null 응답일 때 AuthHubPermissionException을 발생시킨다")
        void shouldThrowExceptionWhenResponseIsNull() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    AuthHubPermissionAdapter.AuthHubPermissionException.class,
                    () -> adapter.toPermissionSpec(null));
        }

        @Test
        @DisplayName("정상 응답일 때 PermissionSpec으로 변환한다")
        void shouldConvertToPermissionSpec() {
            // given
            AuthHubPermissionAdapter.EndpointPermissionResponse epResponse =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "order-service",
                            "/api/v1/orders",
                            "POST",
                            List.of("order:create"),
                            List.of("USER"),
                            false);

            AuthHubPermissionAdapter.PermissionSpecResponse specResponse =
                    new AuthHubPermissionAdapter.PermissionSpecResponse(
                            2L, Instant.parse("2024-06-01T00:00:00Z"), List.of(epResponse));

            // when
            PermissionSpec result = adapter.toPermissionSpec(specResponse);

            // then
            assertThat(result.version()).isEqualTo(2L);
            assertThat(result.permissions()).hasSize(1);
            assertThat(result.permissions().getFirst().serviceName()).isEqualTo("order-service");
            assertThat(result.permissions().getFirst().path()).isEqualTo("/api/v1/orders");
        }
    }

    @Nested
    @DisplayName("toEndpointPermission 메서드")
    class ToEndpointPermissionTest {

        @Test
        @DisplayName("EndpointPermissionResponse를 EndpointPermission으로 변환한다")
        void shouldConvertToEndpointPermission() {
            // given
            AuthHubPermissionAdapter.EndpointPermissionResponse epResponse =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "user-service",
                            "/api/v1/users/{userId}",
                            "delete",
                            List.of("user:delete"),
                            List.of("ADMIN"),
                            false);

            // when
            var result = adapter.toEndpointPermission(epResponse);

            // then
            assertThat(result.serviceName()).isEqualTo("user-service");
            assertThat(result.path()).isEqualTo("/api/v1/users/{userId}");
            assertThat(result.method().name()).isEqualTo("DELETE");
            assertThat(result.requiredRoles()).contains("ADMIN");
            assertThat(result.isPublic()).isFalse();
        }

        @Test
        @DisplayName("Public 엔드포인트를 올바르게 변환한다")
        void shouldConvertPublicEndpoint() {
            // given
            AuthHubPermissionAdapter.EndpointPermissionResponse epResponse =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "public-service",
                            "/api/v1/health",
                            "get",
                            List.of(),
                            List.of(),
                            true);

            // when
            var result = adapter.toEndpointPermission(epResponse);

            // then
            assertThat(result.isPublic()).isTrue();
            assertThat(result.requiredPermissions()).isEmpty();
            assertThat(result.requiredRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toPermissionHash 메서드")
    class ToPermissionHashTest {

        @Test
        @DisplayName("null 응답일 때 AuthHubPermissionException을 발생시킨다")
        void shouldThrowExceptionWhenResponseIsNull() {
            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    AuthHubPermissionAdapter.AuthHubPermissionException.class,
                    () -> adapter.toPermissionHash(null));
        }

        @Test
        @DisplayName("정상 응답일 때 PermissionHash로 변환한다")
        void shouldConvertToPermissionHash() {
            // given
            AuthHubPermissionAdapter.PermissionHashResponse hashResponse =
                    new AuthHubPermissionAdapter.PermissionHashResponse(
                            "test-hash",
                            List.of("product:read", "product:write"),
                            List.of("SELLER"),
                            Instant.parse("2024-03-15T10:00:00Z"));

            // when
            PermissionHash result = adapter.toPermissionHash(hashResponse);

            // then
            assertThat(result.hash()).isEqualTo("test-hash");
            assertThat(result.permissionStrings())
                    .containsExactlyInAnyOrder("product:read", "product:write");
            assertThat(result.roles()).containsExactly("SELLER");
        }
    }

    @Nested
    @DisplayName("Fallback 메서드")
    class FallbackTest {

        @Test
        @DisplayName("fetchPermissionSpecFallback 호출 시 AuthHubPermissionException을 반환한다")
        void shouldReturnExceptionOnPermissionSpecFallback() {
            // given
            RuntimeException cause = new RuntimeException("Original error");

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpecFallback(cause))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(
                                                AuthHubPermissionAdapter.AuthHubPermissionException
                                                        .class);
                                assertThat(error.getMessage())
                                        .contains("Permission Spec 조회 실패 (Fallback)");
                                assertThat(error.getCause()).isEqualTo(cause);
                            })
                    .verify();
        }

        @Test
        @DisplayName("fetchUserPermissionsFallback 호출 시 AuthHubPermissionException을 반환한다")
        void shouldReturnExceptionOnUserPermissionsFallback() {
            // given
            RuntimeException cause = new RuntimeException("Original error");

            // when & then
            StepVerifier.create(
                            adapter.fetchUserPermissionsFallback("tenant-1", "user-123", cause))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(
                                                AuthHubPermissionAdapter.AuthHubPermissionException
                                                        .class);
                                assertThat(error.getMessage())
                                        .contains("User Permissions 조회 실패 (Fallback): user-123");
                                assertThat(error.getCause()).isEqualTo(cause);
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("AuthHubPermissionException 클래스")
    class AuthHubPermissionExceptionTest {

        @Test
        @DisplayName("메시지만 있는 생성자")
        void shouldCreateExceptionWithMessage() {
            // when
            AuthHubPermissionAdapter.AuthHubPermissionException exception =
                    new AuthHubPermissionAdapter.AuthHubPermissionException("Test message");

            // then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("메시지와 원인이 있는 생성자")
        void shouldCreateExceptionWithMessageAndCause() {
            // given
            Throwable cause = new RuntimeException("Original cause");

            // when
            AuthHubPermissionAdapter.AuthHubPermissionException exception =
                    new AuthHubPermissionAdapter.AuthHubPermissionException("Test message", cause);

            // then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("DTO 레코드 테스트")
    class DtoRecordTest {

        @Test
        @DisplayName("PermissionSpecResponse 레코드가 값을 올바르게 저장한다")
        void shouldStorePermissionSpecResponseValues() {
            // given
            AuthHubPermissionAdapter.EndpointPermissionResponse ep =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "test-service", "/test", "GET", List.of(), List.of(), true);
            Instant timestamp = Instant.now();

            // when
            AuthHubPermissionAdapter.PermissionSpecResponse response =
                    new AuthHubPermissionAdapter.PermissionSpecResponse(
                            5L, timestamp, List.of(ep));

            // then
            assertThat(response.version()).isEqualTo(5L);
            assertThat(response.updatedAt()).isEqualTo(timestamp);
            assertThat(response.permissions()).hasSize(1);
        }

        @Test
        @DisplayName("EndpointPermissionResponse 레코드가 값을 올바르게 저장한다")
        void shouldStoreEndpointPermissionResponseValues() {
            // when
            AuthHubPermissionAdapter.EndpointPermissionResponse response =
                    new AuthHubPermissionAdapter.EndpointPermissionResponse(
                            "my-service",
                            "/api/test",
                            "PUT",
                            List.of("resource:update"),
                            List.of("EDITOR"),
                            false);

            // then
            assertThat(response.serviceName()).isEqualTo("my-service");
            assertThat(response.path()).isEqualTo("/api/test");
            assertThat(response.method()).isEqualTo("PUT");
            assertThat(response.requiredPermissions()).containsExactly("resource:update");
            assertThat(response.requiredRoles()).containsExactly("EDITOR");
            assertThat(response.isPublic()).isFalse();
        }

        @Test
        @DisplayName("PermissionHashResponse 레코드가 값을 올바르게 저장한다")
        void shouldStorePermissionHashResponseValues() {
            // given
            Instant timestamp = Instant.now();

            // when
            AuthHubPermissionAdapter.PermissionHashResponse response =
                    new AuthHubPermissionAdapter.PermissionHashResponse(
                            "hash123",
                            List.of("perm:a", "perm:b"),
                            List.of("ROLE_A", "ROLE_B"),
                            timestamp);

            // then
            assertThat(response.hash()).isEqualTo("hash123");
            assertThat(response.permissions()).containsExactly("perm:a", "perm:b");
            assertThat(response.roles()).containsExactly("ROLE_A", "ROLE_B");
            assertThat(response.generatedAt()).isEqualTo(timestamp);
        }
    }
}
