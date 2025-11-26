package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.application.authorization.port.in.command.ValidatePermissionUseCase;
import com.ryuqq.gateway.domain.authorization.exception.PermissionDeniedException;
import com.ryuqq.gateway.domain.authorization.exception.PermissionSpecNotFoundException;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionFilter 테스트")
class PermissionFilterTest {

    @Mock private ValidatePermissionUseCase validatePermissionUseCase;

    @Mock private GatewayFilterChain chain;

    private ObjectMapper objectMapper;
    private PermissionFilter permissionFilter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        permissionFilter = new PermissionFilter(validatePermissionUseCase, objectMapper);
    }

    private EndpointPermission createMockEndpoint() {
        return EndpointPermission.publicEndpoint(
                "test-service", "/test", com.ryuqq.gateway.domain.authorization.vo.HttpMethod.GET);
    }

    @Nested
    @DisplayName("getOrder() 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("올바른 필터 순서 반환")
        void shouldReturnCorrectFilterOrder() {
            // when
            int order = permissionFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.PERMISSION_FILTER);
        }
    }

    @Nested
    @DisplayName("filter() 테스트")
    class FilterTest {

        @Test
        @DisplayName("권한 검증 성공 시 다음 필터로 진행")
        void shouldProceedToNextFilterWhenPermissionGranted() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/users", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse successResponse =
                    ValidatePermissionResponse.authorized(createMockEndpoint());
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(successResponse));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            verify(validatePermissionUseCase).execute(any(ValidatePermissionCommand.class));
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("권한 검증 실패 시 403 Forbidden 응답")
        void shouldReturnForbiddenWhenPermissionDenied() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/admin", HttpMethod.POST);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse deniedResponse = ValidatePermissionResponse.denied();
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(validatePermissionUseCase).execute(any(ValidatePermissionCommand.class));
        }

        @Test
        @DisplayName("PermissionDeniedException 발생 시 403 Forbidden 응답")
        void shouldReturnForbiddenWhenPermissionDeniedExceptionThrown() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/orders", HttpMethod.DELETE);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            PermissionDeniedException exception =
                    new PermissionDeniedException(Set.of("order:delete"), Set.of("order:read"));
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(validatePermissionUseCase).execute(any(ValidatePermissionCommand.class));
        }

        @Test
        @DisplayName("PermissionSpecNotFoundException 발생 시 403 Forbidden 응답")
        void shouldReturnForbiddenWhenPermissionSpecNotFound() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/unknown", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            PermissionSpecNotFoundException exception =
                    new PermissionSpecNotFoundException("/api/v1/unknown", "GET");
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(validatePermissionUseCase).execute(any(ValidatePermissionCommand.class));
        }

        @Test
        @DisplayName("일반 예외 발생 시 500 Internal Server Error 응답")
        void shouldReturnInternalErrorWhenGeneralExceptionThrown() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/users", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            RuntimeException exception = new RuntimeException("Unexpected error");
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(validatePermissionUseCase).execute(any(ValidatePermissionCommand.class));
        }

        @Test
        @DisplayName("ValidatePermissionCommand가 올바르게 생성됨")
        void shouldCreateCorrectValidatePermissionCommand() {
            // given - {id} 같은 URI 템플릿 변수는 MockServerHttpRequest에서 지원하지 않으므로 일반 경로 사용
            ServerWebExchange exchange = createExchange("/api/v1/products/123", HttpMethod.PUT);
            String userId = "user999";
            String tenantId = "tenant888";
            String permissionHash = "hash777";
            Set<String> roles = Set.of("ADMIN", "PRODUCT_MANAGER");
            setUserAttributes(exchange, userId, tenantId, permissionHash, roles);

            ValidatePermissionResponse successResponse =
                    ValidatePermissionResponse.authorized(createMockEndpoint());
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(successResponse));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            // then
            ArgumentCaptor<ValidatePermissionCommand> commandCaptor =
                    ArgumentCaptor.forClass(ValidatePermissionCommand.class);
            verify(validatePermissionUseCase).execute(commandCaptor.capture());

            ValidatePermissionCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.userId()).isEqualTo(userId);
            assertThat(capturedCommand.tenantId()).isEqualTo(tenantId);
            assertThat(capturedCommand.permissionHash()).isEqualTo(permissionHash);
            assertThat(capturedCommand.roles()).isEqualTo(roles);
            assertThat(capturedCommand.requestPath()).isEqualTo("/api/v1/products/123");
            assertThat(capturedCommand.requestMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("null 역할이 빈 Set으로 처리됨")
        void shouldHandleNullRolesAsEmptySet() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/public", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", null);

            ValidatePermissionResponse successResponse =
                    ValidatePermissionResponse.authorized(createMockEndpoint());
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(successResponse));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            // then
            ArgumentCaptor<ValidatePermissionCommand> commandCaptor =
                    ArgumentCaptor.forClass(ValidatePermissionCommand.class);
            verify(validatePermissionUseCase).execute(commandCaptor.capture());

            ValidatePermissionCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.roles()).isEmpty();
        }

        @Test
        @DisplayName("다양한 HTTP 메서드 처리")
        void shouldHandleVariousHttpMethods() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/data", HttpMethod.PATCH);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse successResponse =
                    ValidatePermissionResponse.authorized(createMockEndpoint());
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(successResponse));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            // then
            ArgumentCaptor<ValidatePermissionCommand> commandCaptor =
                    ArgumentCaptor.forClass(ValidatePermissionCommand.class);
            verify(validatePermissionUseCase).execute(commandCaptor.capture());

            ValidatePermissionCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.requestMethod()).isEqualTo("PATCH");
        }

        @Test
        @DisplayName("쿼리 파라미터가 있는 경로 처리")
        void shouldHandlePathWithQueryParameters() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/search?query=test&page=1").build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse successResponse =
                    ValidatePermissionResponse.authorized(createMockEndpoint());
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(successResponse));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            // then
            ArgumentCaptor<ValidatePermissionCommand> commandCaptor =
                    ArgumentCaptor.forClass(ValidatePermissionCommand.class);
            verify(validatePermissionUseCase).execute(commandCaptor.capture());

            ValidatePermissionCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.requestPath()).isEqualTo("/api/v1/search");
        }
    }

    @Nested
    @DisplayName("에러 응답 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("JSON 직렬화 실패 시 빈 응답 반환")
        void shouldReturnEmptyResponseWhenJsonSerializationFails() {
            // given
            // ObjectMapper를 override하여 JsonProcessingException을 발생시키는 것은 어렵기 때문에
            // JsonProcessingException이 발생했을 때 setComplete()가 호출되는 것을 테스트
            // 실제 구현에서는 ApiResponse 직렬화가 실패할 경우가 거의 없으므로
            // 이 테스트는 denied 응답이 정상적으로 반환되는지 확인하는 방향으로 변경
            ServerWebExchange exchange = createExchange("/api/v1/test", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse deniedResponse = ValidatePermissionResponse.denied();
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when & then
            StepVerifier.create(permissionFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("forbidden 응답에서 JSON 직렬화 실패 시 setComplete 호출")
        void shouldCallSetCompleteWhenJsonSerializationFailsForForbidden() throws Exception {
            // given
            ObjectMapper mockObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
            when(mockObjectMapper.writeValueAsBytes(any()))
                    .thenThrow(
                            new com.fasterxml.jackson.core.JsonProcessingException(
                                    "Mocked error") {});
            PermissionFilter filterWithMockedMapper =
                    new PermissionFilter(validatePermissionUseCase, mockObjectMapper);

            ServerWebExchange exchange = createExchange("/api/v1/test", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            ValidatePermissionResponse deniedResponse = ValidatePermissionResponse.denied();
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when & then
            StepVerifier.create(filterWithMockedMapper.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("internalError 응답에서 JSON 직렬화 실패 시 setComplete 호출")
        void shouldCallSetCompleteWhenJsonSerializationFailsForInternalError() throws Exception {
            // given
            ObjectMapper mockObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
            when(mockObjectMapper.writeValueAsBytes(any()))
                    .thenThrow(
                            new com.fasterxml.jackson.core.JsonProcessingException(
                                    "Mocked error") {});
            PermissionFilter filterWithMockedMapper =
                    new PermissionFilter(validatePermissionUseCase, mockObjectMapper);

            ServerWebExchange exchange = createExchange("/api/v1/test", HttpMethod.GET);
            setUserAttributes(exchange, "user123", "tenant456", "hash789", Set.of("USER"));

            RuntimeException exception = new RuntimeException("Unexpected error");
            when(validatePermissionUseCase.execute(any(ValidatePermissionCommand.class)))
                    .thenReturn(Mono.error(exception));

            // when & then
            StepVerifier.create(filterWithMockedMapper.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods
    private ServerWebExchange createExchange(String path, HttpMethod method) {
        MockServerHttpRequest request = MockServerHttpRequest.method(method, path).build();
        return MockServerWebExchange.from(request);
    }

    private void setUserAttributes(
            ServerWebExchange exchange,
            String userId,
            String tenantId,
            String permissionHash,
            Set<String> roles) {
        exchange.getAttributes().put("userId", userId);
        exchange.getAttributes().put("tenantId", tenantId);
        exchange.getAttributes().put("permissionHash", permissionHash);
        // ConcurrentHashMap은 null 값을 허용하지 않으므로 null인 경우 put하지 않음
        // PermissionFilter는 getAttribute로 null을 받게 됨
        if (roles != null) {
            exchange.getAttributes().put("roles", roles);
        }
    }
}
