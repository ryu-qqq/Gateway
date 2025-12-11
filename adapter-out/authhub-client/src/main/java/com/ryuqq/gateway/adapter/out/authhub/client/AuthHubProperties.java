package com.ryuqq.gateway.adapter.out.authhub.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AuthHub Client Configuration Properties
 *
 * <p>AuthHub 외부 시스템 연동 설정 (authhub-client.yml 기반)
 *
 * <p><strong>설정 구조</strong>:
 *
 * <ul>
 *   <li>endpoints: 모든 API 엔드포인트 경로
 *   <li>webclient: WebClient 연결 설정
 *   <li>retry: Resilience4j Retry 설정
 *   <li>circuitBreaker: Resilience4j Circuit Breaker 설정
 * </ul>
 *
 * <p><strong>환경별 설정</strong>:
 *
 * <ul>
 *   <li>local: http://localhost:9090
 *   <li>test: http://localhost:9090
 *   <li>prod: https://auth.set-of.com
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "authhub.client")
public class AuthHubProperties {

    private String baseUrl;
    private Endpoints endpoints = new Endpoints();
    private WebClientConfig webclient = new WebClientConfig();
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    public WebClientConfig getWebclient() {
        return webclient;
    }

    public void setWebclient(WebClientConfig webclient) {
        this.webclient = webclient;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    // ===============================================
    // Convenience Methods for Endpoints
    // ===============================================

    /**
     * JWKS 엔드포인트 조회
     *
     * @return JWKS endpoint path
     */
    public String getJwksEndpoint() {
        return endpoints.getJwks();
    }

    /**
     * Token Refresh 엔드포인트 조회
     *
     * @return Refresh endpoint path
     */
    public String getRefreshEndpoint() {
        return endpoints.getRefresh();
    }

    /**
     * 만료 토큰 정보 추출 엔드포인트 조회
     *
     * @return Extract expired info endpoint path
     */
    public String getExtractExpiredInfoEndpoint() {
        return endpoints.getExtractExpiredInfo();
    }

    /**
     * Tenant Config 엔드포인트 조회
     *
     * @return Tenant config endpoint path
     */
    public String getTenantConfigEndpoint() {
        return endpoints.getTenantConfig();
    }

    /**
     * Permission Spec 엔드포인트 조회
     *
     * @return Permission spec endpoint path
     */
    public String getPermissionSpecEndpoint() {
        return endpoints.getPermissionSpec();
    }

    /**
     * User Permissions 엔드포인트 조회
     *
     * @return User permissions endpoint path
     */
    public String getUserPermissionsEndpoint() {
        return endpoints.getUserPermissions();
    }

    // ===============================================
    // Nested Configuration Classes
    // ===============================================

    /** Endpoints Configuration - 모든 API 엔드포인트 경로 */
    public static class Endpoints {

        // Authentication endpoints
        private String jwks = "/api/v1/auth/jwks";
        private String refresh = "/api/v1/auth/refresh";
        private String extractExpiredInfo = "/api/v1/auth/extract-expired-info";

        // Tenant endpoints
        private String tenantConfig = "/api/v1/tenants/{tenantId}/config";

        // Permission endpoints
        private String permissionSpec = "/api/v1/permissions/spec";
        private String userPermissions = "/api/v1/permissions/users/{userId}";

        public String getJwks() {
            return jwks;
        }

        public void setJwks(String jwks) {
            this.jwks = jwks;
        }

        public String getRefresh() {
            return refresh;
        }

        public void setRefresh(String refresh) {
            this.refresh = refresh;
        }

        public String getExtractExpiredInfo() {
            return extractExpiredInfo;
        }

        public void setExtractExpiredInfo(String extractExpiredInfo) {
            this.extractExpiredInfo = extractExpiredInfo;
        }

        public String getTenantConfig() {
            return tenantConfig;
        }

        public void setTenantConfig(String tenantConfig) {
            this.tenantConfig = tenantConfig;
        }

        public String getPermissionSpec() {
            return permissionSpec;
        }

        public void setPermissionSpec(String permissionSpec) {
            this.permissionSpec = permissionSpec;
        }

        public String getUserPermissions() {
            return userPermissions;
        }

        public void setUserPermissions(String userPermissions) {
            this.userPermissions = userPermissions;
        }
    }

    /** WebClient Configuration - 연결 및 타임아웃 설정 */
    public static class WebClientConfig {

        // Connection pool settings
        private int maxConnections = 500;
        private long pendingAcquireTimeout = 45000;
        private long maxIdleTime = 20000;

        // Timeout settings
        private long connectionTimeout = 3000;
        private long responseTimeout = 3000;

        // Logging
        private boolean wireLoggingEnabled = false;

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public long getPendingAcquireTimeout() {
            return pendingAcquireTimeout;
        }

        public void setPendingAcquireTimeout(long pendingAcquireTimeout) {
            this.pendingAcquireTimeout = pendingAcquireTimeout;
        }

        public long getMaxIdleTime() {
            return maxIdleTime;
        }

        public void setMaxIdleTime(long maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getResponseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(long responseTimeout) {
            this.responseTimeout = responseTimeout;
        }

        public boolean isWireLoggingEnabled() {
            return wireLoggingEnabled;
        }

        public void setWireLoggingEnabled(boolean wireLoggingEnabled) {
            this.wireLoggingEnabled = wireLoggingEnabled;
        }
    }

    /** Retry Configuration - Resilience4j Retry 설정 */
    public static class Retry {
        private int maxAttempts = 3;
        private long waitDuration = 100;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(long waitDuration) {
            this.waitDuration = waitDuration;
        }
    }

    /** Circuit Breaker Configuration - Resilience4j Circuit Breaker 설정 */
    public static class CircuitBreaker {
        private float failureRateThreshold = 50;
        private long waitDurationInOpenState = 10000;
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private int permittedCallsInHalfOpen = 3;

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public long getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(long waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public int getPermittedCallsInHalfOpen() {
            return permittedCallsInHalfOpen;
        }

        public void setPermittedCallsInHalfOpen(int permittedCallsInHalfOpen) {
            this.permittedCallsInHalfOpen = permittedCallsInHalfOpen;
        }
    }
}
