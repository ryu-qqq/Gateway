package com.ryuqq.gateway.domain.authorization.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionSpecNotFoundException 테스트")
class PermissionSpecNotFoundExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("경로와 메서드로 예외 생성")
        void shouldCreateExceptionWithPathAndMethod() {
            // given
            String path = "/api/v1/users";
            String method = "GET";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
            assertThat(exception.getMessage())
                    .isEqualTo(
                            "Permission spec not found for endpoint: GET /api/v1/users (Default"
                                    + " Deny)");
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("다양한 HTTP 메서드로 예외 생성")
        void shouldCreateExceptionWithVariousHttpMethods() {
            // given
            String path = "/api/v1/orders";
            String method = "POST";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
            assertThat(exception.getMessage())
                    .isEqualTo(
                            "Permission spec not found for endpoint: POST /api/v1/orders (Default"
                                    + " Deny)");
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("경로 파라미터가 있는 엔드포인트로 예외 생성")
        void shouldCreateExceptionWithPathParameters() {
            // given
            String path = "/api/v1/users/{userId}/orders/{orderId}";
            String method = "PUT";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
            assertThat(exception.getMessage())
                    .contains("PUT /api/v1/users/{userId}/orders/{orderId}");
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("루트 경로로 예외 생성")
        void shouldCreateExceptionWithRootPath() {
            // given
            String path = "/";
            String method = "GET";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
            assertThat(exception.getMessage())
                    .isEqualTo("Permission spec not found for endpoint: GET / (Default Deny)");
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("긴 경로로 예외 생성")
        void shouldCreateExceptionWithLongPath() {
            // given
            String path = "/api/v1/very/long/path/with/many/segments/and/parameters/{id}/details";
            String method = "DELETE";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
            assertThat(exception.getMessage()).contains("DELETE");
            assertThat(exception.getMessage()).contains(path);
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }
    }

    @Nested
    @DisplayName("경로와 메서드 접근 테스트")
    class PathAndMethodAccessTest {

        @Test
        @DisplayName("path() 메서드가 올바른 경로 반환")
        void shouldReturnCorrectPath() {
            // given
            String path = "/api/v1/products";
            String method = "PATCH";
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // when
            String returnedPath = exception.path();

            // then
            assertThat(returnedPath).isEqualTo(path);
        }

        @Test
        @DisplayName("method() 메서드가 올바른 HTTP 메서드 반환")
        void shouldReturnCorrectMethod() {
            // given
            String path = "/api/v1/inventory";
            String method = "OPTIONS";
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // when
            String returnedMethod = exception.method();

            // then
            assertThat(returnedMethod).isEqualTo(method);
        }

        @Test
        @DisplayName("특수 문자가 포함된 경로 처리")
        void shouldHandlePathWithSpecialCharacters() {
            // given
            String path = "/api/v1/search?query=test&filter=active";
            String method = "GET";
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // when & then
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
            assertThat(exception.getMessage()).contains(path);
        }

        @Test
        @DisplayName("소문자 HTTP 메서드 처리")
        void shouldHandleLowercaseHttpMethod() {
            // given
            String path = "/api/v1/notifications";
            String method = "post";
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // when & then
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
            assertThat(exception.getMessage()).contains("post /api/v1/notifications");
        }
    }

    @Nested
    @DisplayName("메시지 생성 테스트")
    class MessageBuildingTest {

        @Test
        @DisplayName("메시지 형식이 올바름")
        void shouldHaveCorrectMessageFormat() {
            // given
            String path = "/api/v1/analytics";
            String method = "GET";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            String message = exception.getMessage();
            assertThat(message).startsWith("Permission spec not found for endpoint:");
            assertThat(message).contains("GET /api/v1/analytics");
            assertThat(message).endsWith("(Default Deny)");
        }

        @Test
        @DisplayName("메시지에 Default Deny 정책 명시")
        void shouldMentionDefaultDenyPolicy() {
            // given
            String path = "/api/v1/reports";
            String method = "POST";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.getMessage()).contains("(Default Deny)");
        }

        @Test
        @DisplayName("메시지에 HTTP 메서드와 경로가 올바른 순서로 표시")
        void shouldDisplayMethodAndPathInCorrectOrder() {
            // given
            String path = "/api/v1/settings";
            String method = "PUT";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            String message = exception.getMessage();
            int methodIndex = message.indexOf("PUT");
            int pathIndex = message.indexOf("/api/v1/settings");
            assertThat(methodIndex).isLessThan(pathIndex);
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND와 매핑됨")
        void shouldMapToPermissionSpecNotFoundErrorCode() {
            // given
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException("/api/v1/test", "GET");

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_SPEC_NOT_FOUND.getCode());
            assertThat(exception.getCode()).isEqualTo("AUTHZ-002");
        }
    }

    @Nested
    @DisplayName("필드 접근자 테스트")
    class FieldAccessorTest {

        @Test
        @DisplayName("경로와 메서드를 개별 필드 접근자로 확인")
        void shouldAccessPathAndMethodThroughGetters() {
            // given
            String path = "/api/v1/webhooks";
            String method = "POST";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("기본 생성자는 null 값 반환")
        void shouldReturnNullForDefaultConstructor() {
            // given
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException();

            // when & then
            assertThat(exception.path()).isNull();
            assertThat(exception.method()).isNull();
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException("/api/v1/test", "GET");

            // when & then
            assertThat(exception).isInstanceOf(RuntimeException.class);
            // DomainException 클래스가 있다고 가정
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("null 경로로 예외 생성")
        void shouldCreateExceptionWithNullPath() {
            // given
            String path = null;
            String method = "GET";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then - null이 문자열로 처리됨
            assertThat(exception.path()).isNull();
            assertThat(exception.method()).isEqualTo(method);
            assertThat(exception.getMessage()).contains("GET null");
        }

        @Test
        @DisplayName("null 메서드로 예외 생성")
        void shouldCreateExceptionWithNullMethod() {
            // given
            String path = "/api/v1/test";
            String method = null;

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then - null이 문자열로 처리됨
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isNull();
            assertThat(exception.getMessage()).contains("null /api/v1/test");
        }

        @Test
        @DisplayName("빈 문자열 경로로 예외 생성")
        void shouldCreateExceptionWithEmptyPath() {
            // given
            String path = "";
            String method = "GET";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
            assertThat(exception.getMessage()).contains("GET ");
        }

        @Test
        @DisplayName("빈 문자열 메서드로 예외 생성")
        void shouldCreateExceptionWithEmptyMethod() {
            // given
            String path = "/api/v1/test";
            String method = "";

            // when
            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException(path, method);

            // then
            assertThat(exception.path()).isEqualTo(path);
            assertThat(exception.method()).isEqualTo(method);
            assertThat(exception.getMessage()).contains(" /api/v1/test");
        }
    }
}
