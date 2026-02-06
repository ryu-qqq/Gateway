package com.ryuqq.gateway.application.authentication.service.command;

import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.internal.PublicKeyCacheCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Mock private PublicKeyCacheCoordinator publicKeyCacheCoordinator;

    @InjectMocks private RefreshPublicKeysService refreshPublicKeysService;

    @Nested
    @DisplayName("Public Key 갱신")
    class RefreshKeysTest {

        @Test
        @DisplayName("정상적으로 Public Keys를 갱신해야 한다")
        void shouldRefreshPublicKeysSuccessfully() {
            // given
            given(publicKeyCacheCoordinator.refreshAllKeys()).willReturn(Mono.empty());

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result).verifyComplete();

            then(publicKeyCacheCoordinator).should().refreshAllKeys();
        }

        @Test
        @DisplayName("갱신 실패 시 에러가 전파되어야 한다")
        void shouldPropagateErrorWhenRefreshFails() {
            // given
            given(publicKeyCacheCoordinator.refreshAllKeys())
                    .willReturn(Mono.error(new RuntimeException("Failed to refresh public keys")));

            // when
            Mono<Void> result = refreshPublicKeysService.execute();

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            e ->
                                    e instanceof RuntimeException
                                            && e.getMessage()
                                                    .equals("Failed to refresh public keys"))
                    .verify();

            then(publicKeyCacheCoordinator).should().refreshAllKeys();
        }
    }
}
