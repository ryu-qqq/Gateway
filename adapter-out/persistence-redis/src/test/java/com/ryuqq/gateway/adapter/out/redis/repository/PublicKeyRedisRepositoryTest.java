package com.ryuqq.gateway.adapter.out.redis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PublicKeyRedisRepository 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PublicKeyRedisRepository 테스트")
class PublicKeyRedisRepositoryTest {

    private ReactiveRedisTemplate<String, PublicKeyEntity> reactiveRedisTemplate;
    private ReactiveValueOperations<String, PublicKeyEntity> valueOperations;
    private PublicKeyRedisRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        reactiveRedisTemplate = mock(ReactiveRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new PublicKeyRedisRepository(reactiveRedisTemplate);
    }

    private PublicKeyEntity createTestEntity(String kid) {
        return new PublicKeyEntity(
                kid, // kid
                "n-value-test-123", // modulus
                "AQAB", // exponent
                "RSA", // kty
                "sig", // use
                "RS256"); // alg
    }

    @Nested
    @DisplayName("save 메서드 테스트")
    class SaveTest {

        @Test
        @DisplayName("TTL 포함하여 저장 성공")
        void shouldSaveWithTtl() {
            // given
            String kid = "test-kid-001";
            PublicKeyEntity entity = createTestEntity(kid);
            Duration ttl = Duration.ofMinutes(30);

            when(valueOperations.set(anyString(), any(PublicKeyEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(kid, entity, ttl)).verifyComplete();

            verify(valueOperations)
                    .set(eq("authhub:jwt:publickey:" + kid), eq(entity), eq(ttl));
        }

        @Test
        @DisplayName("기본 TTL(1시간)로 저장 성공")
        void shouldSaveWithDefaultTtl() {
            // given
            String kid = "test-kid-002";
            PublicKeyEntity entity = createTestEntity(kid);

            when(valueOperations.set(anyString(), any(PublicKeyEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(kid, entity)).verifyComplete();

            verify(valueOperations)
                    .set(
                            eq("authhub:jwt:publickey:" + kid),
                            eq(entity),
                            eq(Duration.ofHours(1)));
        }
    }

    @Nested
    @DisplayName("findByKid 메서드 테스트")
    class FindByKidTest {

        @Test
        @DisplayName("존재하는 키 조회 성공")
        void shouldFindExistingKey() {
            // given
            String kid = "existing-kid";
            PublicKeyEntity entity = createTestEntity(kid);

            when(valueOperations.get("authhub:jwt:publickey:" + kid))
                    .thenReturn(Mono.just(entity));

            // when & then
            StepVerifier.create(repository.findByKid(kid))
                    .assertNext(
                            result -> {
                                assertThat(result.getKid()).isEqualTo(kid);
                                assertThat(result.getKty()).isEqualTo("RSA");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 키 조회 시 empty 반환")
        void shouldReturnEmptyForNonExistingKey() {
            // given
            String kid = "non-existing-kid";

            when(valueOperations.get("authhub:jwt:publickey:" + kid)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(repository.findByKid(kid)).verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteAll 메서드 테스트")
    class DeleteAllTest {

        @Test
        @DisplayName("모든 키 삭제 성공")
        void shouldDeleteAllKeys() {
            // given
            when(reactiveRedisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(
                            Flux.just(
                                    "authhub:jwt:publickey:kid1",
                                    "authhub:jwt:publickey:kid2",
                                    "authhub:jwt:publickey:kid3"));
            when(reactiveRedisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

            // when & then
            StepVerifier.create(repository.deleteAll()).verifyComplete();

            verify(reactiveRedisTemplate).scan(any(ScanOptions.class));
        }

        @Test
        @DisplayName("삭제할 키가 없을 때도 정상 완료")
        void shouldCompleteWhenNoKeysToDelete() {
            // given
            when(reactiveRedisTemplate.scan(any(ScanOptions.class))).thenReturn(Flux.empty());

            // when & then
            StepVerifier.create(repository.deleteAll()).verifyComplete();
        }
    }
}
