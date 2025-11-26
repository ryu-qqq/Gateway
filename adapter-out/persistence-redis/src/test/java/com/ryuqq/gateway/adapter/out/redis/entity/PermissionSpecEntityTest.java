package com.ryuqq.gateway.adapter.out.redis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionSpecEntity 테스트")
class PermissionSpecEntityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 PermissionSpecEntity 생성")
        void shouldCreatePermissionSpecEntity() {
            // given
            Long version = 123L;
            Instant updatedAt = Instant.parse("2025-11-25T08:00:00Z");
            List<EndpointPermissionEntity> permissions =
                    List.of(
                            new EndpointPermissionEntity(
                                    "user-service",
                                    "/api/v1/users",
                                    "GET",
                                    Set.of("user:read"),
                                    Set.of("USER"),
                                    false),
                            new EndpointPermissionEntity(
                                    "order-service",
                                    "/api/v1/orders",
                                    "POST",
                                    Set.of("order:create"),
                                    Set.of("ADMIN"),
                                    false));

            // when
            PermissionSpecEntity entity = new PermissionSpecEntity(version, updatedAt, permissions);

            // then
            assertThat(entity.getVersion()).isEqualTo(version);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(entity.getPermissions()).isEqualTo(permissions);
        }

        @Test
        @DisplayName("null 값들로 PermissionSpecEntity 생성")
        void shouldCreatePermissionSpecEntityWithNullValues() {
            // when
            PermissionSpecEntity entity = new PermissionSpecEntity(null, null, null);

            // then
            assertThat(entity.getVersion()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getPermissions()).isNull();
        }

        @Test
        @DisplayName("빈 권한 리스트로 PermissionSpecEntity 생성")
        void shouldCreatePermissionSpecEntityWithEmptyPermissions() {
            // given
            Long version = 456L;
            Instant updatedAt = Instant.parse("2025-11-25T09:00:00Z");
            List<EndpointPermissionEntity> emptyPermissions = List.of();

            // when
            PermissionSpecEntity entity =
                    new PermissionSpecEntity(version, updatedAt, emptyPermissions);

            // then
            assertThat(entity.getVersion()).isEqualTo(version);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(entity.getPermissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("PermissionSpecEntity를 JSON으로 직렬화")
        void shouldSerializeToJson() throws JsonProcessingException {
            // given
            Long version = 789L;
            Instant updatedAt = Instant.parse("2025-11-25T10:00:00Z");
            List<EndpointPermissionEntity> permissions =
                    List.of(
                            new EndpointPermissionEntity(
                                    "test-service",
                                    "/api/v1/test",
                                    "GET",
                                    Set.of("test:read"),
                                    Set.of("USER"),
                                    false));
            PermissionSpecEntity entity = new PermissionSpecEntity(version, updatedAt, permissions);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"version\":789");
            assertThat(json).contains("\"updatedAt\":\"2025-11-25T10:00:00Z\"");
            assertThat(json).contains("\"permissions\":");
            assertThat(json).contains("\"serviceName\":\"test-service\"");
            assertThat(json).contains("\"path\":\"/api/v1/test\"");
            assertThat(json).contains("\"method\":\"GET\"");
        }

        @Test
        @DisplayName("null 값들이 있는 PermissionSpecEntity를 JSON으로 직렬화")
        void shouldSerializeEntityWithNullValues() throws JsonProcessingException {
            // given
            PermissionSpecEntity entity = new PermissionSpecEntity(null, null, null);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"version\":null");
            assertThat(json).contains("\"updatedAt\":null");
            assertThat(json).contains("\"permissions\":null");
        }

        @Test
        @DisplayName("빈 권한 리스트가 있는 PermissionSpecEntity를 JSON으로 직렬화")
        void shouldSerializeEntityWithEmptyPermissions() throws JsonProcessingException {
            // given
            Long version = 999L;
            Instant updatedAt = Instant.parse("2025-11-25T11:00:00Z");
            List<EndpointPermissionEntity> emptyPermissions = List.of();
            PermissionSpecEntity entity =
                    new PermissionSpecEntity(version, updatedAt, emptyPermissions);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"version\":999");
            assertThat(json).contains("\"updatedAt\":\"2025-11-25T11:00:00Z\"");
            assertThat(json).contains("\"permissions\":[]");
        }
    }

    @Nested
    @DisplayName("JSON 역직렬화 테스트")
    class JsonDeserializationTest {

        @Test
        @DisplayName("JSON을 PermissionSpecEntity로 역직렬화")
        void shouldDeserializeFromJson() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "version": 555,
                        "updatedAt": "2025-11-25T12:00:00Z",
                        "permissions": [
                            {
                                "serviceName": "payment-service",
                                "path": "/api/v1/payments",
                                "method": "POST",
                                "requiredPermissions": ["payment:create"],
                                "requiredRoles": ["ADMIN"],
                                "isPublic": false
                            }
                        ]
                    }
                    """;

            // when
            PermissionSpecEntity entity = objectMapper.readValue(json, PermissionSpecEntity.class);

            // then
            assertThat(entity.getVersion()).isEqualTo(555L);
            assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2025-11-25T12:00:00Z"));
            assertThat(entity.getPermissions()).hasSize(1);

            EndpointPermissionEntity permission = entity.getPermissions().get(0);
            assertThat(permission.getServiceName()).isEqualTo("payment-service");
            assertThat(permission.getPath()).isEqualTo("/api/v1/payments");
            assertThat(permission.getMethod()).isEqualTo("POST");
            assertThat(permission.getRequiredPermissions()).containsExactly("payment:create");
            assertThat(permission.getRequiredRoles()).containsExactly("ADMIN");
            assertThat(permission.isPublic()).isFalse();
        }

        @Test
        @DisplayName("null 값들이 있는 JSON을 PermissionSpecEntity로 역직렬화")
        void shouldDeserializeJsonWithNullValues() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "version": null,
                        "updatedAt": null,
                        "permissions": null
                    }
                    """;

            // when
            PermissionSpecEntity entity = objectMapper.readValue(json, PermissionSpecEntity.class);

            // then
            assertThat(entity.getVersion()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getPermissions()).isNull();
        }

        @Test
        @DisplayName("빈 권한 배열이 있는 JSON을 PermissionSpecEntity로 역직렬화")
        void shouldDeserializeJsonWithEmptyPermissions() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "version": 777,
                        "updatedAt": "2025-11-25T13:00:00Z",
                        "permissions": []
                    }
                    """;

            // when
            PermissionSpecEntity entity = objectMapper.readValue(json, PermissionSpecEntity.class);

            // then
            assertThat(entity.getVersion()).isEqualTo(777L);
            assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2025-11-25T13:00:00Z"));
            assertThat(entity.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("잘못된 JSON 형식으로 역직렬화 시 예외 발생")
        void shouldThrowExceptionForInvalidJson() {
            // given
            String invalidJson = "{ invalid json }";

            // when & then
            assertThatThrownBy(
                            () -> objectMapper.readValue(invalidJson, PermissionSpecEntity.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("필수 필드가 누락된 JSON으로 역직렬화")
        void shouldDeserializeJsonWithMissingFields() throws JsonProcessingException {
            // given - version 필드만 있는 JSON
            String json =
                    """
                    {
                        "version": 888
                    }
                    """;

            // when
            PermissionSpecEntity entity = objectMapper.readValue(json, PermissionSpecEntity.class);

            // then
            assertThat(entity.getVersion()).isEqualTo(888L);
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getPermissions()).isNull();
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
                        "version": 111,
                        "updatedAt": "2025-11-25T14:00:00Z",
                        "permissions": [
                            {
                                "serviceName": "notification-service",
                                "path": "/api/v1/notifications",
                                "method": "GET",
                                "requiredPermissions": ["notification:read"],
                                "requiredRoles": ["USER"],
                                "isPublic": true
                            }
                        ]
                    }
                    """;

            // when
            PermissionSpecEntity entity = objectMapper.readValue(json, PermissionSpecEntity.class);

            // then
            assertThat(entity.getVersion()).isEqualTo(111L);
            assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2025-11-25T14:00:00Z"));
            assertThat(entity.getPermissions()).hasSize(1);

            EndpointPermissionEntity permission = entity.getPermissions().get(0);
            assertThat(permission.getServiceName()).isEqualTo("notification-service");
            assertThat(permission.isPublic()).isTrue();
        }
    }

    @Nested
    @DisplayName("toString() 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString() 메서드가 올바른 형식으로 출력")
        void shouldReturnCorrectToStringFormat() {
            // given
            Long version = 222L;
            Instant updatedAt = Instant.parse("2025-11-25T15:00:00Z");
            List<EndpointPermissionEntity> permissions =
                    List.of(
                            new EndpointPermissionEntity(
                                    "test-service", "/test", "GET", Set.of(), Set.of(), true));
            PermissionSpecEntity entity = new PermissionSpecEntity(version, updatedAt, permissions);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PermissionSpecEntity{");
            assertThat(result).contains("version=222");
            assertThat(result).contains("updatedAt=2025-11-25T15:00:00Z");
            assertThat(result).contains("permissions=1");
        }

        @Test
        @DisplayName("null permissions일 때 toString() 처리")
        void shouldHandleNullPermissionsInToString() {
            // given
            PermissionSpecEntity entity = new PermissionSpecEntity(333L, null, null);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PermissionSpecEntity{");
            assertThat(result).contains("version=333");
            assertThat(result).contains("updatedAt=null");
            assertThat(result).contains("permissions=0");
        }
    }
}
