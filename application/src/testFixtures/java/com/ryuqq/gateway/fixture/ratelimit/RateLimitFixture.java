package com.ryuqq.gateway.fixture.ratelimit;

import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitPolicy;

/**
 * RateLimit 테스트용 Fixture (Object Mother Pattern)
 *
 * @author development-team
 * @since 1.0.0
 */
public final class RateLimitFixture {

    private static final String DEFAULT_IP = "192.168.1.1";
    private static final String DEFAULT_USER_ID = "user-123";
    private static final String DEFAULT_ADMIN_ID = "admin-456";

    private RateLimitFixture() {}

    // ============== CheckRateLimitCommand ==============

    /** IP 기반 CheckRateLimitCommand 생성 */
    public static CheckRateLimitCommand aCheckRateLimitCommandForIp() {
        return CheckRateLimitCommand.forIp(DEFAULT_IP);
    }

    /** 지정된 IP로 CheckRateLimitCommand 생성 */
    public static CheckRateLimitCommand aCheckRateLimitCommandForIp(String ipAddress) {
        return CheckRateLimitCommand.forIp(ipAddress);
    }

    /** User 기반 CheckRateLimitCommand 생성 */
    public static CheckRateLimitCommand aCheckRateLimitCommandForUser() {
        return CheckRateLimitCommand.forUser(DEFAULT_USER_ID);
    }

    /** Login 기반 CheckRateLimitCommand 생성 */
    public static CheckRateLimitCommand aCheckRateLimitCommandForLogin() {
        return CheckRateLimitCommand.forLogin(DEFAULT_IP);
    }

    /** Endpoint 기반 CheckRateLimitCommand 생성 */
    public static CheckRateLimitCommand aCheckRateLimitCommandForEndpoint(
            String path, String method) {
        return CheckRateLimitCommand.forEndpoint(path, method);
    }

    // ============== RecordFailureCommand ==============

    /** 로그인 실패 RecordFailureCommand 생성 */
    public static RecordFailureCommand aRecordFailureCommandForLogin() {
        return RecordFailureCommand.forLoginFailure(DEFAULT_IP);
    }

    /** 지정된 IP로 로그인 실패 Command 생성 */
    public static RecordFailureCommand aRecordFailureCommandForLogin(String ipAddress) {
        return RecordFailureCommand.forLoginFailure(ipAddress);
    }

    /** Invalid JWT RecordFailureCommand 생성 */
    public static RecordFailureCommand aRecordFailureCommandForInvalidJwt() {
        return RecordFailureCommand.forInvalidJwt(DEFAULT_IP);
    }

    /** 지정된 IP로 Invalid JWT Command 생성 */
    public static RecordFailureCommand aRecordFailureCommandForInvalidJwt(String ipAddress) {
        return RecordFailureCommand.forInvalidJwt(ipAddress);
    }

    // ============== ResetRateLimitCommand ==============

    /** IP 기반 ResetRateLimitCommand 생성 */
    public static ResetRateLimitCommand aResetRateLimitCommandForIp() {
        return new ResetRateLimitCommand(LimitType.IP, DEFAULT_IP, DEFAULT_ADMIN_ID);
    }

    /** 지정된 IP로 ResetRateLimitCommand 생성 */
    public static ResetRateLimitCommand aResetRateLimitCommandForIp(String ipAddress) {
        return new ResetRateLimitCommand(LimitType.IP, ipAddress, DEFAULT_ADMIN_ID);
    }

    /** Login 기반 ResetRateLimitCommand 생성 */
    public static ResetRateLimitCommand aResetRateLimitCommandForLogin() {
        return new ResetRateLimitCommand(LimitType.LOGIN, DEFAULT_IP, DEFAULT_ADMIN_ID);
    }

    /** User 기반 ResetRateLimitCommand 생성 */
    public static ResetRateLimitCommand aResetRateLimitCommandForUser() {
        return new ResetRateLimitCommand(LimitType.USER, DEFAULT_USER_ID, DEFAULT_ADMIN_ID);
    }

    // ============== RateLimitKey ==============

    /** IP 기반 RateLimitKey 생성 */
    public static RateLimitKey aRateLimitKeyForIp() {
        return RateLimitKey.of(LimitType.IP, DEFAULT_IP);
    }

    /** 지정된 타입과 식별자로 RateLimitKey 생성 */
    public static RateLimitKey aRateLimitKey(LimitType limitType, String identifier) {
        return RateLimitKey.of(limitType, identifier);
    }

    // ============== RateLimitPolicy ==============

    /** 기본 정책 생성 */
    public static RateLimitPolicy aDefaultPolicy(LimitType limitType) {
        return RateLimitPolicy.defaultPolicy(limitType);
    }

    // ============== Constants ==============

    public static String defaultIp() {
        return DEFAULT_IP;
    }

    public static String defaultUserId() {
        return DEFAULT_USER_ID;
    }

    public static String defaultAdminId() {
        return DEFAULT_ADMIN_ID;
    }
}
