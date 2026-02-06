package com.ryuqq.gateway.application.authorization.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.internal.PermissionSpecCoordinator;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPermissionSpecService 단위 테스트")
class GetPermissionSpecServiceTest {

    @Mock private PermissionSpecCoordinator permissionSpecCoordinator;

    @InjectMocks private GetPermissionSpecService getPermissionSpecService;

    @Nested
    @DisplayName("getPermissionSpec() 테스트")
    class GetPermissionSpecTest {

        @Test
        @DisplayName("Coordinator를 통해 Permission Spec 조회")
        void shouldGetPermissionSpecThroughCoordinator() {
            // given
            PermissionSpec spec = createPermissionSpec(1L);

            given(permissionSpecCoordinator.findPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(
                            fetchedSpec -> {
                                assertThat(fetchedSpec).isEqualTo(spec);
                                assertThat(fetchedSpec.version()).isEqualTo(1L);
                            })
                    .verifyComplete();

            then(permissionSpecCoordinator).should().findPermissionSpec();
        }

        @Test
        @DisplayName("Coordinator에서 빈 Mono 반환 시 빈 Mono 반환")
        void shouldReturnEmptyMonoWhenCoordinatorReturnsEmpty() {
            // given
            given(permissionSpecCoordinator.findPermissionSpec()).willReturn(Mono.empty());

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).verifyComplete();

            then(permissionSpecCoordinator).should().findPermissionSpec();
        }

        @Test
        @DisplayName("Coordinator에서 에러 발생 시 에러 전파")
        void shouldPropagateErrorFromCoordinator() {
            // given
            given(permissionSpecCoordinator.findPermissionSpec())
                    .willReturn(Mono.error(new RuntimeException("Coordinator error")));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectErrorMessage("Coordinator error").verify();

            then(permissionSpecCoordinator).should().findPermissionSpec();
        }
    }

    @Nested
    @DisplayName("엔드포인트 데이터 검증")
    class EndpointDataValidation {

        @Test
        @DisplayName("조회한 Spec의 엔드포인트 목록 검증")
        void validateEndpointsInFetchedSpec() {
            // given
            EndpointPermission endpoint1 = createEndpoint("/api/v1/orders", HttpMethod.GET);
            EndpointPermission endpoint2 = createEndpoint("/api/v1/products", HttpMethod.POST);

            PermissionSpec spec =
                    PermissionSpec.of(1L, Instant.now(), List.of(endpoint1, endpoint2));

            given(permissionSpecCoordinator.findPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(
                            fetchedSpec -> {
                                assertThat(fetchedSpec.permissions()).hasSize(2);
                                assertThat(fetchedSpec.permissions())
                                        .containsExactly(endpoint1, endpoint2);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("빈 엔드포인트 목록도 정상 처리")
        void handleEmptyEndpointsList() {
            // given
            PermissionSpec emptySpec = PermissionSpec.of(1L, Instant.now(), List.of());

            given(permissionSpecCoordinator.findPermissionSpec()).willReturn(Mono.just(emptySpec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(spec -> assertThat(spec.permissions()).isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("여러 번 호출 테스트")
    class MultipleCallsTest {

        @Test
        @DisplayName("여러 번 호출 시 매번 Coordinator 호출")
        void shouldCallCoordinatorOnEveryCall() {
            // given
            PermissionSpec spec = createPermissionSpec(1L);

            given(permissionSpecCoordinator.findPermissionSpec()).willReturn(Mono.just(spec));

            // when
            getPermissionSpecService.getPermissionSpec().block();
            getPermissionSpecService.getPermissionSpec().block();

            // then
            then(permissionSpecCoordinator).should(times(2)).findPermissionSpec();
        }
    }

    // Helper methods
    private PermissionSpec createPermissionSpec(Long version) {
        EndpointPermission endpoint = createEndpoint("/api/v1/test", HttpMethod.GET);
        return PermissionSpec.of(version, Instant.now(), List.of(endpoint));
    }

    private EndpointPermission createEndpoint(String path, HttpMethod method) {
        return EndpointPermission.publicEndpoint("test-service", path, method);
    }
}
