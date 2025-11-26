package com.ryuqq.gateway.application.authorization.port.in.command;

import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import reactor.core.publisher.Mono;

/**
 * InvalidateUserPermissionUseCase - 사용자 권한 캐시 무효화 UseCase
 *
 * <p>AuthHub로부터 Webhook을 받아 사용자별 Permission Hash 캐시를 무효화합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface InvalidateUserPermissionUseCase {

    /**
     * 사용자 권한 캐시 무효화
     *
     * @param command 무효화 요청
     * @return 완료 Mono
     */
    Mono<Void> execute(InvalidateUserPermissionCommand command);
}
