package com.ryuqq.gateway.application.ratelimit.dto.command;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;

/**
 * Rate Limit 체크 Command DTO
 *
 * <p>Rate Limit 체크 요청을 나타내는 불변 Command 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>limitType은 null일 수 없다
 *   <li>identifier는 null 또는 blank일 수 없다
 * </ul>
 *
 * @param limitType Rate Limit 타입 (ENDPOINT, USER, IP, OTP 등)
 * @param identifier 식별자 (IP 주소, 사용자 ID, 엔드포인트 등)
 * @param additionalKeyParts 추가 키 구성 요소 (엔드포인트의 경우 method 등)
 */
public record CheckRateLimitCommand(
        LimitType limitType, String identifier, String... additionalKeyParts) {

    public CheckRateLimitCommand {
        if (additionalKeyParts == null) {
            additionalKeyParts = new String[0];
        }
    }

    /**
     * Endpoint Rate Limit Command 생성
     *
     * @param path 엔드포인트 경로
     * @param method HTTP 메서드
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forEndpoint(String path, String method) {
        return new CheckRateLimitCommand(LimitType.ENDPOINT, path, method);
    }

    /**
     * User Rate Limit Command 생성
     *
     * @param userId 사용자 ID
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forUser(String userId) {
        return new CheckRateLimitCommand(LimitType.USER, userId);
    }

    /**
     * IP Rate Limit Command 생성
     *
     * @param ipAddress IP 주소
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forIp(String ipAddress) {
        return new CheckRateLimitCommand(LimitType.IP, ipAddress);
    }

    /**
     * OTP Rate Limit Command 생성
     *
     * @param phoneNumber 전화번호
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forOtp(String phoneNumber) {
        return new CheckRateLimitCommand(LimitType.OTP, phoneNumber);
    }

    /**
     * Login Rate Limit Command 생성
     *
     * @param ipAddress IP 주소
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forLogin(String ipAddress) {
        return new CheckRateLimitCommand(LimitType.LOGIN, ipAddress);
    }

    /**
     * Token Refresh Rate Limit Command 생성
     *
     * @param userId 사용자 ID
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forTokenRefresh(String userId) {
        return new CheckRateLimitCommand(LimitType.TOKEN_REFRESH, userId);
    }

    /**
     * Invalid JWT Rate Limit Command 생성
     *
     * @param ipAddress IP 주소
     * @return CheckRateLimitCommand
     */
    public static CheckRateLimitCommand forInvalidJwt(String ipAddress) {
        return new CheckRateLimitCommand(LimitType.INVALID_JWT, ipAddress);
    }
}
