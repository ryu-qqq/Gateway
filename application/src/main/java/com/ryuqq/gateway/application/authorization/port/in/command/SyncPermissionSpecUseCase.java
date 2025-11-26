package com.ryuqq.gateway.application.authorization.port.in.command;

import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import reactor.core.publisher.Mono;

/**
 * SyncPermissionSpecUseCase - Permission Spec 동기화 UseCase
 *
 * <p>AuthHub로부터 Webhook을 받아 Permission Spec 캐시를 무효화합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface SyncPermissionSpecUseCase {

    /**
     * Permission Spec 동기화 (캐시 무효화)
     *
     * @param command 동기화 요청
     * @return 완료 Mono
     */
    Mono<Void> execute(SyncPermissionSpecCommand command);
}
