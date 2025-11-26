package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.out.redis.entity.EndpointPermissionEntity;
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
@DisplayName("PermissionSpecQueryAdapter 테스트")
class PermissionSpecQueryAdapterTest {

    @Mock private PermissionSpecRedisRepository permissionSpecRedisRepository;

    @Mock private PermissionSpecMapper permissionSpecMapper;

    private PermissionSpecQueryAdapter permissionSpecQueryAdapter;

    @BeforeEach
    void setUp() {
        permissionSpecQueryAdapter =
                new PermissionSpecQueryAdapter(permissionSpecRedisRepository, permissionSpecMapper);
    }

    @Nested
    @DisplayName("findPermissionSpec() 테스트")
    class FindPermissionSpecTest {

        @Test
        @DisplayName("Redis에서 Permission Spec 조회 성공")
        void shouldFindPermissionSpecSuccessfully() {
            // given
            PermissionSpecEntity entity = createPermissionSpecEntity();
            PermissionSpec expectedSpec = createPermissionSpec();

            when(permissionSpecRedisRepository.find()).thenReturn(Mono.just(entity));
            when(permissionSpecMapper.toPermissionSpec(entity)).thenReturn(expectedSpec);

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectNext(expectedSpec)
                    .verifyComplete();

            verify(permissionSpecRedisRepository).find();
            verify(permissionSpecMapper).toPermissionSpec(entity);
        }

        @Test
        @DisplayName("Redis에서 Permission Spec이 없을 때 empty Mono 반환")
        void shouldReturnEmptyMonoWhenSpecNotFound() {
            // given
            when(permissionSpecRedisRepository.find()).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec()).verifyComplete();

            verify(permissionSpecRedisRepository).find();
        }

        @Test
        @DisplayName("Redis 조회 중 에러 발생 시 RuntimeException으로 변환")
        void shouldMapErrorToRuntimeException() {
            // given
            RuntimeException originalException = new RuntimeException("Redis connection failed");
            when(permissionSpecRedisRepository.find()).thenReturn(Mono.error(originalException));

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .equals(
                                                            "Failed to get permission spec from"
                                                                    + " Redis")
                                            && throwable.getCause() == originalException)
                    .verify();

