package com.ryuqq.gateway.adapter.in.gateway.controller;

import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.port.in.command.SyncTenantConfigUseCase;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Tenant Config Webhook Controller
 *
 * <p>AuthHub로부터 Webhook을 받아 Tenant Config 캐시를 무효화합니다.
 *
 * <p><strong>엔드포인트</strong>:
 *
 * <ul>
 *   <li>POST /internal/gateway/tenants/config-changed - Tenant Config 캐시 무효화
 * </ul>
 *
 * <p><strong>보안</strong>:
 *
 * <ul>
 *   <li>Internal API (IP Whitelist 필요)
 *   <li>AuthHub에서만 호출 가능
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/gateway/tenants")
public class TenantConfigWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TenantConfigWebhookController.class);

    private final SyncTenantConfigUseCase syncTenantConfigUseCase;

    public TenantConfigWebhookController(SyncTenantConfigUseCase syncTenantConfigUseCase) {
        this.syncTenantConfigUseCase = syncTenantConfigUseCase;
    }

    /**
     * Tenant Config 변경 알림 수신
     *
     * <p>AuthHub에서 Tenant Config이 변경되었을 때 호출됩니다.
     *
     * <p>Redis 캐시를 삭제하여 다음 요청 시 AuthHub API로부터 새로운 Config를 가져오도록 합니다.
     *
     * @param event Tenant Config 변경 이벤트
     * @return Mono&lt;ResponseEntity&lt;Void&gt;&gt; 성공 시 200 OK
     */
    @PostMapping("/config-changed")
    public Mono<ResponseEntity<Void>> handleTenantConfigChanged(
            @RequestBody TenantConfigChangedEvent event) {
        log.info(
                "Received tenant config change webhook: tenantId={}, timestamp={}",
                event.tenantId(),
                event.timestamp());

        SyncTenantConfigCommand command = new SyncTenantConfigCommand(event.tenantId());

        return syncTenantConfigUseCase
                .execute(command)
                .map(
                        response -> {
                            if (response.success()) {
                                log.info(
                                        "Tenant config cache invalidated successfully: tenantId={}",
                                        event.tenantId());
                                return ResponseEntity.ok().<Void>build();
                            }
                            log.warn(
                                    "Tenant config cache invalidation returned false: tenantId={}",
                                    event.tenantId());
                            return ResponseEntity.internalServerError().<Void>build();
                        })
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Failed to invalidate tenant config cache: tenantId={},"
                                            + " error={}",
                                    event.tenantId(),
                                    e.getMessage());
                            return Mono.just(ResponseEntity.internalServerError().<Void>build());
                        });
    }

    /**
     * Tenant Config 변경 이벤트 DTO
     *
     * <p>AuthHub로부터 전송되는 Webhook Payload
     *
     * @param tenantId 변경된 테넌트 ID
     * @param timestamp 변경 시각
     */
    public record TenantConfigChangedEvent(String tenantId, Instant timestamp) {}
}
