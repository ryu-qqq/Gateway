package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionHashMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionHashRedisRepository;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionHashCommandAdapter 테스트")
class PermissionHashCommandAdapterTest {

    @Mock private PermissionHashRedisRepository permissionHashRedisRepository;

    @Mock private PermissionHashMapper permissionHashMapper;

    private PermissionHashCommandAdapter permissionHashCommandAdapter;

    @BeforeEach
    void setUp() {
        permissionHashCommandAdapter =
                new PermissionHashCommandAdapter(
                        permissionHashRedisRepository, permissionHashMapper);
    }

    @Nested
    @DisplayName("save() 테스트")
    class SaveTest {

        @Test
        @DisplayName("Permission Hash 저장 성공")
        void shouldSavePermissionHashSuccessfully() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            PermissionHash permissionHash = createPermissionHash();
            PermissionHashEntity entity = createPermissionHashEntity();

            when(permissionHashMapper.toEntity(permissionHash)).thenReturn(entity);
            when(permissionHashRedisRepository.save(tenantId, userId, entity))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(permissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity);
        }

        @Test
        @DisplayName("빈 권한과 역할이 있는 Permission Hash 저장")
        void shouldSavePermissionHashWithEmptyPermissionsAndRoles() {
            // given
            String tenantId = "tenant789";
            String userId = "user999";
            PermissionHash emptyPermissionHash = createEmptyPermissionHash();
            PermissionHashEntity emptyEntity = createEmptyPermissionHashEntity();

            when(permissionHashMapper.toEntity(emptyPermissionHash)).thenReturn(emptyEntity);
            when(permissionHashRedisRepository.save(tenantId, userId, emptyEntity))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(
                            permissionHashCommandAdapter.save(
                                    tenantId, userId, emptyPermissionHash))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(emptyPermissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, emptyEntity);
        }

        @Test
        @DisplayName("특수 문자가 포함된 tenantId와 userId로 저장")
        void shouldSaveWithSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-with-dash_123";
            String userId = "user@domain.com";
            PermissionHash permissionHash = createPermissionHash();
            PermissionHashEntity entity = createPermissionHashEntity();

