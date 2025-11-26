package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.PublicKeyMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PublicKeyRedisRepository;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PublicKeyQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicKeyQueryAdapter 단위 테스트")
class PublicKeyQueryAdapterTest {

    @Mock private PublicKeyRedisRepository publicKeyRedisRepository;

    @Mock private PublicKeyMapper publicKeyMapper;

    private PublicKeyQueryAdapter publicKeyQueryAdapter;

    private static final String KID = "test-kid";

    @BeforeEach
    void setUp() {
        publicKeyQueryAdapter =
                new PublicKeyQueryAdapter(publicKeyRedisRepository, publicKeyMapper);
    }

    @Nested
    @DisplayName("findByKid 메서드")
    class FindByKidTest {

        @Test
        @DisplayName("캐시에 Public Key가 있으면 반환해야 한다")
        void shouldReturnPublicKeyWhenCacheHit() {
            // given
            PublicKeyEntity entity =
                    new PublicKeyEntity(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            PublicKey publicKey =
                    PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");

            given(publicKeyRedisRepository.findByKid(KID)).willReturn(Mono.just(entity));
            given(publicKeyMapper.toPublicKey(entity)).willReturn(publicKey);

            // when
            Mono<PublicKey> result = publicKeyQueryAdapter.findByKid(KID);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            key -> {
                                assertThat(key.kid()).isEqualTo(KID);
                                assertThat(key.modulus()).isEqualTo("modulus");
                            })
                    .verifyComplete();

            then(publicKeyRedisRepository).should().findByKid(KID);
            then(publicKeyMapper).should().toPublicKey(entity);
        }

        @Test
        @DisplayName("캐시에 Public Key가 없으면 empty Mono를 반환해야 한다")
        void shouldReturnEmptyWhenCacheMiss() {
            // given
            given(publicKeyRedisRepository.findByKid(KID)).willReturn(Mono.empty());

            // when
            Mono<PublicKey> result = publicKeyQueryAdapter.findByKid(KID);

            // then
            StepVerifier.create(result).verifyComplete();

            then(publicKeyRedisRepository).should().findByKid(KID);
            then(publicKeyMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapRedisExceptionInRuntimeException() {
            // given
            given(publicKeyRedisRepository.findByKid(KID))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<PublicKey> result = publicKeyQueryAdapter.findByKid(KID);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains("Failed to get public key from Redis"))
                    .verify();
        }
    }
}
