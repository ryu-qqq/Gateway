package com.ryuqq.gateway.adapter.out.authhub.client;

import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * AuthHub Adapter
 *
 * <p>AuthHubClient 구현체 (WebClient + Resilience4j)
 *
 * <p><strong>통신 대상</strong>:
 *
 * <ul>
 *   <li>AuthHub 외부 시스템
 *   <li>엔드포인트: {@code GET /api/v1/auth/jwks}
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 *   <li>Timeout: Connection 3초, Response 3초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubAdapter implements AuthHubClient {

    private final WebClient webClient;
    private final AuthHubProperties properties;

    public AuthHubAdapter(
            @Qualifier("authHubWebClient") WebClient webClient, AuthHubProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * JWKS 엔드포인트 호출
     *
     * @return Flux&lt;PublicKey&gt; Public Key 스트림
     */
    @Override
    @Retry(name = "authHub", fallbackMethod = "fetchPublicKeysFallback")
    @CircuitBreaker(name = "authHub", fallbackMethod = "fetchPublicKeysFallback")
    public Flux<PublicKey> fetchPublicKeys() {
        return webClient
                .get()
                .uri(properties.getJwksEndpoint())
                .retrieve()
                .bodyToMono(JwksResponse.class)
                .flatMapMany(this::processResponse)
                .onErrorMap(
                        e ->
                                !(e instanceof AuthHubException),
                        e -> new AuthHubException("Failed to fetch JWKS from AuthHub", e));
    }

    /**
     * JWKS 응답 처리
     *
     * @param response JWKS Response
     * @return Flux&lt;PublicKey&gt;
     */
    Flux<PublicKey> processResponse(JwksResponse response) {
        if (response == null || response.keys() == null || response.keys().isEmpty()) {
            return Flux.error(new AuthHubException("Empty JWKS response from AuthHub"));
        }
        return Flux.fromIterable(response.keys()).map(this::toPublicKey);
    }

    /**
     * Fallback 메서드 (Retry/Circuit Breaker 실패 시)
     *
     * @param throwable 예외
     * @return Flux.error
     */
    @SuppressWarnings("unused")
    Flux<PublicKey> fetchPublicKeysFallback(Throwable throwable) {
        return Flux.error(new AuthHubException("AuthHub JWKS 호출 실패 (Fallback)", throwable));
    }

    /**
     * JWKS Key → PublicKey 변환
     *
     * @param key JWKS Key
     * @return PublicKey
     */
    PublicKey toPublicKey(Map<String, Object> key) {
        return new PublicKey(
                (String) key.get("kid"),
                (String) key.get("n"),
                (String) key.get("e"),
                (String) key.get("kty"),
                (String) key.get("use"),
                (String) key.get("alg"));
    }

    /** JWKS Response DTO */
    record JwksResponse(List<Map<String, Object>> keys) {}

    /** AuthHub 예외 */
    public static class AuthHubException extends RuntimeException {
        public AuthHubException(String message) {
            super(message);
        }

        public AuthHubException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
