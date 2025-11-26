package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.PublicKeyMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PublicKeyRedisRepository;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
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
 * PublicKeyCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicKeyCommandAdapter 단위 테스트")
class PublicKeyCommandAdapterTest {

    @Mock private PublicKeyRedisRepository publicKeyRedisRepository;

    @Mock private PublicKeyMapper publicKeyMapper;

    private PublicKeyCommandAdapter publicKeyCommandAdapter;

    @BeforeEach
    void setUp() {
        publicKeyCommandAdapter =
                new PublicKeyCommandAdapter(publicKeyRedisRepository, publicKeyMapper);
    }

    @Nested
    @DisplayName("saveAll 메서드")
    class SaveAllTest {

        @Test
        @DisplayName("Public Key 목록을 Redis에 저장해야 한다")
        void shouldSaveAllPublicKeysToRedis() {
            // given
            PublicKey key1 = PublicKey.of("kid-1", "mod1", "exp1", "RSA", "sig", "RS256");
            PublicKey key2 = PublicKey.of("kid-2", "mod2", "exp2", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(key1, key2);

            PublicKeyEntity entity1 =
                    new PublicKeyEntity("kid-1", "mod1", "exp1", "RSA", "sig", "RS256");
            PublicKeyEntity entity2 =
                    new PublicKeyEntity("kid-2", "mod2", "exp2", "RSA", "sig", "RS256");

            given(publicKeyMapper.toPublicKeyEntity(key1)).willReturn(entity1);
            given(publicKeyMapper.toPublicKeyEntity(key2)).willReturn(entity2);
            given(publicKeyRedisRepository.save(eq("kid-1"), eq(entity1))).willReturn(Mono.empty());
            given(publicKeyRedisRepository.save(eq("kid-2"), eq(entity2))).willReturn(Mono.empty());

            // when
            Mono<Void> result = publicKeyCommandAdapter.saveAll(publicKeys);

            // then
            StepVerifier.create(result).verifyComplete();

            then(publicKeyMapper).should().toPublicKeyEntity(key1);
            then(publicKeyMapper).should().toPublicKeyEntity(key2);
            then(publicKeyRedisRepository).should().save("kid-1", entity1);
            then(publicKeyRedisRepository).should().save("kid-2", entity2);
        }

        @Test
        @DisplayName("빈 목록을 저장 시도 시 정상적으로 완료해야 한다")
        void shouldCompleteSuccessfullyForEmptyList() {
            // given
            List<PublicKey> emptyList = List.of();

            // when
            Mono<Void> result = publicKeyCommandAdapter.saveAll(emptyList);

            // then
            StepVerifier.create(result).verifyComplete();

            then(publicKeyMapper).shouldHaveNoInteractions();
            then(publicKeyRedisRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Redis 저장 에러 발생 시 RuntimeException으로 래핑해야 한다")
        void shouldWrapRedisExceptionInRuntimeException() {
            // given
            PublicKey key = PublicKey.of("kid-1", "mod", "exp", "RSA", "sig", "RS256");
            List<PublicKey> publicKeys = List.of(key);

            PublicKeyEntity entity =
                    new PublicKeyEntity("kid-1", "mod", "exp", "RSA", "sig", "RS256");

            given(publicKeyMapper.toPublicKeyEntity(key)).willReturn(entity);
            given(publicKeyRedisRepository.save(eq("kid-1"), eq(entity)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result = publicKeyCommandAdapter.saveAll(publicKeys);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains(
                                                            "Failed to save public keys to Redis"))
                    .verify();
        }
    }
}
