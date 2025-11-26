package com.ryuqq.gateway.adapter.out.redis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EndpointPermissionEntity 테스트")
class EndpointPermissionEntityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 EndpointPermissionEntity 생성")
        void shouldCreateEndpointPermissionEntity() {
            // given
            String serviceName = "user-service";
            String path = "/api/v1/users";
            String method = "GET";
            Set<String> requiredPermissions = Set.of("user:read", "user:list");
            Set<String> requiredRoles = Set.of("USER", "ADMIN");
            boolean isPublic = false;

            // when
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            serviceName,
                            path,
                            method,
                            requiredPermissions,
                            requiredRoles,
                            isPublic);

            // then
            assertThat(entity.getServiceName()).isEqualTo(serviceName);
            assertThat(entity.getPath()).isEqualTo(path);
            assertThat(entity.getMethod()).isEqualTo(method);
            assertThat(entity.getRequiredPermissions()).isEqualTo(requiredPermissions);
            assertThat(entity.getRequiredRoles()).isEqualTo(requiredRoles);
            assertThat(entity.isPublic()).isFalse();
        }

        @Test
        @DisplayName("공개 엔드포인트 생성")
        void shouldCreatePublicEndpointPermissionEntity() {
            // given
            String serviceName = "auth-service";
            String path = "/api/v1/login";
            String method = "POST";
            Set<String> emptyPermissions = Set.of();
            Set<String> emptyRoles = Set.of();
            boolean isPublic = true;

            // when
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            serviceName, path, method, emptyPermissions, emptyRoles, isPublic);

            // then
            assertThat(entity.getServiceName()).isEqualTo(serviceName);
            assertThat(entity.getPath()).isEqualTo(path);
            assertThat(entity.getMethod()).isEqualTo(method);
            assertThat(entity.getRequiredPermissions()).isEmpty();
            assertThat(entity.getRequiredRoles()).isEmpty();
            assertThat(entity.isPublic()).isTrue();
        }

        @Test
        @DisplayName("null 값들로 EndpointPermissionEntity 생성")
        void shouldCreateEndpointPermissionEntityWithNullValues() {
            // when
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(null, null, null, null, null, false);

            // then
            assertThat(entity.getServiceName()).isNull();
            assertThat(entity.getPath()).isNull();
            assertThat(entity.getMethod()).isNull();
            assertThat(entity.getRequiredPermissions()).isNull();
            assertThat(entity.getRequiredRoles()).isNull();
            assertThat(entity.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("EndpointPermissionEntity를 JSON으로 직렬화")
        void shouldSerializeToJson() throws JsonProcessingException {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            "order-service",
                            "/api/v1/orders",
                            "POST",
                            Set.of("order:create"),
                            Set.of("ADMIN"),
                            false);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"serviceName\":\"order-service\"");
            assertThat(json).contains("\"path\":\"/api/v1/orders\"");
            assertThat(json).contains("\"method\":\"POST\"");
            assertThat(json).contains("\"requiredPermissions\":");
            assertThat(json).contains("\"order:create\"");
            assertThat(json).contains("\"requiredRoles\":");
            assertThat(json).contains("\"ADMIN\"");
            assertThat(json).contains("\"isPublic\":false");
        }

        @Test
        @DisplayName("공개 엔드포인트를 JSON으로 직렬화")
        void shouldSerializePublicEndpointToJson() throws JsonProcessingException {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            "public-service", "/api/v1/health", "GET", Set.of(), Set.of(), true);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"serviceName\":\"public-service\"");
            assertThat(json).contains("\"path\":\"/api/v1/health\"");
            assertThat(json).contains("\"method\":\"GET\"");
            assertThat(json).contains("\"requiredPermissions\":[]");
            assertThat(json).contains("\"requiredRoles\":[]");
            assertThat(json).contains("\"isPublic\":true");
        }

        @Test
        @DisplayName("null 값들이 있는 EndpointPermissionEntity를 JSON으로 직렬화")
        void shouldSerializeEntityWithNullValues() throws JsonProcessingException {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(null, null, null, null, null, false);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"serviceName\":null");
            assertThat(json).contains("\"path\":null");
            assertThat(json).contains("\"method\":null");
            assertThat(json).contains("\"requiredPermissions\":null");
            assertThat(json).contains("\"requiredRoles\":null");
            assertThat(json).contains("\"isPublic\":false");
        }
    }

    @Nested
    @DisplayName("JSON 역직렬화 테스트")
    class JsonDeserializationTest {

        @Test
        @DisplayName("JSON을 EndpointPermissionEntity로 역직렬화")
        void shouldDeserializeFromJson() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "serviceName": "payment-service",
                        "path": "/api/v1/payments/{paymentId}",
                        "method": "GET",
                        "requiredPermissions": ["payment:read", "payment:view"],
                        "requiredRoles": ["USER", "PAYMENT_VIEWER"],
                        "isPublic": false
                    }
                    """;

            // when
            EndpointPermissionEntity entity =
                    objectMapper.readValue(json, EndpointPermissionEntity.class);

            // then
            assertThat(entity.getServiceName()).isEqualTo("payment-service");
            assertThat(entity.getPath()).isEqualTo("/api/v1/payments/{paymentId}");
            assertThat(entity.getMethod()).isEqualTo("GET");
            assertThat(entity.getRequiredPermissions())
                    .containsExactlyInAnyOrder("payment:read", "payment:view");
            assertThat(entity.getRequiredRoles())
                    .containsExactlyInAnyOrder("USER", "PAYMENT_VIEWER");
            assertThat(entity.isPublic()).isFalse();
        }

        @Test
        @DisplayName("공개 엔드포인트 JSON을 역직렬화")
        void shouldDeserializePublicEndpointFromJson() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "serviceName": "notification-service",
                        "path": "/api/v1/notifications/webhook",
                        "method": "POST",
                        "requiredPermissions": [],
                        "requiredRoles": [],
                        "isPublic": true
                    }
                    """;

            // when
            EndpointPermissionEntity entity =
                    objectMapper.readValue(json, EndpointPermissionEntity.class);

            // then
            assertThat(entity.getServiceName()).isEqualTo("notification-service");
            assertThat(entity.getPath()).isEqualTo("/api/v1/notifications/webhook");
            assertThat(entity.getMethod()).isEqualTo("POST");
            assertThat(entity.getRequiredPermissions()).isEmpty();
            assertThat(entity.getRequiredRoles()).isEmpty();
            assertThat(entity.isPublic()).isTrue();
        }

        @Test
        @DisplayName("null 값들이 있는 JSON을 역직렬화")
        void shouldDeserializeJsonWithNullValues() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "serviceName": null,
                        "path": null,
                        "method": null,
                        "requiredPermissions": null,
                        "requiredRoles": null,
                        "isPublic": false
                    }
                    """;

            // when
            EndpointPermissionEntity entity =
                    objectMapper.readValue(json, EndpointPermissionEntity.class);

            // then
            assertThat(entity.getServiceName()).isNull();
            assertThat(entity.getPath()).isNull();
            assertThat(entity.getMethod()).isNull();
            assertThat(entity.getRequiredPermissions()).isNull();
            assertThat(entity.getRequiredRoles()).isNull();
            assertThat(entity.isPublic()).isFalse();
        }

        @Test
        @DisplayName("잘못된 JSON 형식으로 역직렬화 시 예외 발생")
        void shouldThrowExceptionForInvalidJson() {
            // given
            String invalidJson = "{ invalid json }";

            // when & then
            assertThatThrownBy(
                            () ->
                                    objectMapper.readValue(
                                            invalidJson, EndpointPermissionEntity.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("필수 필드가 누락된 JSON으로 역직렬화")
        void shouldDeserializeJsonWithMissingFields() throws JsonProcessingException {
            // given - serviceName과 isPublic만 있는 JSON
            String json =
                    """
                    {
                        "serviceName": "minimal-service",
                        "isPublic": true
                    }
                    """;

            // when
            EndpointPermissionEntity entity =
                    objectMapper.readValue(json, EndpointPermissionEntity.class);

            // then
            assertThat(entity.getServiceName()).isEqualTo("minimal-service");
            assertThat(entity.getPath()).isNull();
            assertThat(entity.getMethod()).isNull();
            assertThat(entity.getRequiredPermissions()).isNull();
            assertThat(entity.getRequiredRoles()).isNull();
            assertThat(entity.isPublic()).isTrue();
        }
    }

    @Nested
    @DisplayName("@JsonCreator 테스트")
    class JsonCreatorTest {

        @Test
        @DisplayName("@JsonCreator 어노테이션이 정상 동작")
        void shouldUseJsonCreatorCorrectly() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "serviceName": "inventory-service",
                        "path": "/api/v1/inventory/{itemId}",
                        "method": "PUT",
                        "requiredPermissions": ["inventory:update"],
                        "requiredRoles": ["INVENTORY_MANAGER"],
                        "isPublic": false
                    }
                    """;

            // when
            EndpointPermissionEntity entity =
                    objectMapper.readValue(json, EndpointPermissionEntity.class);

            // then
            assertThat(entity.getServiceName()).isEqualTo("inventory-service");
            assertThat(entity.getPath()).isEqualTo("/api/v1/inventory/{itemId}");
            assertThat(entity.getMethod()).isEqualTo("PUT");
            assertThat(entity.getRequiredPermissions()).containsExactly("inventory:update");
            assertThat(entity.getRequiredRoles()).containsExactly("INVENTORY_MANAGER");
            assertThat(entity.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPublic() 메서드 테스트")
    class IsPublicMethodTest {

        @Test
        @DisplayName("@JsonProperty 어노테이션이 isPublic 필드에 정상 적용")
        void shouldHandleIsPublicFieldCorrectly() throws JsonProcessingException {
            // given
            EndpointPermissionEntity publicEntity =
                    new EndpointPermissionEntity("test", "/test", "GET", Set.of(), Set.of(), true);
            EndpointPermissionEntity privateEntity =
                    new EndpointPermissionEntity(
                            "test", "/test", "GET", Set.of("test:read"), Set.of(), false);

            // when
            String publicJson = objectMapper.writeValueAsString(publicEntity);
            String privateJson = objectMapper.writeValueAsString(privateEntity);

            // then
            assertThat(publicJson).contains("\"isPublic\":true");
            assertThat(privateJson).contains("\"isPublic\":false");

            // 역직렬화 테스트
            EndpointPermissionEntity deserializedPublic =
                    objectMapper.readValue(publicJson, EndpointPermissionEntity.class);
            EndpointPermissionEntity deserializedPrivate =
                    objectMapper.readValue(privateJson, EndpointPermissionEntity.class);

            assertThat(deserializedPublic.isPublic()).isTrue();
            assertThat(deserializedPrivate.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString() 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString() 메서드가 올바른 형식으로 출력")
        void shouldReturnCorrectToStringFormat() {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            "test-service",
                            "/api/v1/test",
                            "POST",
                            Set.of("test:create"),
                            Set.of("TESTER"),
                            false);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("EndpointPermissionEntity{");
            assertThat(result).contains("serviceName='test-service'");
            assertThat(result).contains("path='/api/v1/test'");
            assertThat(result).contains("method='POST'");
            assertThat(result).contains("isPublic=false");
        }

        @Test
        @DisplayName("공개 엔드포인트의 toString() 출력")
        void shouldReturnCorrectToStringForPublicEndpoint() {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(
                            "public-service", "/public", "GET", Set.of(), Set.of(), true);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("EndpointPermissionEntity{");
            assertThat(result).contains("serviceName='public-service'");
            assertThat(result).contains("path='/public'");
            assertThat(result).contains("method='GET'");
            assertThat(result).contains("isPublic=true");
        }

        @Test
        @DisplayName("null 값들이 있을 때 toString() 처리")
        void shouldHandleNullValuesInToString() {
            // given
            EndpointPermissionEntity entity =
                    new EndpointPermissionEntity(null, null, null, null, null, false);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("EndpointPermissionEntity{");
            assertThat(result).contains("serviceName='null'");
            assertThat(result).contains("path='null'");
            assertThat(result).contains("method='null'");
            assertThat(result).contains("isPublic=false");
        }
    }
}
