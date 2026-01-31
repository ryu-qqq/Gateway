package com.ryuqq.gateway.adapter.in.gateway.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.domain.authentication.vo.ExpiredTokenInfo;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT Payload Parser
 *
 * <p>JWT 토큰의 payload를 서명 검증 없이 파싱합니다. 만료된 토큰에서 사용자 정보를 추출할 때 사용합니다.
 *
 * <p><strong>주의</strong>: 이 클래스는 서명을 검증하지 않습니다. 보안이 필요한 경우 별도의 JWT 검증을 수행해야 합니다.
 *
 * <p><strong>용도</strong>:
 *
 * <ul>
 *   <li>Token Refresh 시 만료된 Access Token에서 userId/tenantId 추출
 *   <li>만료 여부 확인 (exp claim 기반)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class JwtPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(JwtPayloadParser.class);

    private static final String USER_ID_CLAIM = "userId";
    private static final String TENANT_ID_CLAIM = "tenantId";
    private static final String EXP_CLAIM = "exp";

    private final ObjectMapper objectMapper;

    public JwtPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * JWT 토큰에서 만료 정보와 사용자 정보를 추출합니다.
     *
     * <p>서명 검증 없이 payload만 파싱하여 정보를 추출합니다.
     *
     * @param token JWT Access Token
     * @return ExpiredTokenInfo (만료 여부, userId, tenantId)
     * @throws JwtParseException JWT 파싱 실패 시
     */
    public ExpiredTokenInfo extractTokenInfo(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtParseException("Token is null or empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtParseException(
                    "Invalid JWT format: expected 3 parts, got " + parts.length);
        }

        try {
            String payloadJson = decodeBase64Url(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadJson);

            boolean expired = isExpired(payload);
            Long userId = extractUserId(payload);
            String tenantId = extractTenantId(payload);

            log.debug(
                    "Extracted token info: expired={}, userId={}, tenantId={}",
                    expired,
                    userId,
                    tenantId);

            return ExpiredTokenInfo.of(expired, userId, tenantId);

        } catch (JwtParseException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtParseException("Failed to parse JWT payload", e);
        }
    }

    /**
     * Base64 URL 디코딩
     *
     * @param base64Url Base64 URL 인코딩된 문자열
     * @return 디코딩된 문자열
     */
    private String decodeBase64Url(String base64Url) {
        byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param payload JWT payload
     * @return 만료 여부
     */
    private boolean isExpired(JsonNode payload) {
        JsonNode expNode = payload.get(EXP_CLAIM);
        if (expNode == null || !expNode.isNumber()) {
            log.warn("JWT does not contain valid 'exp' claim");
            return true; // exp가 없으면 만료된 것으로 처리
        }

        long expSeconds = expNode.asLong();
        long nowSeconds = Instant.now().getEpochSecond();

        return nowSeconds >= expSeconds;
    }

    /**
     * userId 추출
     *
     * @param payload JWT payload
     * @return userId (없으면 null)
     */
    private Long extractUserId(JsonNode payload) {
        JsonNode userIdNode = payload.get(USER_ID_CLAIM);
        if (userIdNode == null || userIdNode.isNull()) {
            return null;
        }

        if (userIdNode.isNumber()) {
            return userIdNode.asLong();
        }

        if (userIdNode.isTextual()) {
            try {
                return Long.parseLong(userIdNode.asText());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse userId as Long: {}", userIdNode.asText());
                return null;
            }
        }

        return null;
    }

    /**
     * tenantId 추출
     *
     * @param payload JWT payload
     * @return tenantId (없으면 null)
     */
    private String extractTenantId(JsonNode payload) {
        JsonNode tenantIdNode = payload.get(TENANT_ID_CLAIM);
        if (tenantIdNode == null || tenantIdNode.isNull()) {
            return null;
        }

        return tenantIdNode.asText();
    }

    /** JWT 파싱 예외 */
    public static class JwtParseException extends RuntimeException {
        public JwtParseException(String message) {
            super(message);
        }

        public JwtParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
