package com.ryuqq.gateway.integration.config;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * WireMock configuration for external HTTP API mocking. Provides separate WireMock servers for each
 * external service.
 */
@TestConfiguration
public class WireMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer authHubWireMock() {
        return new WireMockServer(wireMockConfig().dynamicPort());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer backendWireMock() {
        return new WireMockServer(wireMockConfig().dynamicPort());
    }
}
