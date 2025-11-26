package com.ryuqq.gateway.application.authorization.port.out.command;

import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import reactor.core.publisher.Mono;

/**
 * PermissionSpecCommandPort - Permission Spec 저장/삭제 Port
 *
 * <p>Redis Cache에 Permission Spec을 저장/삭제하는 Port입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PermissionSpecCommandPort {

    /**
     * Permission Spec 저장 (Cache)
     *
     * @param permissionSpec Permission Spec
     * @return 완료 Mono
     */
    Mono<Void> save(PermissionSpec permissionSpec);

    /**
     * Permission Spec 캐시 무효화
     *
     * @return 완료 Mono
     */
    Mono<Void> invalidate();
}
