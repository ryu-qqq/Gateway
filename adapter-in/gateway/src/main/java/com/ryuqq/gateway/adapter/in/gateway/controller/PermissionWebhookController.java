package com.ryuqq.gateway.adapter.in.gateway.controller;

import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import com.ryuqq.gateway.application.authorization.port.in.command.InvalidateUserPermissionUseCase;
import com.ryuqq.gateway.application.authorization.port.in.command.SyncPermissionSpecUseCase;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Permission Webhook Controller
 *
 * <p>AuthHub로부터 Webhook을 받아 Permission 캐시를 무효화합니다.
 *
 * <p><strong>엔드포인트</strong>:
 *
 * <ul>
 *   <li>POST /webhooks/permission/spec-sync - Permission Spec 캐시 무효화
 *   <li>POST /webhooks/permission/user-invalidate - 사용자별 Permission Hash 캐시 무효화
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/webhooks/permission")
public class PermissionWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PermissionWebhookController.class);

    private final SyncPermissionSpecUseCase syncPermissionSpecUseCase;
    private final InvalidateUserPermissionUseCase invalidateUserPermissionUseCase;

    public PermissionWebhookController(
            SyncPermissionSpecUseCase syncPermissionSpecUseCase,
            InvalidateUserPermissionUseCase invalidateUserPermissionUseCase) {
        this.syncPermissionSpecUseCase = syncPermissionSpecUseCase;
        this.invalidateUserPermissionUseCase = invalidateUserPermissionUseCase;
    }

    /**
     * Permission Spec 캐시 무효화
     *
     * <p>AuthHub에서 Permission Spec이 변경되었을 때 호출됩니다.
     *
     * @param request Spec 동기화 요청
     * @return Mono&lt;ResponseEntity&lt;Void&gt;&gt; 성공 시 200 OK
     */
    @PostMapping("/spec-sync")
    public Mono<ResponseEntity<Void>> syncPermissionSpec(@RequestBody SpecSyncRequest request) {
        log.info(
                "Received permission spec sync webhook: version={}, changedServices={}",
                request.version(),
                request.changedServices());

        SyncPermissionSpecCommand command =
                SyncPermissionSpecCommand.of(request.version(), request.changedServices());

        return syncPermissionSpecUseCase
                .execute(command)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(
                        e -> {
                            log.error("Failed to sync permission spec: {}", e.getMessage());
                            return Mono.just(ResponseEntity.internalServerError().<Void>build());
                        });
    }

    /**
     * 사용자별 Permission Hash 캐시 무효화
     *
     * <p>AuthHub에서 사용자 권한이 변경되었을 때 호출됩니다.
     *
     * @param request 사용자 권한 무효화 요청
     * @return Mono&lt;ResponseEntity&lt;Void&gt;&gt; 성공 시 200 OK
     */
    @PostMapping("/user-invalidate")
    public Mono<ResponseEntity<Void>> invalidateUserPermission(
            @RequestBody UserInvalidateRequest request) {
        log.info(
                "Received user permission invalidate webhook: tenantId={}, userId={}",
                request.tenantId(),
                request.userId());

        InvalidateUserPermissionCommand command =
                InvalidateUserPermissionCommand.of(request.tenantId(), request.userId());

        return invalidateUserPermissionUseCase
                .execute(command)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Failed to invalidate user permission: tenantId={}, userId={},"
                                            + " error={}",
                                    request.tenantId(),
                                    request.userId(),
                                    e.getMessage());
                            return Mono.just(ResponseEntity.internalServerError().<Void>build());
                        });
    }

    /** Spec 동기화 요청 DTO */
    public record SpecSyncRequest(Long version, List<String> changedServices) {}

    /** 사용자 권한 무효화 요청 DTO */
    public record UserInvalidateRequest(String tenantId, String userId) {}
}
