package com.ryuqq.gateway.application.authorization.port.out.client;

import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import reactor.core.publisher.Mono;

/**
 * Permission Client Port (Out)
 *
 * <p>AuthHub에서 Permission 정보를 조회하는 Outbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>AuthHub API 호출하여 Permission Spec 조회
 *   <li>AuthHub API 호출하여 사용자별 Permission Hash 조회
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>PermissionClientAdapter (adapter-out.client)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PermissionClient {

    /**
     * Permission Spec 조회 (AuthHub API)
     *
     * @return Permission Spec
     */
    Mono<PermissionSpec> fetchPermissionSpec();

    /**
     * 사용자별 Permission Hash 조회 (AuthHub API)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Permission Hash
     */
    Mono<PermissionHash> fetchUserPermissions(String tenantId, String userId);
}
