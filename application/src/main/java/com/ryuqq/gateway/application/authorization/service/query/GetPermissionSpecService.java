package com.ryuqq.gateway.application.authorization.service.query;

import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionSpecCommandPort;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionSpecQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * GetPermissionSpecService - Permission Spec 조회 Service
 *
 * <p>Redis 캐시에서 Permission Spec을 조회하고, 없으면 AuthHub에서 가져와 캐시합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPermissionSpecService {

    private static final Logger log = LoggerFactory.getLogger(GetPermissionSpecService.class);

    private final PermissionSpecQueryPort permissionSpecQueryPort;
    private final PermissionSpecCommandPort permissionSpecCommandPort;
    private final AuthHubPermissionClient authHubPermissionClient;

    public GetPermissionSpecService(
            PermissionSpecQueryPort permissionSpecQueryPort,
            PermissionSpecCommandPort permissionSpecCommandPort,
            AuthHubPermissionClient authHubPermissionClient) {
        this.permissionSpecQueryPort = permissionSpecQueryPort;
        this.permissionSpecCommandPort = permissionSpecCommandPort;
        this.authHubPermissionClient = authHubPermissionClient;
    }

    /**
     * Permission Spec 조회 (Cache-aside 패턴)
     *
     * @return Permission Spec
     */
    public Mono<PermissionSpec> getPermissionSpec() {
        return permissionSpecQueryPort
                .findPermissionSpec()
                .doOnNext(spec -> log.debug("Permission spec found in cache"))
                .switchIfEmpty(Mono.defer(this::fetchAndCachePermissionSpec));
    }

    private Mono<PermissionSpec> fetchAndCachePermissionSpec() {
        log.info("Permission spec not found in cache, fetching from AuthHub");

        return authHubPermissionClient
                .fetchPermissionSpec()
                .flatMap(
                        spec ->
                                permissionSpecCommandPort
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
