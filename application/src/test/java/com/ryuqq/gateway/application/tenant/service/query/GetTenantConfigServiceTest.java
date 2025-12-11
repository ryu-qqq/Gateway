package com.ryuqq.gateway.application.tenant.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.port.out.client.AuthHubTenantClient;
import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import com.ryuqq.gateway.application.tenant.port.out.query.TenantConfigQueryPort;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.TenantConfigPersistenceException;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("GetTenantConfigService 테스트")
class GetTenantConfigServiceTest {

    private TenantConfigQueryPort tenantConfigQueryPort;
    private AuthHubTenantClient authHubTenantClient;
    private TenantConfigCommandPort tenantConfigCommandPort;
    private GetTenantConfigService getTenantConfigService;

    @BeforeEach
    void setUp() {
        tenantConfigQueryPort = mock(TenantConfigQueryPort.class);
        authHubTenantClient = mock(AuthHubTenantClient.class);
        tenantConfigCommandPort = mock(TenantConfigCommandPort.class);
        getTenantConfigService =
                new GetTenantConfigService(
                        tenantConfigQueryPort, authHubTenantClient, tenantConfigCommandPort);

        // Default stubbing for AuthHubTenantClient to prevent NPE in switchIfEmpty
        given(authHubTenantClient.fetchTenantConfig(anyString())).willReturn(Mono.empty());
    }

    private TenantConfig createTenantConfig(String tenantId) {
        return TenantConfig.of(TenantId.of(tenantId), false);
    }

    @Test
    @DisplayName("Cache Hit - Redis에서 조회 성공 시 AuthHub 호출하지 않음")
    void shouldReturnFromCacheWhenHit() {
        // given
        String tenantId = "tenant-1";
        TenantConfig cachedConfig = createTenantConfig(tenantId);
        GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

        given(tenantConfigQueryPort.findByTenantId(anyString()))
                .willReturn(Mono.just(cachedConfig));

        // when & then
        StepVerifier.create(getTenantConfigService.execute(query))
                .assertNext(
                        response -> {
                            assertThat(response).isNotNull();
                            assertThat(response.tenantConfig()).isNotNull();
                            assertThat(response.tenantConfig().getTenantIdValue())
                                    .isEqualTo(tenantId);
                        })
                .verifyComplete();

        verify(tenantConfigQueryPort).findByTenantId(tenantId);
        verify(tenantConfigCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("Cache Miss - AuthHub에서 조회 후 캐싱")
    void shouldFetchFromAuthHubAndCacheWhenMiss() {
        // given
        String tenantId = "tenant-2";
        TenantConfig fetchedConfig = createTenantConfig(tenantId);
        GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

        given(tenantConfigQueryPort.findByTenantId(anyString())).willReturn(Mono.empty());
        given(authHubTenantClient.fetchTenantConfig(anyString()))
                .willReturn(Mono.just(fetchedConfig));
        given(tenantConfigCommandPort.save(any(TenantConfig.class))).willReturn(Mono.empty());

        // when & then
        StepVerifier.create(getTenantConfigService.execute(query))
                .assertNext(
                        response -> {
                            assertThat(response).isNotNull();
                            assertThat(response.tenantConfig()).isNotNull();
                            assertThat(response.tenantConfig().getTenantIdValue())
                                    .isEqualTo(tenantId);
                        })
                .verifyComplete();

        verify(tenantConfigQueryPort).findByTenantId(tenantId);
        verify(authHubTenantClient).fetchTenantConfig(tenantId);
        verify(tenantConfigCommandPort).save(any(TenantConfig.class));
    }

    @Test
    @DisplayName("Redis 조회 오류 시 TenantConfigPersistenceException 발생")
    void shouldThrowExceptionWhenRedisError() {
        // given
        String tenantId = "tenant-3";
        GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

        given(tenantConfigQueryPort.findByTenantId(anyString()))
                .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // when & then
        StepVerifier.create(getTenantConfigService.execute(query))
                .expectError(TenantConfigPersistenceException.class)
                .verify();
    }

    @Test
    @DisplayName("AuthHub 호출 오류 시 TenantConfigPersistenceException 발생")
    void shouldThrowExceptionWhenAuthHubError() {
        // given
        String tenantId = "tenant-4";
        GetTenantConfigQuery query = new GetTenantConfigQuery(tenantId);

        given(tenantConfigQueryPort.findByTenantId(anyString())).willReturn(Mono.empty());
        given(authHubTenantClient.fetchTenantConfig(anyString()))
                .willReturn(Mono.error(new RuntimeException("AuthHub unavailable")));

        // when & then
        StepVerifier.create(getTenantConfigService.execute(query))
                .expectError(TenantConfigPersistenceException.class)
                .verify();
    }

    @Test
    @DisplayName("tenantId로 직접 TenantConfig 조회")
    void shouldReturnTenantConfigDirectly() {
        // given
        String tenantId = "tenant-5";
        TenantConfig tenantConfig = createTenantConfig(tenantId);

        given(tenantConfigQueryPort.findByTenantId(anyString()))
                .willReturn(Mono.just(tenantConfig));

        // when & then
        StepVerifier.create(getTenantConfigService.getTenantConfig(tenantId))
                .assertNext(
                        result -> {
                            assertThat(result).isNotNull();
                            assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
                        })
                .verifyComplete();
    }
}
