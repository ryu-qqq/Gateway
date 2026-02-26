package com.ryuqq.gateway.application.authorization.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.application.authorization.internal.PermissionValidationCoordinator;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * ValidatePermissionService 단위 테스트
 *
 * <p>Service → Coordinator 위임 테스트
 *
 * <p>상세한 검증 로직 테스트는 PermissionValidationCoordinatorTest 참조
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePermissionService 단위 테스트")
class ValidatePermissionServiceTest {

    @Mock private PermissionValidationCoordinator permissionValidationCoordinator;

    @InjectMocks private ValidatePermissionService validatePermissionService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PERMISSION_HASH = "hash-abc";

    @Nested
    @DisplayName("Coordinator 위임 테스트")
    class CoordinatorDelegation {

        @Test
        @DisplayName("execute 호출 시 Coordinator.validate로 위임")
        void delegateToCoordinator() {
            // given
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/orders", HttpMethod.GET);
            ValidatePermissionResponse expectedResponse =
                    ValidatePermissionResponse.authorized(endpoint);

            given(permissionValidationCoordinator.validate(command))
                    .willReturn(Mono.just(expectedResponse));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.authorized()).isTrue();
                                assertThat(response.endpointPermission()).isEqualTo(endpoint);
                            })
                    .verifyComplete();

            then(permissionValidationCoordinator).should().validate(command);
        }

        @Test
        @DisplayName("Coordinator 에러 발생 시 에러 전파")
        void propagateCoordinatorError() {
            // given
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(permissionValidationCoordinator.validate(command))
                    .willReturn(Mono.error(new RuntimeException("Validation failed")));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Validation failed").verify();
        }
    }
}
