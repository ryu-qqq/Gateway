package com.ryuqq.gateway.application.ratelimit.dto.command;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;

/**
 * 실패 기록 Command DTO
 *
 * <p>로그인 실패, JWT 검증 실패 등의 실패를 기록하는 Command 객체
 *
 * <p><strong>사용 사례</strong>:
 *
 * <ul>
 *   <li>LOGIN: 로그인 실패 시 IP별 실패 횟수 증가
 *   <li>INVALID_JWT: JWT 검증 실패 시 IP별 실패 횟수 증가
 * </ul>
 *
 * @param limitType Rate Limit 타입 (LOGIN, INVALID_JWT)
 * @param identifier 식별자 (IP 주소)
 */
public record RecordFailureCommand(LimitType limitType, String identifier) {

    /** Compact Constructor - 검증 로직 */
    public RecordFailureCommand {
        if (limitType == null) {
            throw new IllegalArgumentException("LimitType cannot be null");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or blank");
        }
    }

    /**
     * 로그인 실패 기록 Command 생성
     *
     * @param ipAddress IP 주소
     * @return RecordFailureCommand
     */
    public static RecordFailureCommand forLoginFailure(String ipAddress) {
        return new RecordFailureCommand(LimitType.LOGIN, ipAddress);
    }

    /**
     * Invalid JWT 기록 Command 생성
     *
     * @param ipAddress IP 주소
     * @return RecordFailureCommand
     */
    public static RecordFailureCommand forInvalidJwt(String ipAddress) {
        return new RecordFailureCommand(LimitType.INVALID_JWT, ipAddress);
    }
}
