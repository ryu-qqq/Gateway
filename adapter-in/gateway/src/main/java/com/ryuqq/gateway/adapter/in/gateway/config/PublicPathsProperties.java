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
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "gateway.routing")
public class PublicPathsProperties {

    /** 기본 Public 경로 (항상 포함) */
    private static final List<String> DEFAULT_PUBLIC_PATHS = List.of("/actuator/**");

    private List<ServiceConfig> services = new ArrayList<>();

    public List<ServiceConfig> getServices() {
        return services;
    }

    public void setServices(List<ServiceConfig> services) {
        this.services = services;
    }

    /**
     * 모든 서비스의 public-paths를 통합하여 반환
     *
     * @return 통합된 public path 목록
     */
    public List<String> getAllPublicPaths() {
        List<String> allPaths = new ArrayList<>(DEFAULT_PUBLIC_PATHS);

        for (ServiceConfig service : services) {
            allPaths.addAll(service.getPublicPaths());
        }

        return allPaths;
    }

    /** Service Configuration for extracting public-paths */
    public static class ServiceConfig {

        private String id;
        private List<String> publicPaths = new ArrayList<>();

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
    }
}
