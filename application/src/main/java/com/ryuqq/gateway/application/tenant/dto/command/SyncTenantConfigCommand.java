package com.ryuqq.gateway.application.tenant.dto.command;

/**
 * Tenant Config 동기화 Command DTO
 *
 * <p>Webhook을 통한 Tenant Config 캐시 무효화를 위한 Command 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>tenantId는 null 또는 blank일 수 없다
 * </ul>
 *
 * <p><strong>사용 시나리오</strong>:
 *
 * <ol>
 *   <li>AuthHub에서 Tenant Config 변경
 *   <li>Webhook으로 Gateway에 알림 전송
 *   <li>Gateway에서 Redis 캐시 무효화
 * </ol>
 *
 * @param tenantId 동기화할 테넌트 ID (null/blank 불가)
 */
public record SyncTenantConfigCommand(String tenantId) {

    /** Compact Constructor - 검증 로직 */
    public SyncTenantConfigCommand {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
    }
}
