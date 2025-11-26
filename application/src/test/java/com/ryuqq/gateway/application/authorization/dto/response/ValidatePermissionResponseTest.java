package com.ryuqq.gateway.application.authorization.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ValidatePermissionResponse 테스트")
class ValidatePermissionResponseTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 ValidatePermissionResponse 생성")
        void shouldCreateValidatePermissionResponse() {
            // given
            boolean authorized = true;
            EndpointPermission endpointPermission = createEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    new ValidatePermissionResponse(authorized, endpointPermission);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isEqualTo(endpointPermission);
        }

        @Test
        @DisplayName("null EndpointPermission으로 생성")
        void shouldCreateWithNullEndpointPermission() {
            // given
            boolean authorized = false;
            EndpointPermission endpointPermission = null;

            // when
            ValidatePermissionResponse response =
                    new ValidatePermissionResponse(authorized, endpointPermission);

            // then
            assertThat(response.authorized()).isFalse();
            assertThat(response.endpointPermission()).isNull();
        }
    }

    @Nested
    @DisplayName("authorized() 정적 팩토리 메서드 테스트")
    class AuthorizedMethodTest {

        @Test
        @DisplayName("권한 승인 응답 생성")
        void shouldCreateAuthorizedResponse() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.authorized(endpointPermission);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isEqualTo(endpointPermission);
        }

        @Test
        @DisplayName("null EndpointPermission으로 권한 승인 응답 생성")
        void shouldCreateAuthorizedResponseWithNullEndpointPermission() {
            // when
            ValidatePermissionResponse response = ValidatePermissionResponse.authorized(null);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isNull();
        }

        @Test
        @DisplayName("복잡한 EndpointPermission으로 권한 승인 응답 생성")
        void shouldCreateAuthorizedResponseWithComplexEndpointPermission() {
            // given
            EndpointPermission endpointPermission = createComplexEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.authorized(endpointPermission);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isEqualTo(endpointPermission);
            assertThat(response.endpointPermission().requiredPermissions()).hasSize(3);
            assertThat(response.endpointPermission().requiredRoles()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("denied() 정적 팩토리 메서드 테스트")
    class DeniedMethodTest {

        @Test
        @DisplayName("권한 거부 응답 생성")
        void shouldCreateDeniedResponse() {
            // when
            ValidatePermissionResponse response = ValidatePermissionResponse.denied();

            // then
            assertThat(response.authorized()).isFalse();
            assertThat(response.endpointPermission()).isNull();
        }
    }

    @Nested
    @DisplayName("publicEndpoint() 정적 팩토리 메서드 테스트")
    class PublicEndpointMethodTest {

        @Test
        @DisplayName("공개 엔드포인트 응답 생성")
        void shouldCreatePublicEndpointResponse() {
            // given
            EndpointPermission publicEndpointPermission = createPublicEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.publicEndpoint(publicEndpointPermission);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isEqualTo(publicEndpointPermission);
            assertThat(response.endpointPermission().isPublic()).isTrue();
        }

        @Test
        @DisplayName("null EndpointPermission으로 공개 엔드포인트 응답 생성")
        void shouldCreatePublicEndpointResponseWithNullEndpointPermission() {
            // when
            ValidatePermissionResponse response = ValidatePermissionResponse.publicEndpoint(null);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission()).isNull();
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals()가 올바르게 동작")
        void shouldHaveCorrectEquals() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();
            ValidatePermissionResponse response1 =
                    ValidatePermissionResponse.authorized(endpointPermission);
            ValidatePermissionResponse response2 =
                    ValidatePermissionResponse.authorized(endpointPermission);
            ValidatePermissionResponse response3 = ValidatePermissionResponse.denied();

            // when & then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1).isNotEqualTo(response3);
        }

        @Test
        @DisplayName("hashCode()가 올바르게 동작")
        void shouldHaveCorrectHashCode() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();
            ValidatePermissionResponse response1 =
                    ValidatePermissionResponse.authorized(endpointPermission);
            ValidatePermissionResponse response2 =
                    ValidatePermissionResponse.authorized(endpointPermission);

            // when & then
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("toString()이 모든 필드를 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.authorized(endpointPermission);

            // when
            String toString = response.toString();

            // then
            assertThat(toString).contains("true");
            assertThat(toString).contains("EndpointPermission");
        }

        @Test
        @DisplayName("denied 응답의 toString()")
        void shouldHaveCorrectToStringForDeniedResponse() {
            // given
            ValidatePermissionResponse response = ValidatePermissionResponse.denied();

            // when
            String toString = response.toString();

            // then
            assertThat(toString).contains("false");
            assertThat(toString).contains("null");
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 비교 테스트")
    class StaticFactoryMethodComparisonTest {

        @Test
        @DisplayName("authorized()와 publicEndpoint()는 동일한 결과")
        void shouldHaveSameResultForAuthorizedAndPublicEndpoint() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();

            // when
            ValidatePermissionResponse authorizedResponse =
                    ValidatePermissionResponse.authorized(endpointPermission);
            ValidatePermissionResponse publicResponse =
                    ValidatePermissionResponse.publicEndpoint(endpointPermission);

            // then
            assertThat(authorizedResponse).isEqualTo(publicResponse);
            assertThat(authorizedResponse.authorized()).isEqualTo(publicResponse.authorized());
            assertThat(authorizedResponse.endpointPermission())
                    .isEqualTo(publicResponse.endpointPermission());
        }

        @Test
        @DisplayName("authorized()와 denied()는 다른 결과")
        void shouldHaveDifferentResultForAuthorizedAndDenied() {
            // given
            EndpointPermission endpointPermission = createEndpointPermission();

            // when
            ValidatePermissionResponse authorizedResponse =
                    ValidatePermissionResponse.authorized(endpointPermission);
            ValidatePermissionResponse deniedResponse = ValidatePermissionResponse.denied();

            // then
            assertThat(authorizedResponse).isNotEqualTo(deniedResponse);
            assertThat(authorizedResponse.authorized()).isNotEqualTo(deniedResponse.authorized());
            assertThat(authorizedResponse.endpointPermission())
                    .isNotEqualTo(deniedResponse.endpointPermission());
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("EndpointPermission이 불변임")
        void shouldHaveImmutableEndpointPermission() {
            // given
            EndpointPermission originalEndpointPermission = createEndpointPermission();
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.authorized(originalEndpointPermission);

            // when
            EndpointPermission returnedEndpointPermission = response.endpointPermission();

            // then
            assertThat(returnedEndpointPermission).isEqualTo(originalEndpointPermission);
            // EndpointPermission은 불변 객체이므로 수정할 수 없음
        }
    }

    @Nested
    @DisplayName("다양한 시나리오 테스트")
    class VariousScenariosTest {

        @Test
        @DisplayName("관리자 권한이 필요한 엔드포인트 승인")
        void shouldAuthorizeAdminEndpoint() {
            // given
            EndpointPermission adminEndpoint = createAdminEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.authorized(adminEndpoint);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission().requiredRoles()).contains("ADMIN");
        }

        @Test
        @DisplayName("공개 엔드포인트 승인")
        void shouldAuthorizePublicEndpoint() {
            // given
            EndpointPermission publicEndpoint = createPublicEndpointPermission();

            // when
            ValidatePermissionResponse response =
                    ValidatePermissionResponse.publicEndpoint(publicEndpoint);

            // then
            assertThat(response.authorized()).isTrue();
            assertThat(response.endpointPermission().isPublic()).isTrue();
            assertThat(response.endpointPermission().requiredPermissions()).isEmpty();
            assertThat(response.endpointPermission().requiredRoles()).isEmpty();
        }

        @Test
        @DisplayName("권한 부족으로 거부")
        void shouldDenyInsufficientPermission() {
            // when
            ValidatePermissionResponse response = ValidatePermissionResponse.denied();

            // then
            assertThat(response.authorized()).isFalse();
            assertThat(response.endpointPermission()).isNull();
        }
    }

    // Helper methods for creating test data
    private EndpointPermission createEndpointPermission() {
        return EndpointPermission.of(
                "user-service",
                "/api/v1/users",
                HttpMethod.GET,
                Set.of(Permission.of("user:read")),
                Set.of("USER"),
                false);
    }

    private EndpointPermission createComplexEndpointPermission() {
        return EndpointPermission.of(
                "order-service",
                "/api/v1/orders",
                HttpMethod.POST,
                Set.of(
                        Permission.of("order:create"),
                        Permission.of("order:write"),
                        Permission.of("payment:process")),
                Set.of("USER", "ORDER_MANAGER"),
                false);
    }

    private EndpointPermission createPublicEndpointPermission() {
        return EndpointPermission.of(
                "public-service", "/api/v1/health", HttpMethod.GET, Set.of(), Set.of(), true);
    }

    private EndpointPermission createAdminEndpointPermission() {
        return EndpointPermission.of(
                "admin-service",
                "/api/v1/admin/users",
                HttpMethod.DELETE,
                Set.of(Permission.of("admin:delete")),
                Set.of("ADMIN"),
                false);
    }
}
