package com.ryuqq.gateway.application.ratelimit.dto.command;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;

/**
 * Rate Limit 리셋 Command DTO
 *
 * <p>Rate Limit 카운터를 리셋하는 Command 객체 (Admin 전용)
 *
 * <p><strong>사용 사례</strong>:
 *
 * <ul>
 *   <li>차단된 IP 해제
 *   <li>잠금된 계정 해제
 *   <li>Rate Limit 카운터 초기화
 * </ul>
 *
 * @param limitType Rate Limit 타입
 * @param identifier 식별자
 * @param adminId 관리자 ID (감사 로그용)
 */
public record ResetRateLimitCommand(LimitType limitType, String identifier, String adminId) {}
