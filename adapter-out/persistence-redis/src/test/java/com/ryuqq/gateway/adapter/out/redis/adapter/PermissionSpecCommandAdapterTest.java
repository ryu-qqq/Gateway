package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionSpecMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionSpecRedisRepository;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.time.Instant;
import java.util.List;
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
@DisplayName("PermissionSpecCommandAdapter 테스트")
class PermissionSpecCommandAdapterTest {

    @Mock private PermissionSpecRedisRepository permissionSpecRedisRepository;

    @Mock private PermissionSpecMapper permissionSpecMapper;

    private PermissionSpecCommandAdapter permissionSpecCommandAdapter;

    @BeforeEach
    void setUp() {
        permissionSpecCommandAdapter =
                new PermissionSpecCommandAdapter(
                        permissionSpecRedisRepository, permissionSpecMapper);
    }

    @Nested
    @DisplayName("save() 테스트")
    class SaveTest {

        @Test
        @DisplayName("Permission Spec 저장 성공")
        void shouldSavePermissionSpecSuccessfully() {
            // given
            PermissionSpec permissionSpec = createPermissionSpec();
            PermissionSpecEntity entity = createPermissionSpecEntity();

            when(permissionSpecMapper.toEntity(permissionSpec)).thenReturn(entity);
            when(permissionSpecRedisRepository.save(entity)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.save(permissionSpec)).verifyComplete();

            verify(permissionSpecMapper).toEntity(permissionSpec);
            verify(permissionSpecRedisRepository).save(entity);
        }

        @Test
        @DisplayName("빈 권한 리스트가 있는 Permission Spec 저장")
        void shouldSavePermissionSpecWithEmptyPermissions() {
            // given
            PermissionSpec emptyPermissionSpec = createEmptyPermissionSpec();
            PermissionSpecEntity emptyEntity = createEmptyPermissionSpecEntity();

            when(permissionSpecMapper.toEntity(emptyPermissionSpec)).thenReturn(emptyEntity);
            when(permissionSpecRedisRepository.save(emptyEntity)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.save(emptyPermissionSpec))
                    .verifyComplete();

            verify(permissionSpecMapper).toEntity(emptyPermissionSpec);
            verify(permissionSpecRedisRepository).save(emptyEntity);
        }

        @Test
        @DisplayName("복수의 엔드포인트가 있는 Permission Spec 저장")
        void shouldSavePermissionSpecWithMultipleEndpoints() {
            // given
            PermissionSpec multipleEndpointsSpec = createPermissionSpecWithMultipleEndpoints();
            PermissionSpecEntity multipleEndpointsEntity =
                    createPermissionSpecEntityWithMultipleEndpoints();

            when(permissionSpecMapper.toEntity(multipleEndpointsSpec))
                    .thenReturn(multipleEndpointsEntity);
            when(permissionSpecRedisRepository.save(multipleEndpointsEntity))
                    .thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.save(multipleEndpointsSpec))
                    .verifyComplete();

            verify(permissionSpecMapper).toEntity(multipleEndpointsSpec);
            verify(permissionSpecRedisRepository).save(multipleEndpointsEntity);
        }

