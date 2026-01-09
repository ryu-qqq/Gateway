package com.ryuqq.gateway.bootstrap.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Routing Configuration with AWS Cloud Map Service Discovery
 *
 * <p>Convention-based routing: /api/v1/{service}/** → {service}.{namespace}:{port}
 *
 * <p><strong>Routing Strategy:</strong>
 *
 * <ul>
 *   <li>Path prefix determines target service
 *   <li>AWS Cloud Map resolves service name to ECS Task IPs
 *   <li>Local: static URI mapping via configuration
 *   <li>Production: DNS-based discovery with Cloud Map
 * </ul>
 *
 * <p><strong>Configuration Example:</strong>
 *
 * <pre>{@code
 * gateway:
 *   routing:
 *     discovery:
 *       enabled: true
 *       namespace: connectly.local
 *       default-port: 8080
 *     services:
 *       - id: authhub
 *         port: 9090
 *         paths:
 *           - /api/v1/auth/**
 * }</pre>
 *
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(GatewayRoutingConfig.GatewayRoutingProperties.class)
public class GatewayRoutingConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingConfig.class);

    /**
     * Creates RouteLocator with convention-based routing
     *
     * @param builder RouteLocatorBuilder
     * @param properties routing configuration properties
     * @return configured RouteLocator
     */
    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder, GatewayRoutingProperties properties) {

        var routes = builder.routes();
        DiscoveryConfig discovery = properties.getDiscovery();

        if (discovery.isEnabled()) {
            log.info(
                    "Service Discovery enabled - namespace: {}, default-port: {}",
                    discovery.getNamespace(),
                    discovery.getDefaultPort());
        } else {
            log.info("Service Discovery disabled - using static URI configuration");
        }

        for (ServiceRoute service : properties.getServices()) {
            String serviceId = service.getId();
            String uri = buildServiceUri(serviceId, service, discovery);
            List<String> hosts = service.getHosts();

            if (!hosts.isEmpty()) {
                log.info(
                        "Registering host-based route: {} hosts={} -> {}",
                        service.getPaths(),
                        hosts,
                        uri);
            } else {
                log.info("Registering path-based route: {} -> {}", service.getPaths(), uri);
            }

            for (String path : service.getPaths()) {
                String routeId = serviceId + "-" + (path.hashCode() & Integer.MAX_VALUE);

                routes =
                        routes.route(
                                routeId,
                                r -> {
                                    var predicateSpec = r.path(path);

                                    // Add host predicate if hosts are configured
                                    // Check both Host header and X-Forwarded-Host for CloudFront
                                    // compatibility
                                    if (!hosts.isEmpty()) {
                                        String hostPattern =
                                                String.join("|", hosts)
                                                        .replace(".", "\\.")
                                                        .replace("*", ".*");
                                        predicateSpec =
                                                predicateSpec
                                                        .and()
                                                        .predicate(
                                                                exchange -> {
                                                                    var request =
                                                                            exchange.getRequest();
                                                                    // Check X-Forwarded-Host first
                                                                    // (CloudFront/ALB)
                                                                    // X-Forwarded-Host may contain
                                                                    // comma-separated values
                                                                    String forwardedHost =
                                                                            request.getHeaders()
                                                                                    .getFirst(
                                                                                            "X-Forwarded-Host");
                                                                    if (forwardedHost != null
                                                                            && !forwardedHost
                                                                                    .isEmpty()) {
                                                                        // Parse comma-separated
                                                                        // hosts, use first valid
                                                                        String firstHost =
                                                                                extractFirstValidHost(
                                                                                        forwardedHost);
                                                                        if (firstHost != null) {
                                                                            return hosts.stream()
                                                                                    .anyMatch(
                                                                                            h ->
                                                                                                    matchHost(
                                                                                                            h,
                                                                                                            firstHost));
                                                                        }
                                                                    }
                                                                    // Fallback to Host header
                                                                    String host =
                                                                            request.getHeaders()
                                                                                    .getFirst(
                                                                                            "Host");
                                                                    if (host != null) {
                                                                        // Remove port if present
                                                                        String hostWithoutPort =
                                                                                removePort(host);
                                                                        return hosts.stream()
                                                                                .anyMatch(
                                                                                        h ->
                                                                                                matchHost(
                                                                                                        h,
                                                                                                        hostWithoutPort));
                                                                    }
                                                                    return false;
                                                                });
                                    }

                                    return predicateSpec
                                            .filters(
                                                    f -> {
                                                        if (service.isStripPrefix()) {
                                                            return f.stripPrefix(
                                                                    service.getStripPrefixParts());
                                                        }
                                                        return f;
                                                    })
                                            .uri(uri);
                                });
            }
        }

        return routes.build();
    }

    /**
     * Match host pattern against actual host
     *
     * <p>Supports wildcard patterns like "*.set-of.com"
     *
     * @param pattern host pattern (e.g., "set-of.com", "*.set-of.com")
     * @param host actual host from request
     * @return true if matches
     */
    private boolean matchHost(String pattern, String host) {
        if (pattern.equals(host)) {
            return true;
        }
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1); // ".set-of.com"
            return host.endsWith(suffix) || host.equals(pattern.substring(2));
        }
        return false;
    }

    /**
     * 쉼표로 구분된 호스트 목록에서 첫 번째 유효한 호스트 추출
     *
     * <p>빈 값이나 공백만 있는 값은 건너뛰고 첫 번째 유효한 호스트를 반환합니다. 포트 번호가 있으면 제거합니다.
     *
     * @param hosts 쉼표로 구분된 호스트 문자열
     * @return 첫 번째 유효한 호스트 (포트 제외) 또는 null
     */
    private String extractFirstValidHost(String hosts) {
        for (String host : hosts.split(",")) {
            String trimmed = host.trim();
            if (!trimmed.isEmpty()) {
                return removePort(trimmed);
            }
        }
        return null;
    }

    /**
     * Host에서 포트 번호 제거
     *
     * @param host Host 값 (예: "api.set-of.com:443")
     * @return 포트 제거된 Host (예: "api.set-of.com")
     */
    private String removePort(String host) {
        if (host == null) {
            return null;
        }
        int colonIndex = host.indexOf(':');
        return colonIndex > 0 ? host.substring(0, colonIndex) : host;
    }

    /**
     * Build service URI based on discovery configuration
     *
     * <p>When discovery is enabled: http://{service-id}.{namespace}:{port} When discovery is
     * disabled: use static URI from configuration
     *
     * @param serviceId service identifier
     * @param service service route configuration
     * @param discovery discovery configuration
     * @return service URI
     */
    private String buildServiceUri(
            String serviceId, ServiceRoute service, DiscoveryConfig discovery) {

        // Static URI takes precedence (for local development)
        if (service.getUri() != null && !service.getUri().isBlank()) {
            return service.getUri();
        }

        // Cloud Map Discovery mode
        if (discovery.isEnabled()) {
            int port = service.getPort() > 0 ? service.getPort() : discovery.getDefaultPort();

            return String.format("http://%s.%s:%d", serviceId, discovery.getNamespace(), port);
        }

        throw new IllegalStateException(
                "Service URI not configured and discovery is disabled for: " + serviceId);
    }

    /**
     * Gateway Routing Properties
     *
     * <p>Binds to {@code gateway.routing.*} configuration properties
     */
    @ConfigurationProperties(prefix = "gateway.routing")
    public static class GatewayRoutingProperties {

        private DiscoveryConfig discovery = new DiscoveryConfig();
        private List<ServiceRoute> services = List.of();
        private Map<String, String> hostMapping = Map.of();

        public DiscoveryConfig getDiscovery() {
            return discovery;
        }

        public void setDiscovery(DiscoveryConfig discovery) {
            this.discovery = discovery;
        }

        public List<ServiceRoute> getServices() {
            return services;
        }

        public void setServices(List<ServiceRoute> services) {
            this.services = services;
        }

        public Map<String, String> getHostMapping() {
            return hostMapping;
        }

        public void setHostMapping(Map<String, String> hostMapping) {
            this.hostMapping = hostMapping;
        }
    }

    /** Service Discovery Configuration */
    public static class DiscoveryConfig {

        /** Enable AWS Cloud Map service discovery */
        private boolean enabled = false;

        /** Cloud Map namespace (e.g., connectly.local) */
        private String namespace = "connectly.local";

        /** Default port when service port is not specified */
        private int defaultPort = 8080;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public int getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(int defaultPort) {
            this.defaultPort = defaultPort;
        }
    }

    /**
     * Service Route Configuration
     *
     * <p>Spring Boot ConfigurationProperties 바인딩을 위한 POJO 클래스입니다.
     */
    @SuppressWarnings("PMD.DataClass")
    public static class ServiceRoute {

        /** Service identifier (used for Cloud Map DNS name) */
        private String id;

        /** Static URI (for local development, takes precedence over discovery) */
        private String uri;

        /** Service port (0 means use default-port) */
        private int port = 0;

        /** URL path patterns to route to this service */
        private List<String> paths = List.of();

        /** Whether to strip path prefix */
        private boolean stripPrefix = false;

        /** Number of path segments to strip */
        private int stripPrefixParts = 0;

        /** Public paths that don't require JWT authentication */
        private List<String> publicPaths = List.of();

        /** Host patterns for host-based routing (e.g., "*.set-of.com", "server.set-of.net") */
        private List<String> hosts = List.of();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<String> getPaths() {
            return Collections.unmodifiableList(paths);
        }

        public void setPaths(List<String> paths) {
            this.paths = paths == null ? List.of() : new ArrayList<>(paths);
        }

        public boolean isStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(boolean stripPrefix) {
            this.stripPrefix = stripPrefix;
        }

        public int getStripPrefixParts() {
            return stripPrefixParts;
        }

        public void setStripPrefixParts(int stripPrefixParts) {
            this.stripPrefixParts = stripPrefixParts;
        }

        public List<String> getPublicPaths() {
            return Collections.unmodifiableList(publicPaths);
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths == null ? List.of() : new ArrayList<>(publicPaths);
        }

        public List<String> getHosts() {
            return Collections.unmodifiableList(hosts);
        }

        public void setHosts(List<String> hosts) {
            this.hosts = hosts == null ? List.of() : new ArrayList<>(hosts);
        }
    }
}
