package com.ryuqq.gateway.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration tests. Provides Redis container for Gateway testing.
 *
 * <p>All containers are configured with reuse enabled for faster test execution.
 */
@TestConfiguration
public class TestContainersConfig {

    private static final String REDIS_IMAGE = "redis:7-alpine";

    @Bean(initMethod = "start", destroyMethod = "stop")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(6379)
                .withReuse(true);
    }
}