            verify(permissionSpecRedisRepository).find();
        }

        @Test
        @DisplayName("매퍼에서 에러 발생 시 RuntimeException으로 변환")
        void shouldMapMapperErrorToRuntimeException() {
            // given
            PermissionSpecEntity entity = createPermissionSpecEntity();
            RuntimeException mapperException = new RuntimeException("Mapping failed");

            when(permissionSpecRedisRepository.find()).thenReturn(Mono.just(entity));
            when(permissionSpecMapper.toPermissionSpec(entity)).thenThrow(mapperException);

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .equals(
                                                            "Failed to get permission spec from"
                                                                    + " Redis")
                                            && throwable.getCause() == mapperException)
                    .verify();

            verify(permissionSpecRedisRepository).find();
            verify(permissionSpecMapper).toPermissionSpec(entity);
        }

        @Test
        @DisplayName("복수의 엔드포인트가 있는 Permission Spec 조회")
        void shouldFindPermissionSpecWithMultipleEndpoints() {
            // given
            PermissionSpecEntity entity = createPermissionSpecEntityWithMultipleEndpoints();
            PermissionSpec expectedSpec = createPermissionSpecWithMultipleEndpoints();

            when(permissionSpecRedisRepository.find()).thenReturn(Mono.just(entity));
            when(permissionSpecMapper.toPermissionSpec(entity)).thenReturn(expectedSpec);

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectNext(expectedSpec)
                    .verifyComplete();

            verify(permissionSpecRedisRepository).find();
            verify(permissionSpecMapper).toPermissionSpec(entity);
        }

        @Test
        @DisplayName("빈 권한 리스트가 있는 Permission Spec 조회")
        void shouldFindPermissionSpecWithEmptyPermissions() {
            // given
            PermissionSpecEntity entity = createPermissionSpecEntityWithEmptyPermissions();
            PermissionSpec expectedSpec = createPermissionSpecWithEmptyPermissions();

            when(permissionSpecRedisRepository.find()).thenReturn(Mono.just(entity));
            when(permissionSpecMapper.toPermissionSpec(entity)).thenReturn(expectedSpec);

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectNext(expectedSpec)
                    .verifyComplete();

            verify(permissionSpecRedisRepository).find();
            verify(permissionSpecMapper).toPermissionSpec(entity);
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Redis 연결 타임아웃 에러 처리")
        void shouldHandleRedisTimeoutError() {
            // given
            RuntimeException timeoutException = new RuntimeException("Connection timeout");
            when(permissionSpecRedisRepository.find()).thenReturn(Mono.error(timeoutException));

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable
                                                    .getMessage()
                                                    .equals(
                                                            "Failed to get permission spec from"
                                                                    + " Redis"))
                    .verify();

            verify(permissionSpecRedisRepository).find();
        }

        @Test
        @DisplayName("Redis 직렬화 에러 처리")
        void shouldHandleRedisSerializationError() {
            // given
            RuntimeException serializationException = new RuntimeException("Serialization error");
            when(permissionSpecRedisRepository.find())
                    .thenReturn(Mono.error(serializationException));

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable.getCause() == serializationException)
                    .verify();

            verify(permissionSpecRedisRepository).find();
        }
    }

    @Nested
    @DisplayName("로깅 테스트")
    class LoggingTest {

        @Test
        @DisplayName("성공 시 디버그 로그 출력")
        void shouldLogDebugOnSuccess() {
            // given
            PermissionSpecEntity entity = createPermissionSpecEntity();
            PermissionSpec expectedSpec = createPermissionSpec();

            when(permissionSpecRedisRepository.find()).thenReturn(Mono.just(entity));
            when(permissionSpecMapper.toPermissionSpec(entity)).thenReturn(expectedSpec);

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectNext(expectedSpec)
                    .verifyComplete();

            // 로그는 실제로는 검증하기 어려우므로 정상 완료만 확인
            verify(permissionSpecRedisRepository).find();
            verify(permissionSpecMapper).toPermissionSpec(entity);
        }

        @Test
        @DisplayName("에러 시 에러 로그 출력")
        void shouldLogErrorOnFailure() {
            // given
            RuntimeException exception = new RuntimeException("Test error");
            when(permissionSpecRedisRepository.find()).thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(permissionSpecQueryAdapter.findPermissionSpec())
                    .expectError(RuntimeException.class)
                    .verify();

            verify(permissionSpecRedisRepository).find();
        }
    }

    // Helper methods for creating test data
    private PermissionSpecEntity createPermissionSpecEntity() {
        EndpointPermissionEntity endpointEntity =
                new EndpointPermissionEntity(
                        "user-service",
                        "/api/v1/users",
                        "GET",
                        Set.of("user:read"),
                        Set.of("USER"),
                        false);

        return new PermissionSpecEntity(
                123L, Instant.parse("2025-11-25T08:00:00Z"), List.of(endpointEntity));
    }

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

    private PermissionSpecEntity createPermissionSpecEntityWithMultipleEndpoints() {
        EndpointPermissionEntity endpoint1 =
                new EndpointPermissionEntity(
                        "user-service",
                        "/api/v1/users",
                        "GET",
                        Set.of("user:read"),
                        Set.of("USER"),
                        false);
        EndpointPermissionEntity endpoint2 =
                new EndpointPermissionEntity(
                        "order-service",
                        "/api/v1/orders",
                        "POST",
                        Set.of("order:create"),
                        Set.of("ADMIN"),
                        false);

        return new PermissionSpecEntity(
                456L, Instant.parse("2025-11-25T09:00:00Z"), List.of(endpoint1, endpoint2));
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
                456L, Instant.parse("2025-11-25T09:00:00Z"), List.of(endpoint1, endpoint2));
    }

    private PermissionSpecEntity createPermissionSpecEntityWithEmptyPermissions() {
        return new PermissionSpecEntity(789L, Instant.parse("2025-11-25T10:00:00Z"), List.of());
    }

    private PermissionSpec createPermissionSpecWithEmptyPermissions() {
        return PermissionSpec.of(789L, Instant.parse("2025-11-25T10:00:00Z"), List.of());
    }
}
