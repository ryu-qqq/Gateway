package com.ryuqq.gateway.application.authorization.port.out.query;

import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import reactor.core.publisher.Mono;

/**
 * PermissionSpecQueryPort - Permission Spec 조회 Port
 *
 * <p>Redis Cache에서 Permission Spec을 조회하는 Port입니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PermissionSpecQueryPort {

    /**
     * Permission Spec 조회 (Cache)
     *
     * @return Permission Spec (Cache Miss 시 empty)
     */
    Mono<PermissionSpec> findPermissionSpec();
}
