package com.ryuqq.gateway.application.authentication.internal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.manager.PublicKeyCommandManager;
import com.ryuqq.gateway.application.authentication.manager.PublicKeyQueryManager;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.PublicKeyNotFoundException;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.fixture.authentication.AuthenticationFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PublicKeyCacheCoordinator 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PublicKeyCacheCoordinator 단위 테스트")
class PublicKeyCacheCoordinatorTest {

    @Mock private PublicKeyQueryManager publicKeyQueryManager;

    @Mock private PublicKeyCommandManager publicKeyCommandManager;

    @Mock private AuthHubClient authHubClient;

    @InjectMocks private PublicKeyCacheCoordinator publicKeyCacheCoordinator;

    private static final String KID = AuthenticationFixture.DEFAULT_KID;

    @Nested
    @DisplayName("Cache Hit 시나리오")
    class CacheHitTest {

        @Test
        @DisplayName("Cache에 Public Key가 있으면 캐시에서 반환해야 한다")
        void shouldReturnFromCacheWhenPublicKeyExists() {
            // given
            PublicKey cachedKey = AuthenticationFixture.aPublicKey();

            // switchIfEmpty가 eager evaluation하므로 기본값 설정
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.empty());
            given(publicKeyQueryManager.findByKid(KID)).willReturn(Mono.just(cachedKey));

            // when
            Mono<PublicKey> result = publicKeyCacheCoordinator.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            publicKey -> {
                                assertThat(publicKey.kid()).isEqualTo(KID);
                            })
                    .verifyComplete();

            then(publicKeyQueryManager).should().findByKid(KID);
            then(publicKeyCommandManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Cache Miss + AuthHub Fallback 시나리오")
    class CacheMissFallbackTest {

        @Test
        @DisplayName("Cache Miss 시 AuthHub에서 조회하고 캐시에 저장해야 한다")
        void shouldFetchFromAuthHubAndCacheOnCacheMiss() {
            // given
            PublicKey targetKey = AuthenticationFixture.aPublicKey(KID);
            PublicKey otherKey = AuthenticationFixture.aPublicKey("other-kid");
            List<PublicKey> publicKeys = List.of(targetKey, otherKey);

            given(publicKeyQueryManager.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandManager.saveAll(publicKeys)).willReturn(Mono.empty());

            // when
            Mono<PublicKey> result = publicKeyCacheCoordinator.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(publicKey -> assertThat(publicKey.kid()).isEqualTo(KID))
                    .verifyComplete();

            then(publicKeyQueryManager).should().findByKid(KID);
            then(authHubClient).should().fetchPublicKeys();
            then(publicKeyCommandManager).should().saveAll(publicKeys);
        }

        @Test
        @DisplayName("AuthHub에도 해당 kid가 없으면 PublicKeyNotFoundException 발생")
        void shouldThrowExceptionWhenKidNotFoundInAuthHub() {
            // given
            PublicKey otherKey = AuthenticationFixture.aPublicKey("other-kid");
            List<PublicKey> publicKeys = List.of(otherKey);

            given(publicKeyQueryManager.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));

            // when
            Mono<PublicKey> result = publicKeyCacheCoordinator.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(PublicKeyNotFoundException.class);
                                assertThat(error.getMessage()).contains(KID);
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("Cache 전체 갱신")
    class RefreshAllKeysTest {

        @Test
        @DisplayName("AuthHub에서 Public Keys 조회 후 캐시에 저장해야 한다")
        void shouldFetchAndSaveAllPublicKeys() {
            // given
            PublicKey key1 = AuthenticationFixture.aPublicKey("kid-1");
            PublicKey key2 = AuthenticationFixture.aPublicKey("kid-2");
            List<PublicKey> publicKeys = List.of(key1, key2);

            given(authHubClient.fetchPublicKeys()).willReturn(Flux.fromIterable(publicKeys));
            given(publicKeyCommandManager.saveAll(publicKeys)).willReturn(Mono.empty());

            // when
            Mono<Void> result = publicKeyCacheCoordinator.refreshAllKeys();

            // then
            StepVerifier.create(result).verifyComplete();

            then(authHubClient).should().fetchPublicKeys();
            then(publicKeyCommandManager).should().saveAll(publicKeys);
        }

        @Test
        @DisplayName("AuthHub 호출 실패 시 RuntimeException으로 래핑")
        void shouldWrapAuthHubExceptionInRuntimeException() {
            // given
            given(authHubClient.fetchPublicKeys())
                    .willReturn(Flux.error(new RuntimeException("Network error")));

            // when
            Mono<Void> result = publicKeyCacheCoordinator.refreshAllKeys();

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(RuntimeException.class);
                                assertThat(error.getMessage())
                                        .contains("Failed to refresh public keys");
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandlingTest {

        @Test
        @DisplayName("AuthHub 호출 실패 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapAuthHubExceptionInRuntimeException() {
            // given
            given(publicKeyQueryManager.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys())
                    .willReturn(Flux.error(new RuntimeException("Network error")));

            // when
            Mono<PublicKey> result = publicKeyCacheCoordinator.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(RuntimeException.class);
                                assertThat(error.getMessage())
                                        .contains("Failed to get public key for kid");
                            })
                    .verify();
        }

        @Test
        @DisplayName("PublicKeyNotFoundException은 그대로 전파되어야 한다")
        void shouldPropagatePublicKeyNotFoundException() {
            // given
            given(publicKeyQueryManager.findByKid(KID)).willReturn(Mono.empty());
            given(authHubClient.fetchPublicKeys()).willReturn(Flux.empty());

            // when
            Mono<PublicKey> result = publicKeyCacheCoordinator.getPublicKey(KID);

            // then
            StepVerifier.create(result).expectError(PublicKeyNotFoundException.class).verify();
        }
    }
}
