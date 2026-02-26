package com.ryuqq.gateway.application.authorization.internal;

import com.ryuqq.gateway.application.authorization.manager.PermissionClientManager;
import com.ryuqq.gateway.application.authorization.manager.PermissionSpecCommandManager;
import com.ryuqq.gateway.application.authorization.manager.PermissionSpecQueryManager;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Spec Coordinator (Reactive)
 *
 * <p>Redis Cache + AuthHub Fallback 전략을 조율하는 Coordinator
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>PermissionSpecQueryManager로 Redis Cache 조회
 *   <li>Cache Miss 시 PermissionClientManager로 AuthHub API 호출
 *   <li>조회된 Permission Spec을 PermissionSpecCommandManager로 Redis에 저장
 * </ol>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionSpecQueryManager - Redis Cache 조회
 *   <li>PermissionClientManager - AuthHub API 호출 (Cache Miss Fallback)
 *   <li>PermissionSpecCommandManager - Redis Cache 저장
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PermissionSpecCoordinator.class);

    private final PermissionSpecQueryManager permissionSpecQueryManager;
    private final PermissionClientManager permissionClientManager;
    private final PermissionSpecCommandManager permissionSpecCommandManager;

    public PermissionSpecCoordinator(
            PermissionSpecQueryManager permissionSpecQueryManager,
            PermissionClientManager permissionClientManager,
            PermissionSpecCommandManager permissionSpecCommandManager) {
        this.permissionSpecQueryManager = permissionSpecQueryManager;
        this.permissionClientManager = permissionClientManager;
        this.permissionSpecCommandManager = permissionSpecCommandManager;
    }

    /**
     * Permission Spec 조회 (Cache Hit/Miss 전략)
     *
     * <p>Redis Cache에서 먼저 조회하고, Cache Miss 시 AuthHub API를 호출합니다.
     *
     * @return Mono&lt;PermissionSpec&gt;
     */
    public Mono<PermissionSpec> findPermissionSpec() {
        return permissionSpecQueryManager
                .findPermissionSpec()
                .doOnNext(spec -> log.debug("Permission spec found in cache"))
                .switchIfEmpty(Mono.defer(this::fetchFromAuthHubAndCache));
    }

    /**
     * AuthHub에서 Permission Spec 조회 후 Redis에 캐싱
     *
     * @return Mono&lt;PermissionSpec&gt;
     */
    private Mono<PermissionSpec> fetchFromAuthHubAndCache() {
        log.info("Permission spec not found in cache, fetching from AuthHub");

        return permissionClientManager
                .fetchPermissionSpec()
                .flatMap(
                        spec ->
                                permissionSpecCommandManager
                                        .save(spec)
                                        .thenReturn(spec)
                                        .doOnSuccess(
                                                s ->
                                                        log.info(
                                                                "Permission spec cached: {}"
                                                                        + " endpoints",
                                                                s.permissions().size())))
                .doOnError(
                        e ->
                                log.error(
                                        "Failed to fetch permission spec from AuthHub: {}",
                                        e.getMessage()));
    }
}
