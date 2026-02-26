package com.ryuqq.gateway.application.authentication.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.internal.PublicKeyCacheCoordinator;
import com.ryuqq.gateway.domain.authentication.exception.PublicKeyNotFoundException;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
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
 * GetPublicKeyService 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPublicKeyService 단위 테스트")
class GetPublicKeyServiceTest {

    @Mock private PublicKeyCacheCoordinator publicKeyCacheCoordinator;

    @InjectMocks private GetPublicKeyService getPublicKeyService;

    private static final String KID = "test-kid";

    @Nested
    @DisplayName("Public Key 조회")
    class GetPublicKeyTest {

        @Test
        @DisplayName("정상적으로 Public Key를 조회해야 한다")
        void shouldGetPublicKeySuccessfully() {
            // given
            PublicKey publicKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            given(publicKeyCacheCoordinator.getPublicKey(KID)).willReturn(Mono.just(publicKey));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            key -> {
                                assertThat(key.kid()).isEqualTo(KID);
                                assertThat(key.modulus()).isEqualTo("modulus");
                            })
                    .verifyComplete();

            then(publicKeyCacheCoordinator).should().getPublicKey(KID);
        }

        @Test
        @DisplayName("Public Key가 없으면 PublicKeyNotFoundException이 발생해야 한다")
        void shouldThrowExceptionWhenKeyNotFound() {
            // given
            given(publicKeyCacheCoordinator.getPublicKey(KID))
                    .willReturn(Mono.error(new PublicKeyNotFoundException(KID)));

            // when
            Mono<PublicKey> result = getPublicKeyService.getPublicKey(KID);

            // then
            StepVerifier.create(result).expectError(PublicKeyNotFoundException.class).verify();

            then(publicKeyCacheCoordinator).should().getPublicKey(KID);
        }
    }
}
