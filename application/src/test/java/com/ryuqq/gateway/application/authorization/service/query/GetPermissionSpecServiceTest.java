package com.ryuqq.gateway.application.authorization.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionSpecCommandPort;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionSpecQueryPort;
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

    @Mock private PermissionSpecQueryPort permissionSpecQueryPort;

    @Mock private PermissionSpecCommandPort permissionSpecCommandPort;

    @Mock private AuthHubPermissionClient authHubPermissionClient;

    @InjectMocks private GetPermissionSpecService getPermissionSpecService;

    @Nested
    @DisplayName("Cache Hit 시나리오")
    class CacheHitScenario {

        @Test
        @DisplayName("캐시에 Spec이 존재하면 캐시에서 반환")
        void returnSpecFromCacheWhenExists() {
            // given
            PermissionSpec cachedSpec = createPermissionSpec(1L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.just(cachedSpec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(
                            spec -> {
                                assertThat(spec).isEqualTo(cachedSpec);
                                assertThat(spec.version()).isEqualTo(1L);
                            })
                    .verifyComplete();

            then(authHubPermissionClient).shouldHaveNoInteractions();
            then(permissionSpecCommandPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("캐시 조회 성공 시 AuthHub 호출하지 않음")
        void notCallAuthHubWhenCacheHit() {
            // given
            PermissionSpec cachedSpec = createPermissionSpec(1L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.just(cachedSpec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectNextCount(1).verifyComplete();

            then(authHubPermissionClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Cache Miss 시나리오")
    class CacheMissScenario {

        @Test
        @DisplayName("캐시에 Spec이 없으면 AuthHub에서 조회 후 캐시")
        void fetchFromAuthHubAndCacheWhenCacheMiss() {
            // given
            PermissionSpec fetchedSpec = createPermissionSpec(2L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec()).willReturn(Mono.just(fetchedSpec));
            given(permissionSpecCommandPort.save(any(PermissionSpec.class)))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(
                            spec -> {
                                assertThat(spec).isEqualTo(fetchedSpec);
                                assertThat(spec.version()).isEqualTo(2L);
                            })
                    .verifyComplete();

            then(authHubPermissionClient).should().fetchPermissionSpec();
            then(permissionSpecCommandPort).should().save(fetchedSpec);
        }

        @Test
        @DisplayName("AuthHub에서 조회한 Spec을 캐시에 저장")
        void saveSpecToCacheAfterFetchingFromAuthHub() {
            // given
            PermissionSpec fetchedSpec = createPermissionSpec(3L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec()).willReturn(Mono.just(fetchedSpec));
            given(permissionSpecCommandPort.save(fetchedSpec)).willReturn(Mono.empty());

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectNext(fetchedSpec).verifyComplete();

            then(permissionSpecCommandPort).should().save(fetchedSpec);
        }

        @Test
        @DisplayName("캐시 저장 실패해도 조회한 Spec 반환")
        void returnFetchedSpecEvenIfCacheSaveFails() {
            // given
            PermissionSpec fetchedSpec = createPermissionSpec(4L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec()).willReturn(Mono.just(fetchedSpec));
            given(permissionSpecCommandPort.save(any(PermissionSpec.class)))
                    .willReturn(Mono.error(new RuntimeException("Cache save failed")));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectErrorMessage("Cache save failed").verify();
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("캐시 조회 실패 시 AuthHub에서 조회")
        void fetchFromAuthHubWhenCacheQueryFails() {
            // given
            PermissionSpec fetchedSpec = createPermissionSpec(5L);

            given(permissionSpecQueryPort.findPermissionSpec())
                    .willReturn(Mono.error(new RuntimeException("Cache query failed")));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectErrorMessage("Cache query failed").verify();

            then(authHubPermissionClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("AuthHub 조회 실패 시 에러 전파")
        void propagateErrorWhenAuthHubFetchFails() {
            // given
            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec())
                    .willReturn(Mono.error(new RuntimeException("AuthHub fetch failed")));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectErrorMessage("AuthHub fetch failed").verify();
        }

        @Test
        @DisplayName("AuthHub가 빈 응답 반환 시 빈 Mono 반환")
        void returnEmptyMonoWhenAuthHubReturnsEmpty() {
            // given
            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec()).willReturn(Mono.empty());

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).verifyComplete();

            then(permissionSpecCommandPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Cache-aside 패턴 검증")
    class CacheAsidePattern {

        @Test
        @DisplayName("캐시 우선 조회 후 미스 시 AuthHub 조회")
        void followCacheAsidePattern() {
            // given
            PermissionSpec fetchedSpec = createPermissionSpec(6L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.empty());
            given(authHubPermissionClient.fetchPermissionSpec()).willReturn(Mono.just(fetchedSpec));
            given(permissionSpecCommandPort.save(any(PermissionSpec.class)))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result).expectNext(fetchedSpec).verifyComplete();

            then(permissionSpecQueryPort).should().findPermissionSpec();
            then(authHubPermissionClient).should().fetchPermissionSpec();
            then(permissionSpecCommandPort).should().save(fetchedSpec);
        }

        @Test
        @DisplayName("여러 번 호출 시 매번 캐시 조회")
        void queryCacheOnEveryCall() {
            // given
            PermissionSpec cachedSpec = createPermissionSpec(7L);

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.just(cachedSpec));

            // when
            getPermissionSpecService.getPermissionSpec().block();
            getPermissionSpecService.getPermissionSpec().block();

            // then
            then(permissionSpecQueryPort).should(times(2)).findPermissionSpec();
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

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.just(spec));

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

            given(permissionSpecQueryPort.findPermissionSpec()).willReturn(Mono.just(emptySpec));

            // when
            Mono<PermissionSpec> result = getPermissionSpecService.getPermissionSpec();

            // then
            StepVerifier.create(result)
                    .assertNext(spec -> assertThat(spec.permissions()).isEmpty())
                    .verifyComplete();
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
