package com.ryuqq.gateway.adapter.out.authhub.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * AuthHubAdapter Unit Test
 *
 * <p>WebClient Mock 기반 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AuthHubAdapter 단위 테스트")
class AuthHubAdapterTest {

    private WebClient webClient;
    private AuthHubProperties properties;
    private AuthHubAdapter adapter;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        properties = new AuthHubProperties();
        properties.setBaseUrl("http://localhost:9090");
        properties.setJwksEndpoint("/api/v1/auth/jwks");

        adapter = new AuthHubAdapter(webClient, properties);
    }

    @Nested
    @DisplayName("fetchPublicKeys 메서드")
    class FetchPublicKeysTest {

        @Test
        @DisplayName("JWKS 응답이 정상일 때 PublicKey 목록을 반환한다")
        @SuppressWarnings("unchecked")
        void shouldReturnPublicKeysWhenJwksResponseIsValid() {
            // given
            AuthHubAdapter.Jwk keyData =
                    new AuthHubAdapter.Jwk(
                            "key-id-1", "modulus-value", "AQAB", "RSA", "sig", "RS256");

            AuthHubAdapter.JwksResponse jwksResponse =
                    new AuthHubAdapter.JwksResponse(List.of(keyData));

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubAdapter.JwksResponse.class))
                    .willReturn(Mono.just(jwksResponse));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .assertNext(
                            publicKey -> {
                                assertThat(publicKey.kid()).isEqualTo("key-id-1");
                                assertThat(publicKey.modulus()).isEqualTo("modulus-value");
                                assertThat(publicKey.exponent()).isEqualTo("AQAB");
                                assertThat(publicKey.kty()).isEqualTo("RSA");
                                assertThat(publicKey.use()).isEqualTo("sig");
                                assertThat(publicKey.alg()).isEqualTo("RS256");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("여러 개의 PublicKey를 반환한다")
        @SuppressWarnings("unchecked")
        void shouldReturnMultiplePublicKeys() {
            // given
            AuthHubAdapter.Jwk keyData1 =
                    new AuthHubAdapter.Jwk("key-1", "n1", "e1", "RSA", "sig", "RS256");

            AuthHubAdapter.Jwk keyData2 =
                    new AuthHubAdapter.Jwk("key-2", "n2", "e2", "RSA", "sig", "RS256");

            AuthHubAdapter.JwksResponse jwksResponse =
                    new AuthHubAdapter.JwksResponse(List.of(keyData1, keyData2));

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubAdapter.JwksResponse.class))
                    .willReturn(Mono.just(jwksResponse));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys()).expectNextCount(2).verifyComplete();
        }

        @Test
        @DisplayName("WebClient 호출 시 예외가 발생하면 AuthHubException으로 변환한다")
        @SuppressWarnings("unchecked")
        void shouldWrapExceptionIntoAuthHubException() {
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
            given(responseSpec.bodyToMono(AuthHubAdapter.JwksResponse.class))
                    .willReturn(Mono.error(originalException));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage())
                                        .contains("Failed to fetch JWKS from AuthHub");
                                assertThat(error.getCause()).isEqualTo(originalException);
                            })
                    .verify();
        }

        @Test
        @DisplayName("AuthHubException이 발생하면 그대로 전파한다")
        @SuppressWarnings("unchecked")
        void shouldPropagateAuthHubExceptionAsIs() {
            // given
            AuthHubAdapter.AuthHubException authHubException =
                    new AuthHubAdapter.AuthHubException("Original AuthHub error");

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec =
                    mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec =
                    mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(AuthHubAdapter.JwksResponse.class))
                    .willReturn(Mono.error(authHubException));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage()).isEqualTo("Original AuthHub error");
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("processResponse 메서드")
    class ProcessResponseTest {

        @Test
        @DisplayName("null 응답일 때 AuthHubException을 발생시킨다")
        void shouldThrowExceptionWhenResponseIsNull() {
            // when & then
            StepVerifier.create(adapter.processResponse(null))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage()).contains("Empty JWKS response");
                            })
                    .verify();
        }

        @Test
        @DisplayName("keys가 null일 때 AuthHubException을 발생시킨다")
        void shouldThrowExceptionWhenKeysIsNull() {
            // given
            AuthHubAdapter.JwksResponse response = new AuthHubAdapter.JwksResponse(null);

            // when & then
            StepVerifier.create(adapter.processResponse(response))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage()).contains("Empty JWKS response");
                            })
                    .verify();
        }

        @Test
        @DisplayName("keys가 빈 리스트일 때 AuthHubException을 발생시킨다")
        void shouldThrowExceptionWhenKeysIsEmpty() {
            // given
            AuthHubAdapter.JwksResponse response =
                    new AuthHubAdapter.JwksResponse(Collections.emptyList());

            // when & then
            StepVerifier.create(adapter.processResponse(response))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage()).contains("Empty JWKS response");
                            })
                    .verify();
        }

        @Test
        @DisplayName("정상 응답일 때 PublicKey를 변환하여 반환한다")
        void shouldReturnPublicKeysForValidResponse() {
            // given
            AuthHubAdapter.Jwk keyData =
                    new AuthHubAdapter.Jwk("test-kid", "test-n", "test-e", "RSA", "sig", "RS256");

            AuthHubAdapter.JwksResponse response =
                    new AuthHubAdapter.JwksResponse(List.of(keyData));

            // when & then
            StepVerifier.create(adapter.processResponse(response))
                    .assertNext(
                            publicKey -> {
                                assertThat(publicKey.kid()).isEqualTo("test-kid");
                                assertThat(publicKey.modulus()).isEqualTo("test-n");
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("fetchPublicKeysFallback 메서드")
    class FetchPublicKeysFallbackTest {

        @Test
        @DisplayName("Fallback 호출 시 AuthHubException을 반환한다")
        void shouldReturnAuthHubExceptionOnFallback() {
            // given
            RuntimeException cause = new RuntimeException("Original error");

            // when & then
            StepVerifier.create(adapter.fetchPublicKeysFallback(cause))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error)
                                        .isInstanceOf(AuthHubAdapter.AuthHubException.class);
                                assertThat(error.getMessage())
                                        .contains("AuthHub JWKS 호출 실패 (Fallback)");
                                assertThat(error.getCause()).isEqualTo(cause);
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("toPublicKey 메서드")
    class ToPublicKeyTest {

        @Test
        @DisplayName("Jwk를 PublicKey로 변환한다")
        void shouldConvertJwkToPublicKey() {
            // given
            AuthHubAdapter.Jwk keyData =
                    new AuthHubAdapter.Jwk("key-id", "modulus", "exponent", "RSA", "sig", "RS256");

            // when
            PublicKey result = adapter.toPublicKey(keyData);

            // then
            assertThat(result.kid()).isEqualTo("key-id");
            assertThat(result.modulus()).isEqualTo("modulus");
            assertThat(result.exponent()).isEqualTo("exponent");
            assertThat(result.kty()).isEqualTo("RSA");
            assertThat(result.use()).isEqualTo("sig");
            assertThat(result.alg()).isEqualTo("RS256");
        }
    }

    @Nested
    @DisplayName("AuthHubException 클래스")
    class AuthHubExceptionTest {

        @Test
        @DisplayName("메시지만 있는 생성자")
        void shouldCreateExceptionWithMessage() {
            // given & when
            AuthHubAdapter.AuthHubException exception =
                    new AuthHubAdapter.AuthHubException("Test message");

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
            AuthHubAdapter.AuthHubException exception =
                    new AuthHubAdapter.AuthHubException("Test message", cause);

            // then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("JwksResponse 레코드")
    class JwksResponseTest {

        @Test
        @DisplayName("keys를 올바르게 저장하고 반환한다")
        void shouldStoreAndReturnKeys() {
            // given
            AuthHubAdapter.Jwk keyData =
                    new AuthHubAdapter.Jwk("test-key", null, null, null, null, null);

            // when
            AuthHubAdapter.JwksResponse response =
                    new AuthHubAdapter.JwksResponse(List.of(keyData));

            // then
            assertThat(response.keys()).hasSize(1);
            assertThat(response.keys().get(0).kid()).isEqualTo("test-key");
        }

        @Test
        @DisplayName("null keys도 허용한다")
        void shouldAllowNullKeys() {
            // when
            AuthHubAdapter.JwksResponse response = new AuthHubAdapter.JwksResponse(null);

            // then
            assertThat(response.keys()).isNull();
        }
    }
}
