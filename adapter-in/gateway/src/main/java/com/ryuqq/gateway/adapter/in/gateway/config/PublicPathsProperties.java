package com.ryuqq.gateway.adapter.in.gateway.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Public Paths Configuration Properties
 *
 * <p>JWT 인증이 필요하지 않은 Public 경로 설정을 관리합니다.
 *
 * <p>설정은 gateway.routing.services[*].public-paths에서 수집됩니다.
 *
 * <p><strong>Host 기반 서비스 처리:</strong>
 *
 * <ul>
 *   <li>hosts가 정의된 서비스의 public-paths는 해당 host 요청에만 적용됩니다.
 *   <li>hosts가 없는 서비스의 public-paths는 전역으로 적용됩니다.
 *   <li>이를 통해 legacy-web의 /** public-paths가 다른 서비스에 영향을 주지 않습니다.
 * </ul>
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "gateway.routing")
public class PublicPathsProperties {

    /** 기본 Public 경로 (항상 포함) */
    private static final List<String> DEFAULT_PUBLIC_PATHS =
            List.of("/actuator/**", "/**/system/**");

    private List<ServiceConfig> services = new ArrayList<>();

    public List<ServiceConfig> getServices() {
        return services;
    }

    public void setServices(List<ServiceConfig> services) {
        this.services = services;
    }

    /**
     * Host가 정의되지 않은 서비스의 public-paths만 통합하여 반환
     *
     * <p>Host 기반 서비스(legacy-web, legacy-admin 등)의 public-paths는 제외됩니다. 이 서비스들의 public-paths는 해당
     * host 요청에서만 적용되어야 합니다.
     *
     * @return 전역 public path 목록 (host 기반 서비스 제외)
     */
    public List<String> getAllPublicPaths() {
        List<String> allPaths = new ArrayList<>(DEFAULT_PUBLIC_PATHS);

        for (ServiceConfig service : services) {
            // Host가 정의된 서비스는 전역 public-paths에서 제외
            if (!service.hasHosts()) {
                allPaths.addAll(service.getPublicPaths());
            }
        }

        return allPaths;
    }

    /**
     * 특정 Host에 해당하는 서비스의 public-paths 반환
     *
     * @param host 요청 Host 헤더 값
     * @return 해당 host에 적용되는 public path 목록
     */
    public List<String> getPublicPathsForHost(String host) {
        if (host == null || host.isEmpty()) {
            return Collections.emptyList();
        }

        for (ServiceConfig service : services) {
            if (service.matchesHost(host)) {
                return service.getPublicPaths();
            }
        }

        return Collections.emptyList();
    }

    /** Service Configuration for extracting public-paths */
    public static class ServiceConfig {

        private String id;
        private List<String> publicPaths = new ArrayList<>();
        private List<String> hosts = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getPublicPaths() {
            return Collections.unmodifiableList(publicPaths);
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths =
                    publicPaths == null ? new ArrayList<>() : new ArrayList<>(publicPaths);
        }

        public List<String> getHosts() {
            return Collections.unmodifiableList(hosts);
        }

        public void setHosts(List<String> hosts) {
            this.hosts = hosts == null ? new ArrayList<>() : new ArrayList<>(hosts);
        }

        /**
         * Host가 정의되어 있는지 확인
         *
         * @return hosts 리스트가 비어있지 않으면 true
         */
        public boolean hasHosts() {
            return hosts != null && !hosts.isEmpty();
        }

        /**
         * 주어진 host가 이 서비스에 매칭되는지 확인
         *
         * @param host 확인할 host
         * @return 매칭되면 true
         */
        public boolean matchesHost(String host) {
            if (hosts == null || hosts.isEmpty()) {
                return false;
            }
            return hosts.stream().anyMatch(h -> h.equalsIgnoreCase(host));
        }
    }
}
