package com.ryuqq.gateway.application.ratelimit.dto.response;

/**
 * 차단된 IP 응답 DTO
 *
 * @param ipAddress 차단된 IP 주소
 * @param ttlSeconds 남은 차단 시간 (초)
 * @author development-team
 * @since 1.0.0
 */
public record BlockedIpResponse(String ipAddress, long ttlSeconds) {

    public static BlockedIpResponse of(String ipAddress, long ttlSeconds) {
        return new BlockedIpResponse(ipAddress, ttlSeconds);
    }
}
