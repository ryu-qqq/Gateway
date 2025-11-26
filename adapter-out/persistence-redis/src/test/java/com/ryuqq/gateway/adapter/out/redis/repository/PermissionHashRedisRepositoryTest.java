package com.ryuqq.gateway.adapter.out.redis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
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
 * PermissionHashRedisRepository 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PermissionHashRedisRepository 테스트")
class PermissionHashRedisRepositoryTest {

    private ReactiveRedisTemplate<String, PermissionHashEntity> reactiveRedisTemplate;
    private ReactiveValueOperations<String, PermissionHashEntity> valueOperations;
    private PermissionHashRedisRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        reactiveRedisTemplate = mock(ReactiveRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new PermissionHashRedisRepository(reactiveRedisTemplate);
    }

    private PermissionHashEntity createTestEntity() {
        return new PermissionHashEntity(
                "hash-value-123",
                Set.of("read", "write"),
                Set.of("ROLE_USER", "ROLE_ADMIN"),
                Instant.now().plusSeconds(60));
    }

    @Nested
    @DisplayName("save 메서드 테스트")
    class SaveTest {

        @Test
        @DisplayName("TTL 포함하여 저장 성공")
        void shouldSaveWithTtl() {
            // given
            String tenantId = "tenant-001";
            String userId = "user-001";
            PermissionHashEntity entity = createTestEntity();
            Duration ttl = Duration.ofMinutes(5);

            when(valueOperations.set(anyString(), any(PermissionHashEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(tenantId, userId, entity, ttl)).verifyComplete();

            verify(valueOperations)
                    .set(
                            eq("authhub:permission:hash:" + tenantId + ":" + userId),
                            eq(entity),
                            eq(ttl));
        }

        @Test
        @DisplayName("기본 TTL(30초)로 저장 성공")
        void shouldSaveWithDefaultTtl() {
            // given
            String tenantId = "tenant-002";
            String userId = "user-002";
            PermissionHashEntity entity = createTestEntity();

            when(valueOperations.set(anyString(), any(PermissionHashEntity.class), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.save(tenantId, userId, entity)).verifyComplete();

            verify(valueOperations)
                    .set(
                            eq("authhub:permission:hash:" + tenantId + ":" + userId),
                            eq(entity),
                            eq(Duration.ofSeconds(30)));
        }
    }

    @Nested
    @DisplayName("findByTenantAndUser 메서드 테스트")
    class FindByTenantAndUserTest {

        @Test
        @DisplayName("존재하는 데이터 조회 성공")
        void shouldFindExistingData() {
            // given
            String tenantId = "tenant-001";
            String userId = "user-001";
            PermissionHashEntity entity = createTestEntity();

            when(valueOperations.get("authhub:permission:hash:" + tenantId + ":" + userId))
                    .thenReturn(Mono.just(entity));

            // when & then
            StepVerifier.create(repository.findByTenantAndUser(tenantId, userId))
                    .assertNext(
                            result -> {
                                assertThat(result.getHash()).isEqualTo("hash-value-123");
                                assertThat(result.getPermissions()).containsExactlyInAnyOrder("read", "write");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 데이터 조회 시 empty 반환")
        void shouldReturnEmptyWhenNotFound() {
            // given
            String tenantId = "non-existing-tenant";
            String userId = "non-existing-user";

            when(valueOperations.get("authhub:permission:hash:" + tenantId + ":" + userId))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(repository.findByTenantAndUser(tenantId, userId)).verifyComplete();
        }
    }

    @Nested
    @DisplayName("delete 메서드 테스트")
    class DeleteTest {

        @Test
        @DisplayName("삭제 성공")
        void shouldDeleteSuccessfully() {
            // given
            String tenantId = "tenant-001";
            String userId = "user-001";

            when(reactiveRedisTemplate.delete(
                            "authhub:permission:hash:" + tenantId + ":" + userId))
                    .thenReturn(Mono.just(1L));

            // when & then
            StepVerifier.create(repository.delete(tenantId, userId)).verifyComplete();

            verify(reactiveRedisTemplate)
                    .delete("authhub:permission:hash:" + tenantId + ":" + userId);
        }

        @Test
        @DisplayName("존재하지 않는 키 삭제 시에도 정상 완료")
        void shouldCompleteWhenKeyNotExists() {
            // given
            String tenantId = "non-existing";
            String userId = "non-existing";

            when(reactiveRedisTemplate.delete(
                            "authhub:permission:hash:" + tenantId + ":" + userId))
                    .thenReturn(Mono.just(0L));

            // when & then
            StepVerifier.create(repository.delete(tenantId, userId)).verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteByTenant 메서드 테스트")
    class DeleteByTenantTest {

        @Test
        @DisplayName("테넌트별 모든 키 삭제 성공")
        void shouldDeleteAllKeysByTenant() {
            // given
            String tenantId = "tenant-001";

            when(reactiveRedisTemplate.scan(any(ScanOptions.class)))
                    .thenReturn(
                            Flux.just(
                                    "authhub:permission:hash:" + tenantId + ":user1",
                                    "authhub:permission:hash:" + tenantId + ":user2",
                                    "authhub:permission:hash:" + tenantId + ":user3"));
            when(reactiveRedisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

            // when & then
            StepVerifier.create(repository.deleteByTenant(tenantId)).verifyComplete();

            verify(reactiveRedisTemplate).scan(any(ScanOptions.class));
        }

        @Test
        @DisplayName("삭제할 키가 없을 때도 정상 완료")
        void shouldCompleteWhenNoKeysToDelete() {
            // given
            String tenantId = "empty-tenant";

            when(reactiveRedisTemplate.scan(any(ScanOptions.class))).thenReturn(Flux.empty());

            // when & then
            StepVerifier.create(repository.deleteByTenant(tenantId)).verifyComplete();
        }
    }
}