        @Test
        @DisplayName("매퍼에서 에러 발생 시 예외 전파")
        void shouldPropagateMapperError() {
            // given
            PermissionSpec permissionSpec = createPermissionSpec();
            RuntimeException mapperException = new RuntimeException("Mapping failed");

            when(permissionSpecMapper.toEntity(permissionSpec)).thenThrow(mapperException);

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.save(permissionSpec))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecMapper).toEntity(permissionSpec);
        }

        @Test
        @DisplayName("Redis 저장 중 에러 발생 시 예외 전파")
        void shouldPropagateRedisError() {
            // given
            PermissionSpec permissionSpec = createPermissionSpec();
            PermissionSpecEntity entity = createPermissionSpecEntity();
            RuntimeException redisException = new RuntimeException("Redis save failed");

            when(permissionSpecMapper.toEntity(permissionSpec)).thenReturn(entity);
            when(permissionSpecRedisRepository.save(entity)).thenReturn(Mono.error(redisException));

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.save(permissionSpec))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecMapper).toEntity(permissionSpec);
            verify(permissionSpecRedisRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("invalidate() 테스트")
    class InvalidateTest {

        @Test
        @DisplayName("Permission Spec 캐시 무효화 성공")
        void shouldInvalidatePermissionSpecSuccessfully() {
            // given
            when(permissionSpecRedisRepository.delete()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.invalidate()).verifyComplete();

            verify(permissionSpecRedisRepository).delete();
        }

        @Test
        @DisplayName("Redis 삭제 중 에러 발생 시 예외 전파")
        void shouldPropagateRedisDeleteError() {
            // given
            RuntimeException redisException = new RuntimeException("Redis delete failed");
            when(permissionSpecRedisRepository.delete()).thenReturn(Mono.error(redisException));

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.invalidate())
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecRedisRepository).delete();
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외 전파")
        void shouldPropagateRedisConnectionError() {
            // given
            RuntimeException connectionException = new RuntimeException("Redis connection failed");
            when(permissionSpecRedisRepository.delete())
                    .thenReturn(Mono.error(connectionException));

            // when & then
            StepVerifier.create(permissionSpecCommandAdapter.invalidate())
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecRedisRepository).delete();
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("저장 후 무효화 시나리오")
        void shouldSaveAndThenInvalidate() {
            // given
            PermissionSpec permissionSpec = createPermissionSpec();
            PermissionSpecEntity entity = createPermissionSpecEntity();

            when(permissionSpecMapper.toEntity(permissionSpec)).thenReturn(entity);
            when(permissionSpecRedisRepository.save(entity)).thenReturn(Mono.empty());
            when(permissionSpecRedisRepository.delete()).thenReturn(Mono.empty());

            // when & then - 저장
            StepVerifier.create(permissionSpecCommandAdapter.save(permissionSpec)).verifyComplete();

            // when & then - 무효화
            StepVerifier.create(permissionSpecCommandAdapter.invalidate()).verifyComplete();

            verify(permissionSpecMapper).toEntity(permissionSpec);
            verify(permissionSpecRedisRepository).save(entity);
            verify(permissionSpecRedisRepository).delete();
        }

        @Test
        @DisplayName("여러 번 저장 후 무효화")
        void shouldSaveMultipleTimesAndInvalidate() {
            // given
            PermissionSpec spec1 = createPermissionSpec();
            PermissionSpec spec2 = createPermissionSpecWithMultipleEndpoints();
            PermissionSpecEntity entity1 = createPermissionSpecEntity();
            PermissionSpecEntity entity2 = createPermissionSpecEntityWithMultipleEndpoints();

            when(permissionSpecMapper.toEntity(spec1)).thenReturn(entity1);
            when(permissionSpecMapper.toEntity(spec2)).thenReturn(entity2);
            when(permissionSpecRedisRepository.save(any())).thenReturn(Mono.empty());
            when(permissionSpecRedisRepository.delete()).thenReturn(Mono.empty());

            // when & then - 첫 번째 저장
            StepVerifier.create(permissionSpecCommandAdapter.save(spec1)).verifyComplete();

            // when & then - 두 번째 저장
            StepVerifier.create(permissionSpecCommandAdapter.save(spec2)).verifyComplete();

            // when & then - 무효화
            StepVerifier.create(permissionSpecCommandAdapter.invalidate()).verifyComplete();

            verify(permissionSpecMapper).toEntity(spec1);
            verify(permissionSpecMapper).toEntity(spec2);
            verify(permissionSpecRedisRepository).save(entity1);
            verify(permissionSpecRedisRepository).save(entity2);
            verify(permissionSpecRedisRepository).delete();
        }
    }

    // Helper methods for creating test data
    private PermissionSpec createPermissionSpec() {
        EndpointPermission endpoint =
                EndpointPermission.of(
                        "user-service",
                        "/api/v1/users",
                        HttpMethod.GET,
                        Set.of(Permission.of("user:read")),
                        Set.of("USER"),
                        false);

        return PermissionSpec.of(123L, Instant.parse("2025-11-25T08:00:00Z"), List.of(endpoint));
    }

    private PermissionSpecEntity createPermissionSpecEntity() {
        // Mock entity - 실제 구현에서는 매퍼가 생성
        return new PermissionSpecEntity(123L, Instant.parse("2025-11-25T08:00:00Z"), List.of());
    }

    private PermissionSpec createEmptyPermissionSpec() {
        return PermissionSpec.of(456L, Instant.parse("2025-11-25T09:00:00Z"), List.of());
    }

    private PermissionSpecEntity createEmptyPermissionSpecEntity() {
        return new PermissionSpecEntity(456L, Instant.parse("2025-11-25T09:00:00Z"), List.of());
    }

    private PermissionSpec createPermissionSpecWithMultipleEndpoints() {
        EndpointPermission endpoint1 =
                EndpointPermission.of(
                        "user-service",
                        "/api/v1/users",
                        HttpMethod.GET,
                        Set.of(Permission.of("user:read")),
                        Set.of("USER"),
                        false);
        EndpointPermission endpoint2 =
                EndpointPermission.of(
                        "order-service",
                        "/api/v1/orders",
                        HttpMethod.POST,
                        Set.of(Permission.of("order:create")),
                        Set.of("ADMIN"),
                        false);

        return PermissionSpec.of(
                789L, Instant.parse("2025-11-25T10:00:00Z"), List.of(endpoint1, endpoint2));
    }

    private PermissionSpecEntity createPermissionSpecEntityWithMultipleEndpoints() {
        return new PermissionSpecEntity(789L, Instant.parse("2025-11-25T10:00:00Z"), List.of());
    }
}
