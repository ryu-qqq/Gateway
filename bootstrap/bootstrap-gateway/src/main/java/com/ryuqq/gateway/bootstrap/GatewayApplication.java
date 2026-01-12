package com.ryuqq.gateway.bootstrap;

import java.security.Security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Connectly Gateway Application
 *
 * <p>Zero-Trust API Gateway with JWT Authentication
 *
 * <p><strong>Architecture:</strong>
 *
 * <ul>
 *   <li><strong>Domain Layer:</strong> JWT, Authentication domain models
 *   <li><strong>Application Layer:</strong> JWT validation, Public Key management Use Cases
 *   <li><strong>Adapter-In Gateway:</strong> Spring Cloud Gateway Filters
 *   <li><strong>Adapter-Out:</strong> Redis (Public Key cache), AuthHub (JWKS endpoint)
 *   <li><strong>Bootstrap:</strong> Application wiring and configuration
 * </ul>
 *
 * <p><strong>Component Scan:</strong>
 *
 * <ul>
 *   <li>com.ryuqq.gateway
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * # Run with Gradle
 * ./gradlew :bootstrap:bootstrap-gateway:bootRun
 *
 * # Run JAR
 * java -jar bootstrap/bootstrap-gateway/build/libs/connectly-gateway.jar
 *
 * # With profile
 * ./gradlew :bootstrap:bootstrap-gateway:bootRun --args='--spring.profiles.active=dev'
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.ryuqq.gateway")
@ConfigurationPropertiesScan(basePackages = "com.ryuqq.gateway")
public class GatewayApplication {

    // DNS 캐싱 설정 (Cloud Map DNS 조회 최적화)
    static {
        // 성공한 DNS 조회 결과 30초 캐싱 (기본: 30초, -1은 영구 캐싱)
        Security.setProperty("networkaddress.cache.ttl", "30");
        // 실패한 DNS 조회 결과 5초 캐싱 (기본: 10초)
        Security.setProperty("networkaddress.cache.negative.ttl", "5");
    }

    /**
     * Application entry point
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
