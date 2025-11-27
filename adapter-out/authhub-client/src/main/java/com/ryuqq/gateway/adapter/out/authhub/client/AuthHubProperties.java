package com.ryuqq.gateway.adapter.out.authhub.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AuthHub Client Configuration Properties
 *
 * <p>AuthHub 외부 시스템 연동 설정 (authhub-client.yml 기반)
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
    private String jwksEndpoint = "/api/v1/auth/jwks";
    private String refreshEndpoint = "/api/v1/auth/refresh";
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getJwksEndpoint() {
        return jwksEndpoint;
    }

    public void setJwksEndpoint(String jwksEndpoint) {
        this.jwksEndpoint = jwksEndpoint;
    }

    public String getRefreshEndpoint() {
        return refreshEndpoint;
    }

    public void setRefreshEndpoint(String refreshEndpoint) {
        this.refreshEndpoint = refreshEndpoint;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
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

    /** Timeout Configuration */
    public static class Timeout {
        private long connection = 3000;
        private long response = 3000;

        public long getConnection() {
            return connection;
        }

        public void setConnection(long connection) {
            this.connection = connection;
        }

        public long getResponse() {
            return response;
        }

        public void setResponse(long response) {
            this.response = response;
        }
    }

    /** Retry Configuration */
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

    /** Circuit Breaker Configuration */
    public static class CircuitBreaker {
        private float failureRateThreshold = 50;
        private long waitDurationInOpenState = 10000;
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;

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
    }
}
