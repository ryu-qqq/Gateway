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
@DisplayName("PermissionHashQueryAdapter 테스트")
class PermissionHashQueryAdapterTest {

    @Mock private PermissionHashRedisRepository permissionHashRedisRepository;

    @Mock private PermissionHashMapper permissionHashMapper;

    private PermissionHashQueryAdapter permissionHashQueryAdapter;

    @BeforeEach
    void setUp() {
        permissionHashQueryAdapter =
                new PermissionHashQueryAdapter(permissionHashRedisRepository, permissionHashMapper);
    }

    @Nested
    @DisplayName("findByTenantAndUser() 테스트")
    class FindByTenantAndUserTest {

        @Test
        @DisplayName("Redis에서 Permission Hash 조회 성공")
        void shouldFindPermissionHashSuccessfully() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";
            PermissionHashEntity entity = createPermissionHashEntity();
            PermissionHash expectedHash = createPermissionHash();

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenReturn(expectedHash);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectNext(expectedHash)
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }

        @Test
        @DisplayName("Redis에서 Permission Hash가 없을 때 empty Mono 반환")
        void shouldReturnEmptyMonoWhenHashNotFound() {
            // given
            String tenantId = "tenant789";
            String userId = "user999";

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
        }

        @Test
        @DisplayName("Redis 조회 중 에러 발생 시 RuntimeException으로 변환")
        void shouldMapErrorToRuntimeException() {
            // given
            String tenantId = "tenant111";
            String userId = "user222";
            RuntimeException originalException = new RuntimeException("Redis connection failed");

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.error(originalException));

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains(
                                                            "Failed to get permission hash from"
                                                                    + " Redis")
                                            && throwable.getMessage().contains("tenantId=tenant111")
                                            && throwable.getMessage().contains("userId=user222")
                                            && throwable.getCause() == originalException)
                    .verify();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
        }

        @Test
        @DisplayName("매퍼에서 에러 발생 시 RuntimeException으로 변환")
        void shouldMapMapperErrorToRuntimeException() {
            // given
            String tenantId = "tenant333";
            String userId = "user444";
            PermissionHashEntity entity = createPermissionHashEntity();
            RuntimeException mapperException = new RuntimeException("Mapping failed");

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenThrow(mapperException);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains(
                                                            "Failed to get permission hash from"
                                                                    + " Redis")
                                            && throwable.getMessage().contains("tenantId=tenant333")
                                            && throwable.getMessage().contains("userId=user444")
                                            && throwable.getCause() == mapperException)
                    .verify();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }

        @Test
        @DisplayName("특수 문자가 포함된 tenantId와 userId로 조회")
        void shouldHandleSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-with-dash_123";
            String userId = "user@domain.com";
            PermissionHashEntity entity = createPermissionHashEntity();
            PermissionHash expectedHash = createPermissionHash();

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenReturn(expectedHash);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectNext(expectedHash)
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }

        @Test
        @DisplayName("긴 ID 값들로 조회")
        void shouldHandleLongIds() {
            // given
            String tenantId = "very-long-tenant-id-with-many-characters-1234567890";
            String userId = "very-long-user-id-with-many-characters-abcdefghijklmnopqrstuvwxyz";
            PermissionHashEntity entity = createPermissionHashEntity();
            PermissionHash expectedHash = createPermissionHash();

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenReturn(expectedHash);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectNext(expectedHash)
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }

        @Test
        @DisplayName("숫자로만 구성된 ID로 조회")
        void shouldHandleNumericIds() {
            // given
            String tenantId = "123456789";
            String userId = "987654321";
            PermissionHashEntity entity = createPermissionHashEntity();
            PermissionHash expectedHash = createPermissionHash();

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenReturn(expectedHash);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectNext(expectedHash)
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }

        @Test
        @DisplayName("빈 권한과 역할이 있는 Permission Hash 조회")
        void shouldFindPermissionHashWithEmptyPermissionsAndRoles() {
            // given
            String tenantId = "tenant555";
            String userId = "user666";
            PermissionHashEntity entity = createEmptyPermissionHashEntity();
            PermissionHash expectedHash = createEmptyPermissionHash();

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.just(entity));
            when(permissionHashMapper.toPermissionHash(entity)).thenReturn(expectedHash);

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectNext(expectedHash)
                    .verifyComplete();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
            verify(permissionHashMapper).toPermissionHash(entity);
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Redis 연결 타임아웃 에러 처리")
        void shouldHandleRedisTimeoutError() {
            // given
            String tenantId = "tenant777";
            String userId = "user888";
            RuntimeException timeoutException = new RuntimeException("Connection timeout");

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.error(timeoutException));

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .contains(
                                                            "Failed to get permission hash from"
                                                                    + " Redis"))
                    .verify();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
        }

        @Test
        @DisplayName("Redis 직렬화 에러 처리")
        void shouldHandleRedisSerializationError() {
            // given
            String tenantId = "tenant999";
            String userId = "user000";
            RuntimeException serializationException = new RuntimeException("Serialization error");

            when(permissionHashRedisRepository.findByTenantAndUser(tenantId, userId))
                    .thenReturn(Mono.error(serializationException));

            // when & then
            StepVerifier.create(permissionHashQueryAdapter.findByTenantAndUser(tenantId, userId))
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable.getCause() == serializationException)
                    .verify();

            verify(permissionHashRedisRepository).findByTenantAndUser(tenantId, userId);
        }
    }

    // Helper methods for creating test data
    private PermissionHashEntity createPermissionHashEntity() {
        return new PermissionHashEntity(
                "hash123abc",
                Set.of("user:read", "user:write"),
                Set.of("USER", "ADMIN"),
                Instant.parse("2025-11-25T08:00:00Z"));
    }

    private PermissionHash createPermissionHash() {
        return PermissionHash.of(
                "hash123abc",
                Set.of(Permission.of("user:read"), Permission.of("user:write")),
                Set.of("USER", "ADMIN"),
                Instant.parse("2025-11-25T08:00:00Z"));
    }

    private PermissionHashEntity createEmptyPermissionHashEntity() {
        return new PermissionHashEntity(
                "emptyHash456", Set.of(), Set.of(), Instant.parse("2025-11-25T09:00:00Z"));
    }

    private PermissionHash createEmptyPermissionHash() {
        return PermissionHash.of(
                "emptyHash456", Set.of(), Set.of(), Instant.parse("2025-11-25T09:00:00Z"));
    }
}
