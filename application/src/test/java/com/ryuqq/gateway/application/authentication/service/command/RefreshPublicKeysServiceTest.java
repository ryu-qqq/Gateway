package com.ryuqq.gateway.application.authentication.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * RefreshPublicKeysService 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshPublicKeysService 단위 테스트")
class RefreshPublicKeysServiceTest {

    @Mock private AuthHubClient authHubClient;

    @Mock private PublicKeyCommandPort publicKeyCommandPort;

    private RefreshPublicKeysService refreshPublicKeysService;

    @BeforeEach
    void setUp() {
        refreshPublicKeysService =
                new RefreshPublicKeysService(authHubClient, publicKeyCommandPort);
    }

    @Nested
    @DisplayName("Public Key 갱신 성공 시나리오")
    class RefreshSuccessTest {

        @Test
        @DisplayName("AuthHub에서 Public Keys 조회 후 Redis에 저장해야 한다")
        void shouldFetchAndSavePublicKeys() {
            // given
            PublicKey key1 = PublicKey.of("kid-1", "modulus1", "exponent1", "RSA", "sig", "RS256");
            PublicKey key2 = PublicKey.of("kid-2", "modulus2", "exponent2", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(key1, key2);

            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandPort.saveAll(publicKeys)).willReturn(Mono.empty());

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result).verifyComplete();

            then(authHubClient).should().fetchPublicKeys();
            then(publicKeyCommandPort).should().saveAll(publicKeys);
        }

        @Test
        @DisplayName("빈 Public Key 목록도 정상적으로 저장해야 한다")
        void shouldHandleEmptyPublicKeysList() {
            // given
            List<PublicKey> emptyList = List.of();

            given(authHubClient.fetchPublicKeys()).willReturn(Flux.empty());
            given(publicKeyCommandPort.saveAll(emptyList)).willReturn(Mono.empty());

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result).verifyComplete();

            then(authHubClient).should().fetchPublicKeys();
            then(publicKeyCommandPort).should().saveAll(emptyList);
        }
    }

    @Nested
    @DisplayName("Public Key 갱신 실패 시나리오")
    class RefreshFailureTest {

        @Test
        @DisplayName("AuthHub 호출 실패 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapAuthHubExceptionInRuntimeException() {
            // given
            given(authHubClient.fetchPublicKeys())
                    .willReturn(Flux.error(new RuntimeException("Network error")));

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .equals("Failed to refresh public keys"))
                    .verify();

            then(publicKeyCommandPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Redis 저장 실패 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapRedisSaveExceptionInRuntimeException() {
            // given
            PublicKey key1 = PublicKey.of("kid-1", "modulus1", "exponent1", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(key1);

            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandPort.saveAll(publicKeys))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .equals("Failed to refresh public keys"))
                    .verify();
        }
    }
}
