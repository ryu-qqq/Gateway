package com.ryuqq.gateway.adapter.in.gateway.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.domain.authentication.vo.ExpiredTokenInfo;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JwtPayloadParser 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("JwtPayloadParser 테스트")
class JwtPayloadParserTest {

    private JwtPayloadParser jwtPayloadParser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtPayloadParser = new JwtPayloadParser(objectMapper);
    }

    @Nested
    @DisplayName("extractTokenInfo 메서드")
    class ExtractTokenInfoTest {

        @Test
        @DisplayName("만료된 토큰에서 정보를 추출한다")
        void shouldExtractInfoFromExpiredToken() {
            // given
            long expiredTime = Instant.now().getEpochSecond() - 3600; // 1시간 전 만료
            String token = createTestToken(123L, "tenant-001", expiredTime);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.isExpired()).isTrue();
            assertThat(result.userId()).isEqualTo(123L);
            assertThat(result.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("유효한 토큰에서 정보를 추출한다")
        void shouldExtractInfoFromValidToken() {
            // given
            long futureTime = Instant.now().getEpochSecond() + 3600; // 1시간 후 만료
            String token = createTestToken(456L, "tenant-002", futureTime);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.isExpired()).isFalse();
            assertThat(result.userId()).isEqualTo(456L);
            assertThat(result.tenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("userId가 문자열일 때도 파싱한다")
        void shouldParseStringUserId() {
            // given
            String payload =
                    "{\"userId\":\"789\",\"tenantId\":\"tenant-003\",\"exp\":"
                            + (Instant.now().getEpochSecond() - 100)
                            + "}";
            String token = createTokenWithPayload(payload);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.userId()).isEqualTo(789L);
            assertThat(result.tenantId()).isEqualTo("tenant-003");
        }

        @Test
        @DisplayName("userId가 없으면 null을 반환한다")
        void shouldReturnNullWhenUserIdMissing() {
            // given
            String payload =
                    "{\"tenantId\":\"tenant-004\",\"exp\":"
                            + (Instant.now().getEpochSecond() + 100)
                            + "}";
            String token = createTokenWithPayload(payload);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.userId()).isNull();
            assertThat(result.tenantId()).isEqualTo("tenant-004");
        }

        @Test
        @DisplayName("tenantId가 없으면 null을 반환한다")
        void shouldReturnNullWhenTenantIdMissing() {
            // given
            String payload =
                    "{\"userId\":999,\"exp\":" + (Instant.now().getEpochSecond() + 100) + "}";
            String token = createTokenWithPayload(payload);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.userId()).isEqualTo(999L);
            assertThat(result.tenantId()).isNull();
        }

        @Test
        @DisplayName("exp가 없으면 만료된 것으로 처리한다")
        void shouldTreatAsExpiredWhenExpMissing() {
            // given
            String payload = "{\"userId\":111,\"tenantId\":\"tenant-005\"}";
            String token = createTokenWithPayload(payload);

            // when
            ExpiredTokenInfo result = jwtPayloadParser.extractTokenInfo(token);

            // then
            assertThat(result.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionTest {

        @Test
        @DisplayName("토큰이 null이면 예외가 발생한다")
        void shouldThrowExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> jwtPayloadParser.extractTokenInfo(null))
                    .isInstanceOf(JwtPayloadParser.JwtParseException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("토큰이 빈 문자열이면 예외가 발생한다")
        void shouldThrowExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> jwtPayloadParser.extractTokenInfo(""))
                    .isInstanceOf(JwtPayloadParser.JwtParseException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("토큰 형식이 잘못되면 예외가 발생한다")
        void shouldThrowExceptionWhenTokenFormatInvalid() {
            assertThatThrownBy(() -> jwtPayloadParser.extractTokenInfo("invalid.token"))
                    .isInstanceOf(JwtPayloadParser.JwtParseException.class)
                    .hasMessageContaining("Invalid JWT format");
        }

        @Test
        @DisplayName("payload가 유효하지 않은 JSON이면 예외가 발생한다")
        void shouldThrowExceptionWhenPayloadInvalidJson() {
            // given
            String invalidPayload =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
            String token = "header." + invalidPayload + ".signature";

            // when/then
            assertThatThrownBy(() -> jwtPayloadParser.extractTokenInfo(token))
                    .isInstanceOf(JwtPayloadParser.JwtParseException.class)
                    .hasMessageContaining("Failed to parse");
        }
    }

    // ===============================================
    // Helper Methods
    // ===============================================

    private String createTestToken(Long userId, String tenantId, long exp) {
        String payload =
                String.format(
                        "{\"userId\":%d,\"tenantId\":\"%s\",\"exp\":%d}", userId, tenantId, exp);
        return createTokenWithPayload(payload);
    }

    private String createTokenWithPayload(String payload) {
        String header =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String encodedPayload =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));

        return header + "." + encodedPayload + "." + signature;
    }
}