            when(permissionHashMapper.toEntity(permissionHash)).thenReturn(entity);
            when(permissionHashRedisRepository.save(tenantId, userId, entity))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(permissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity);
        }

        @Test
        @DisplayName("긴 ID 값들로 저장")
        void shouldSaveWithLongIds() {
            // given
            String tenantId = "very-long-tenant-id-with-many-characters-1234567890";
            String userId = "very-long-user-id-with-many-characters-abcdefghijklmnopqrstuvwxyz";
            PermissionHash permissionHash = createPermissionHash();
            PermissionHashEntity entity = createPermissionHashEntity();

            when(permissionHashMapper.toEntity(permissionHash)).thenReturn(entity);
            when(permissionHashRedisRepository.save(tenantId, userId, entity))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(permissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity);
        }

        @Test
        @DisplayName("매퍼에서 에러 발생 시 예외 전파")
        void shouldPropagateMapperError() {
            // given
            String tenantId = "tenant111";
            String userId = "user222";
            PermissionHash permissionHash = createPermissionHash();
            RuntimeException mapperException = new RuntimeException("Mapping failed");

            when(permissionHashMapper.toEntity(permissionHash)).thenThrow(mapperException);

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashMapper).toEntity(permissionHash);
        }

        @Test
        @DisplayName("Redis 저장 중 에러 발생 시 예외 전파")
        void shouldPropagateRedisError() {
            // given
            String tenantId = "tenant333";
            String userId = "user444";
            PermissionHash permissionHash = createPermissionHash();
            PermissionHashEntity entity = createPermissionHashEntity();
            RuntimeException redisException = new RuntimeException("Redis save failed");

            when(permissionHashMapper.toEntity(permissionHash)).thenReturn(entity);
            when(permissionHashRedisRepository.save(tenantId, userId, entity))
                    .thenReturn(Mono.error(redisException));

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashMapper).toEntity(permissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity);
        }
    }

    @Nested
    @DisplayName("invalidate() 테스트")
    class InvalidateTest {

        @Test
        @DisplayName("Permission Hash 캐시 무효화 성공")
        void shouldInvalidatePermissionHashSuccessfully() {
            // given
            String tenantId = "tenant555";
            String userId = "user666";

            when(permissionHashRedisRepository.delete(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("특수 문자가 포함된 ID로 무효화")
        void shouldInvalidateWithSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-special_777";
            String userId = "user+special@test.com";

            when(permissionHashRedisRepository.delete(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("숫자로만 구성된 ID로 무효화")
        void shouldInvalidateWithNumericIds() {
            // given
            String tenantId = "123456789";
            String userId = "987654321";

            when(permissionHashRedisRepository.delete(tenantId, userId)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("Redis 삭제 중 에러 발생 시 예외 전파")
        void shouldPropagateRedisDeleteError() {
            // given
            String tenantId = "tenant888";
            String userId = "user999";
            RuntimeException redisException = new RuntimeException("Redis delete failed");

            when(permissionHashRedisRepository.delete(tenantId, userId))
                    .thenReturn(Mono.error(redisException));

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외 전파")
        void shouldPropagateRedisConnectionError() {
            // given
            String tenantId = "tenant000";
            String userId = "user111";
            RuntimeException connectionException = new RuntimeException("Redis connection failed");

            when(permissionHashRedisRepository.delete(tenantId, userId))
                    .thenReturn(Mono.error(connectionException));

            // when & then
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("저장 후 무효화 시나리오")
        void shouldSaveAndThenInvalidate() {
            // given
            String tenantId = "tenant222";
            String userId = "user333";
            PermissionHash permissionHash = createPermissionHash();
            PermissionHashEntity entity = createPermissionHashEntity();

            when(permissionHashMapper.toEntity(permissionHash)).thenReturn(entity);
            when(permissionHashRedisRepository.save(tenantId, userId, entity))
                    .thenReturn(Mono.empty());
            when(permissionHashRedisRepository.delete(tenantId, userId)).thenReturn(Mono.empty());

            // when & then - 저장
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, permissionHash))
                    .verifyComplete();

            // when & then - 무효화
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(permissionHash);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity);
            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("동일한 사용자의 여러 번 저장 후 무효화")
        void shouldSaveMultipleTimesAndInvalidate() {
            // given
            String tenantId = "tenant444";
            String userId = "user555";
            PermissionHash hash1 = createPermissionHash();
            PermissionHash hash2 = createPermissionHashWithDifferentPermissions();
            PermissionHashEntity entity1 = createPermissionHashEntity();
            PermissionHashEntity entity2 = createPermissionHashEntityWithDifferentPermissions();

            when(permissionHashMapper.toEntity(hash1)).thenReturn(entity1);
            when(permissionHashMapper.toEntity(hash2)).thenReturn(entity2);
            when(permissionHashRedisRepository.save(tenantId, userId, entity1))
                    .thenReturn(Mono.empty());
            when(permissionHashRedisRepository.save(tenantId, userId, entity2))
                    .thenReturn(Mono.empty());
            when(permissionHashRedisRepository.delete(tenantId, userId)).thenReturn(Mono.empty());

            // when & then - 첫 번째 저장
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, hash1))
                    .verifyComplete();

            // when & then - 두 번째 저장 (덮어쓰기)
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId, hash2))
                    .verifyComplete();

            // when & then - 무효화
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(hash1);
            verify(permissionHashMapper).toEntity(hash2);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity1);
            verify(permissionHashRedisRepository).save(tenantId, userId, entity2);
            verify(permissionHashRedisRepository).delete(tenantId, userId);
        }

        @Test
        @DisplayName("다른 사용자들의 독립적인 저장/무효화")
        void shouldHandleMultipleUsersIndependently() {
            // given
            String tenantId = "tenant666";
            String userId1 = "user777";
            String userId2 = "user888";
            PermissionHash hash1 = createPermissionHash();
            PermissionHash hash2 = createPermissionHashWithDifferentPermissions();
            PermissionHashEntity entity1 = createPermissionHashEntity();
            PermissionHashEntity entity2 = createPermissionHashEntityWithDifferentPermissions();

            when(permissionHashMapper.toEntity(hash1)).thenReturn(entity1);
            when(permissionHashMapper.toEntity(hash2)).thenReturn(entity2);
            when(permissionHashRedisRepository.save(tenantId, userId1, entity1))
                    .thenReturn(Mono.empty());
            when(permissionHashRedisRepository.save(tenantId, userId2, entity2))
                    .thenReturn(Mono.empty());
            when(permissionHashRedisRepository.delete(tenantId, userId1)).thenReturn(Mono.empty());

            // when & then - 첫 번째 사용자 저장
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId1, hash1))
                    .verifyComplete();

            // when & then - 두 번째 사용자 저장
            StepVerifier.create(permissionHashCommandAdapter.save(tenantId, userId2, hash2))
                    .verifyComplete();

            // when & then - 첫 번째 사용자만 무효화
            StepVerifier.create(permissionHashCommandAdapter.invalidate(tenantId, userId1))
                    .verifyComplete();

            verify(permissionHashMapper).toEntity(hash1);
            verify(permissionHashMapper).toEntity(hash2);
            verify(permissionHashRedisRepository).save(tenantId, userId1, entity1);
            verify(permissionHashRedisRepository).save(tenantId, userId2, entity2);
            verify(permissionHashRedisRepository).delete(tenantId, userId1);
        }
    }

    // Helper methods for creating test data
    private PermissionHash createPermissionHash() {
        return PermissionHash.of(
                "hash123abc",
                Set.of(Permission.of("user:read"), Permission.of("user:write")),
                Set.of("USER", "ADMIN"),
                Instant.parse("2025-11-25T08:00:00Z"));
    }

    private PermissionHashEntity createPermissionHashEntity() {
        return new PermissionHashEntity(
                "hash123abc",
                Set.of("user:read", "user:write"),
                Set.of("USER", "ADMIN"),
                Instant.parse("2025-11-25T08:00:00Z"));
    }

    private PermissionHash createEmptyPermissionHash() {
        return PermissionHash.of(
                "emptyHash456", Set.of(), Set.of(), Instant.parse("2025-11-25T09:00:00Z"));
    }

    private PermissionHashEntity createEmptyPermissionHashEntity() {
        return new PermissionHashEntity(
                "emptyHash456", Set.of(), Set.of(), Instant.parse("2025-11-25T09:00:00Z"));
    }

    private PermissionHash createPermissionHashWithDifferentPermissions() {
        return PermissionHash.of(
                "hash789def",
                Set.of(Permission.of("order:read"), Permission.of("order:create")),
                Set.of("ORDER_MANAGER"),
                Instant.parse("2025-11-25T10:00:00Z"));
    }

    private PermissionHashEntity createPermissionHashEntityWithDifferentPermissions() {
        return new PermissionHashEntity(
                "hash789def",
                Set.of("order:read", "order:create"),
                Set.of("ORDER_MANAGER"),
                Instant.parse("2025-11-25T10:00:00Z"));
    }
}
