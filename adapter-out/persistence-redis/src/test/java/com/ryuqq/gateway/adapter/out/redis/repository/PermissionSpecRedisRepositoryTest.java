package com.ryuqq.gateway.adapter.out.redis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.EndpointPermissionEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PermissionSpecRedisRepository 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PermissionSpecRedisRepository 테스트")
class PermissionSpecRedisRepositoryTest {

    private ReactiveRedisTemplate<String, PermissionSpecEntity> reactiveRedisTemplate;
    private ReactiveValueOperations<String, PermissionSpecEntity> valueOperations;
    private PermissionSpecRedisRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        reactiveRedisTemplate = mock(ReactiveRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new PermissionSpecRedisRepository(reactiveRedisTemplate);
    }

    private PermissionSpecEntity createTestEntity() {
        EndpointPermissionEntity endpoint1 =
                new EndpointPermissionEntity(
                        "user-service",
                        "/api/v1/users",
                        "GET",
                        Set.of("read"),
                        Set.of("ROLE_USER"),
                        false);
        EndpointPermissionEntity endpoint2 =
                new EndpointPermissionEntity(
                        "order-service",
                        "/api/v1/orders",
                        "POST",
                        Set.of("write"),
                        Set.of("ROLE_ADMIN"),
                        false);

        return new PermissionSpecEntity(
                1L,
                Instant.now(),
                List.of(endpoint1, endpoint2));
    }

    @Nested
    @DisplayName("save 메서드 테스트")
    class SaveTest {

        @Test
        @DisplayName("TTL 포함하여 저장 성공")
        void shouldSaveWithTtl() {
            // given
            PermissionSpecEntity entity = createTestEntity();
            Duration ttl = Duration.ofMinutes(1);

            when(valueOperations.set(anyString(), any(PermissionSpecEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(entity, ttl)).verifyComplete();

            verify(valueOperations)
                    .set(eq("authhub:permission:spec"), eq(entity), eq(ttl));
        }

        @Test
        @DisplayName("기본 TTL(30초)로 저장 성공")
        void shouldSaveWithDefaultTtl() {
            // given
            PermissionSpecEntity entity = createTestEntity();

            when(valueOperations.set(anyString(), any(PermissionSpecEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(entity)).verifyComplete();

            verify(valueOperations)
                    .set(
                            eq("authhub:permission:spec"),
                            eq(entity),
                            eq(Duration.ofSeconds(30)));
        }
    }

    @Nested
    @DisplayName("find 메서드 테스트")
    class FindTest {

        @Test
        @DisplayName("존재하는 데이터 조회 성공")
        void shouldFindExistingData() {
            // given
            PermissionSpecEntity entity = createTestEntity();

            when(valueOperations.get("authhub:permission:spec")).thenReturn(Mono.just(entity));

            // when & then
            StepVerifier.create(repository.find())
                    .assertNext(
                            result -> {
                                assertThat(result.getVersion()).isEqualTo(1L);
                                assertThat(result.getPermissions()).hasSize(2);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 데이터 조회 시 empty 반환")
        void shouldReturnEmptyWhenNotFound() {
            // given
            when(valueOperations.get("authhub:permission:spec")).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(repository.find()).verifyComplete();
        }
    }

    @Nested
    @DisplayName("delete 메서드 테스트")
    class DeleteTest {

        @Test
        @DisplayName("삭제 성공")
        void shouldDeleteSuccessfully() {
            // given
            when(reactiveRedisTemplate.delete("authhub:permission:spec"))
                    .thenReturn(Mono.just(1L));

            // when & then
            StepVerifier.create(repository.delete()).verifyComplete();

            verify(reactiveRedisTemplate).delete("authhub:permission:spec");
        }

        @Test
        @DisplayName("존재하지 않는 키 삭제 시에도 정상 완료")
        void shouldCompleteWhenKeyNotExists() {
            // given
            when(reactiveRedisTemplate.delete("authhub:permission:spec"))
                    .thenReturn(Mono.just(0L));

            // when & then
            StepVerifier.create(repository.delete()).verifyComplete();
        }
    }
}
