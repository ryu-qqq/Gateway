package com.ryuqq.gateway.adapter.out.redis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionHashEntity 테스트")
class PermissionHashEntityTest {

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
        @DisplayName("정상적인 PermissionHashEntity 생성")
        void shouldCreatePermissionHashEntity() {
            // given
            String hash = "abc123def456";
            Set<String> permissions = Set.of("user:read", "user:write", "order:read");
            Set<String> roles = Set.of("USER", "ADMIN");
            Instant generatedAt = Instant.parse("2025-11-25T08:00:00Z");

            // when
            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, permissions, roles, generatedAt);

            // then
            assertThat(entity.getHash()).isEqualTo(hash);
            assertThat(entity.getPermissions()).isEqualTo(permissions);
            assertThat(entity.getRoles()).isEqualTo(roles);
            assertThat(entity.getGeneratedAt()).isEqualTo(generatedAt);
        }

        @Test
        @DisplayName("null 값들로 PermissionHashEntity 생성")
        void shouldCreatePermissionHashEntityWithNullValues() {
            // when
            PermissionHashEntity entity = new PermissionHashEntity(null, null, null, null);

            // then
            assertThat(entity.getHash()).isNull();
            assertThat(entity.getPermissions()).isNull();
            assertThat(entity.getRoles()).isNull();
            assertThat(entity.getGeneratedAt()).isNull();
        }

        @Test
        @DisplayName("빈 Set으로 PermissionHashEntity 생성")
        void shouldCreatePermissionHashEntityWithEmptySets() {
            // given
            String hash = "empty123";
            Set<String> emptyPermissions = Set.of();
            Set<String> emptyRoles = Set.of();
            Instant generatedAt = Instant.parse("2025-11-25T09:00:00Z");

            // when
            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, emptyPermissions, emptyRoles, generatedAt);

            // then
            assertThat(entity.getHash()).isEqualTo(hash);
            assertThat(entity.getPermissions()).isEmpty();
            assertThat(entity.getRoles()).isEmpty();
            assertThat(entity.getGeneratedAt()).isEqualTo(generatedAt);
        }
    }

    @Nested
    @DisplayName("JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("PermissionHashEntity를 JSON으로 직렬화")
        void shouldSerializeToJson() throws JsonProcessingException {
            // given
            String hash = "serialize123";
            Set<String> permissions = Set.of("test:read", "test:write");
            Set<String> roles = Set.of("TESTER");
            Instant generatedAt = Instant.parse("2025-11-25T10:00:00Z");
            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, permissions, roles, generatedAt);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"hash\":\"serialize123\"");
            assertThat(json).contains("\"generatedAt\":\"2025-11-25T10:00:00Z\"");
            assertThat(json).contains("\"permissions\":");
            assertThat(json).contains("\"roles\":");
            assertThat(json).contains("\"test:read\"");
            assertThat(json).contains("\"test:write\"");
            assertThat(json).contains("\"TESTER\"");
        }

        @Test
        @DisplayName("null 값들이 있는 PermissionHashEntity를 JSON으로 직렬화")
        void shouldSerializeEntityWithNullValues() throws JsonProcessingException {
            // given
            PermissionHashEntity entity = new PermissionHashEntity(null, null, null, null);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"hash\":null");
            assertThat(json).contains("\"permissions\":null");
            assertThat(json).contains("\"roles\":null");
            assertThat(json).contains("\"generatedAt\":null");
        }

        @Test
        @DisplayName("빈 Set이 있는 PermissionHashEntity를 JSON으로 직렬화")
        void shouldSerializeEntityWithEmptySets() throws JsonProcessingException {
            // given
            String hash = "empty456";
            Set<String> emptyPermissions = Set.of();
            Set<String> emptyRoles = Set.of();
            Instant generatedAt = Instant.parse("2025-11-25T11:00:00Z");
            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, emptyPermissions, emptyRoles, generatedAt);

            // when
            String json = objectMapper.writeValueAsString(entity);

            // then
            assertThat(json).contains("\"hash\":\"empty456\"");
            assertThat(json).contains("\"permissions\":[]");
            assertThat(json).contains("\"roles\":[]");
            assertThat(json).contains("\"generatedAt\":\"2025-11-25T11:00:00Z\"");
        }
    }

    @Nested
    @DisplayName("JSON 역직렬화 테스트")
    class JsonDeserializationTest {

        @Test
        @DisplayName("JSON을 PermissionHashEntity로 역직렬화")
        void shouldDeserializeFromJson() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "hash": "deserialize789",
                        "permissions": ["payment:create", "payment:read"],
                        "roles": ["ADMIN", "PAYMENT_MANAGER"],
                        "generatedAt": "2025-11-25T12:00:00Z"
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isEqualTo("deserialize789");
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrder("payment:create", "payment:read");
            assertThat(entity.getRoles()).containsExactlyInAnyOrder("ADMIN", "PAYMENT_MANAGER");
            assertThat(entity.getGeneratedAt()).isEqualTo(Instant.parse("2025-11-25T12:00:00Z"));
        }

        @Test
        @DisplayName("null 값들이 있는 JSON을 PermissionHashEntity로 역직렬화")
        void shouldDeserializeJsonWithNullValues() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "hash": null,
                        "permissions": null,
                        "roles": null,
                        "generatedAt": null
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isNull();
            assertThat(entity.getPermissions()).isNull();
            assertThat(entity.getRoles()).isNull();
            assertThat(entity.getGeneratedAt()).isNull();
        }

        @Test
        @DisplayName("빈 배열이 있는 JSON을 PermissionHashEntity로 역직렬화")
        void shouldDeserializeJsonWithEmptyArrays() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "hash": "empty999",
                        "permissions": [],
                        "roles": [],
                        "generatedAt": "2025-11-25T13:00:00Z"
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isEqualTo("empty999");
            assertThat(entity.getPermissions()).isEmpty();
            assertThat(entity.getRoles()).isEmpty();
            assertThat(entity.getGeneratedAt()).isEqualTo(Instant.parse("2025-11-25T13:00:00Z"));
        }

        @Test
        @DisplayName("잘못된 JSON 형식으로 역직렬화 시 예외 발생")
        void shouldThrowExceptionForInvalidJson() {
            // given
            String invalidJson = "{ invalid json }";

            // when & then
            assertThatThrownBy(
                            () -> objectMapper.readValue(invalidJson, PermissionHashEntity.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("필수 필드가 누락된 JSON으로 역직렬화")
        void shouldDeserializeJsonWithMissingFields() throws JsonProcessingException {
            // given - hash 필드만 있는 JSON
            String json =
                    """
                    {
                        "hash": "partial111"
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isEqualTo("partial111");
            assertThat(entity.getPermissions()).isNull();
            assertThat(entity.getRoles()).isNull();
            assertThat(entity.getGeneratedAt()).isNull();
        }

        @Test
        @DisplayName("단일 권한과 역할이 있는 JSON 역직렬화")
        void shouldDeserializeJsonWithSinglePermissionAndRole() throws JsonProcessingException {
            // given
            String json =
                    """
                    {
                        "hash": "single222",
                        "permissions": ["single:permission"],
                        "roles": ["SINGLE_ROLE"],
                        "generatedAt": "2025-11-25T14:00:00Z"
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isEqualTo("single222");
            assertThat(entity.getPermissions()).containsExactly("single:permission");
            assertThat(entity.getRoles()).containsExactly("SINGLE_ROLE");
            assertThat(entity.getGeneratedAt()).isEqualTo(Instant.parse("2025-11-25T14:00:00Z"));
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
                        "hash": "creator333",
                        "permissions": ["creator:read", "creator:write"],
                        "roles": ["CREATOR"],
                        "generatedAt": "2025-11-25T15:00:00Z"
                    }
                    """;

            // when
            PermissionHashEntity entity = objectMapper.readValue(json, PermissionHashEntity.class);

            // then
            assertThat(entity.getHash()).isEqualTo("creator333");
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrder("creator:read", "creator:write");
            assertThat(entity.getRoles()).containsExactly("CREATOR");
            assertThat(entity.getGeneratedAt()).isEqualTo(Instant.parse("2025-11-25T15:00:00Z"));
        }
    }

    @Nested
    @DisplayName("toString() 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString() 메서드가 올바른 형식으로 출력")
        void shouldReturnCorrectToStringFormat() {
            // given
            String hash = "toString444";
            Set<String> permissions = Set.of("test:read", "test:write");
            Set<String> roles = Set.of("USER", "ADMIN");
            Instant generatedAt = Instant.parse("2025-11-25T16:00:00Z");
            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, permissions, roles, generatedAt);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PermissionHashEntity{");
            assertThat(result).contains("hash='toString444'");
            assertThat(result).contains("permissions=2");
            assertThat(result).contains("roles=2");
            assertThat(result).contains("generatedAt=2025-11-25T16:00:00Z");
        }

        @Test
        @DisplayName("null Set일 때 toString() 처리")
        void shouldHandleNullSetsInToString() {
            // given
            PermissionHashEntity entity = new PermissionHashEntity("nullTest555", null, null, null);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PermissionHashEntity{");
            assertThat(result).contains("hash='nullTest555'");
            assertThat(result).contains("permissions=0");
            assertThat(result).contains("roles=0");
            assertThat(result).contains("generatedAt=null");
        }

        @Test
        @DisplayName("빈 Set일 때 toString() 처리")
        void shouldHandleEmptySetsInToString() {
            // given
            Set<String> emptyPermissions = Set.of();
            Set<String> emptyRoles = Set.of();
            Instant generatedAt = Instant.parse("2025-11-25T17:00:00Z");
            PermissionHashEntity entity =
                    new PermissionHashEntity(
                            "emptyTest666", emptyPermissions, emptyRoles, generatedAt);

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PermissionHashEntity{");
            assertThat(result).contains("hash='emptyTest666'");
            assertThat(result).contains("permissions=0");
            assertThat(result).contains("roles=0");
            assertThat(result).contains("generatedAt=2025-11-25T17:00:00Z");
        }
    }
}
