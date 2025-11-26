package com.ryuqq.gateway.application.authentication.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import com.ryuqq.gateway.application.authentication.port.out.query.PublicKeyQueryPort;
import com.ryuqq.gateway.domain.authentication.exception.PublicKeyNotFoundException;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * GetPublicKeyService 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPublicKeyService 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class GetPublicKeyServiceTest {

    @Mock private PublicKeyQueryPort publicKeyQueryPort;

    @Mock private AuthHubClient authHubClient;

    @Mock private PublicKeyCommandPort publicKeyCommandPort;

    private GetPublicKeyService getPublicKeyService;

    private static final String KID = "test-kid";

    @BeforeEach
    void setUp() {
        getPublicKeyService =
                new GetPublicKeyService(publicKeyQueryPort, authHubClient, publicKeyCommandPort);
        // Default mock: switchIfEmpty가 eager evaluation하므로 기본값 설정
        given(authHubClient.fetchPublicKeys()).willReturn(Flux.empty());
    }

    @Nested
    @DisplayName("Cache Hit 시나리오")
    class CacheHitTest {

        @Test
        @DisplayName("Redis Cache에 Public Key가 있으면 캐시에서 반환해야 한다")
        void shouldReturnFromCacheWhenPublicKeyExists() {
            // given
            PublicKey cachedKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");

            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.just(cachedKey));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            publicKey -> {
                                assertThat(publicKey.kid()).isEqualTo(KID);
                                assertThat(publicKey.modulus()).isEqualTo("modulus");
                            })
                    .verifyComplete();

            then(publicKeyQueryPort).should().findByKid(KID);
            // Note: switchIfEmpty가 eager evaluation하므로 fetchPublicKeys는 호출됨
            // 그러나 실제 AuthHub 결과는 사용되지 않음 (cache hit)
            then(publicKeyCommandPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Cache Miss + AuthHub Fallback 시나리오")
    class CacheMissFallbackTest {

        @Test
        @DisplayName("Cache Miss 시 AuthHub에서 조회하고 캐시에 저장해야 한다")
        void shouldFetchFromAuthHubAndCacheOnCacheMiss() {
            // given
            PublicKey targetKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            PublicKey otherKey =
                    PublicKey.of("other-kid", "modulus2", "exponent2", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(targetKey, otherKey);

            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandPort.saveAll(publicKeys)).willReturn(Mono.empty());

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            publicKey -> {
                                assertThat(publicKey.kid()).isEqualTo(KID);
                            })
                    .verifyComplete();

            then(publicKeyQueryPort).should().findByKid(KID);
            then(authHubClient).should().fetchPublicKeys();
            then(publicKeyCommandPort).should().saveAll(publicKeys);
        }

        @Test
        @DisplayName("AuthHub에도 해당 kid가 없으면 PublicKeyNotFoundException이 발생해야 한다")
        void shouldThrowPublicKeyNotFoundExceptionWhenKidNotFoundInAuthHub() {
            // given
            PublicKey otherKey =
                    PublicKey.of("other-kid", "modulus", "exponent", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(otherKey);

            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof PublicKeyNotFoundException
                                            && throwable.getMessage().contains(KID))
                    .verify();
        }
    }

    @Nested
    @DisplayName("에러 처리 시나리오")
    class ErrorHandlingTest {

        @Test
        @DisplayName("AuthHub 호출 실패 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapAuthHubExceptionInRuntimeException() {
            // given
            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys())
                    .willReturn(Flux.error(new RuntimeException("Network error")));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains("Failed to get public key for kid"))
                    .verify();
        }

        @Test
        @DisplayName("PublicKeyNotFoundException은 그대로 전파되어야 한다")
        void shouldPropagatePublicKeyNotFoundException() {
            // given
            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.empty());

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable -> throwable instanceof PublicKeyNotFoundException)
                    .verify();
        }

        @Test
        @DisplayName("Redis 저장 실패 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapRedisSaveExceptionInRuntimeException() {
            // given
            PublicKey targetKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(targetKey);

            given(publicKeyQueryPort.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandPort.saveAll(publicKeys))
                    .willReturn(Mono.error(new RuntimeException("Redis error")));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains("Failed to get public key for kid"))
                    .verify();
        }
    }
}
