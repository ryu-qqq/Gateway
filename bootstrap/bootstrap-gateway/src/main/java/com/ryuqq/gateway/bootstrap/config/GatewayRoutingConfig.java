package com.ryuqq.gateway.bootstrap.config;

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

            log.info("Registering route: {} -> {}", service.getPaths(), uri);

            for (String path : service.getPaths()) {
                String routeId = serviceId + "-" + Math.abs(path.hashCode());

                routes =
                        routes.route(
                                routeId,
                                r ->
                                        r.path(path)
                                                .filters(
                                                        f -> {
                                                            if (service.isStripPrefix()) {
                                                                return f.stripPrefix(
                                                                        service
                                                                                .getStripPrefixParts());
                                                            }
                                                            return f;
                                                        })
                                                .uri(uri));
            }
        }

        return routes.build();
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

    /** Service Route Configuration */
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
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
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
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }
    }
}
