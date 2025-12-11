package com.ryuqq.gateway.adapter.out.redis.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.redis.entity.SessionConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantRateLimitConfigEntity;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantConfigMapper 테스트")
class TenantConfigMapperTest {

    private TenantConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TenantConfigMapper();
    }

    @Nested
    @DisplayName("toTenantConfig() - Entity → Domain 변환")
    class ToTenantConfigTest {

        @Test
        @DisplayName("null Entity 입력 시 null 반환")
        void shouldReturnNullWhenEntityIsNull() {
            // when
            TenantConfig result = mapper.toTenantConfig(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("전체 필드가 포함된 Entity를 Domain으로 변환")
        void shouldConvertEntityWithAllFieldsToDomain() {
            // given
            String tenantId = "tenant-1";
            Set<String> socialLogins = Set.of("KAKAO", "GOOGLE");
            Map<String, Set<String>> roleHierarchy =
                    Map.of("ADMIN", Set.of("READ", "WRITE", "DELETE"));

            SessionConfigEntity sessionEntity = new SessionConfigEntity(5, 3600L, 86400L);
            TenantRateLimitConfigEntity rateLimitEntity = new TenantRateLimitConfigEntity(100, 50);

            TenantConfigEntity entity =
                    new TenantConfigEntity(
                            tenantId,
                            true,
                            socialLogins,
                            roleHierarchy,
                            sessionEntity,
                            rateLimitEntity);

            // when
            TenantConfig result = mapper.toTenantConfig(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTenantIdValue()).isEqualTo(tenantId);
            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getAllowedSocialLogins())
                    .containsExactlyInAnyOrder(SocialProvider.KAKAO, SocialProvider.GOOGLE);
            assertThat(result.getRoleHierarchy())
                    .containsKey("ADMIN")
                    .containsEntry("ADMIN", Set.of("READ", "WRITE", "DELETE"));
            assertThat(result.getSessionConfig().maxActiveSessions()).isEqualTo(5);
            assertThat(result.getSessionConfig().accessTokenTTLSeconds()).isEqualTo(3600L);
            assertThat(result.getSessionConfig().refreshTokenTTLSeconds()).isEqualTo(86400L);
            assertThat(result.getRateLimitConfig().loginAttemptsPerHour()).isEqualTo(100);
            assertThat(result.getRateLimitConfig().otpRequestsPerHour()).isEqualTo(50);
        }

        @Test
        @DisplayName("null SessionConfig Entity 시 기본값 사용")
        void shouldUseDefaultSessionConfigWhenEntityIsNull() {
            // given
            TenantConfigEntity entity =
                    new TenantConfigEntity("tenant-2", false, Set.of(), Map.of(), null, null);

            // when
            TenantConfig result = mapper.toTenantConfig(entity);

            // then
            assertThat(result).isNotNull();
            SessionConfig defaultConfig = SessionConfig.defaultConfig();
            assertThat(result.getSessionConfig().maxActiveSessions())
                    .isEqualTo(defaultConfig.maxActiveSessions());
            assertThat(result.getSessionConfig().accessTokenTTLSeconds())
                    .isEqualTo(defaultConfig.accessTokenTTLSeconds());
        }

        @Test
        @DisplayName("null RateLimitConfig Entity 시 기본값 사용")
        void shouldUseDefaultRateLimitConfigWhenEntityIsNull() {
            // given
            TenantConfigEntity entity =
                    new TenantConfigEntity("tenant-3", false, Set.of(), Map.of(), null, null);

            // when
            TenantConfig result = mapper.toTenantConfig(entity);

            // then
            assertThat(result).isNotNull();
            TenantRateLimitConfig defaultConfig = TenantRateLimitConfig.defaultConfig();
            assertThat(result.getRateLimitConfig().loginAttemptsPerHour())
                    .isEqualTo(defaultConfig.loginAttemptsPerHour());
            assertThat(result.getRateLimitConfig().otpRequestsPerHour())
                    .isEqualTo(defaultConfig.otpRequestsPerHour());
        }

        @Test
        @DisplayName("빈 소셜 로그인 Set 변환")
        void shouldConvertEmptySocialLogins() {
            // given
            TenantConfigEntity entity =
                    new TenantConfigEntity("tenant-4", false, Set.of(), Map.of(), null, null);

            // when
            TenantConfig result = mapper.toTenantConfig(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAllowedSocialLogins()).isEmpty();
        }

        @Test
        @DisplayName("단일 소셜 프로바이더 변환")
        void shouldConvertSingleSocialProvider() {
            // given
            TenantConfigEntity entity =
                    new TenantConfigEntity(
                            "tenant-5", false, Set.of("NAVER"), Map.of(), null, null);

            // when
            TenantConfig result = mapper.toTenantConfig(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAllowedSocialLogins()).containsExactly(SocialProvider.NAVER);
        }
    }

    @Nested
    @DisplayName("toTenantConfigEntity() - Domain → Entity 변환")
    class ToTenantConfigEntityTest {

        @Test
        @DisplayName("null Domain 입력 시 null 반환")
        void shouldReturnNullWhenDomainIsNull() {
            // when
            TenantConfigEntity result = mapper.toTenantConfigEntity(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("전체 필드가 포함된 Domain을 Entity로 변환")
        void shouldConvertDomainWithAllFieldsToEntity() {
            // given
            String tenantId = "tenant-10";
            Set<SocialProvider> socialLogins = Set.of(SocialProvider.KAKAO, SocialProvider.GOOGLE);
            Map<String, Set<String>> roleHierarchy = Map.of("USER", Set.of("READ"));
            SessionConfig sessionConfig = SessionConfig.ofSeconds(3, 1800L, 43200L);
            TenantRateLimitConfig rateLimitConfig = TenantRateLimitConfig.of(200, 100);

            TenantConfig tenantConfig =
                    TenantConfig.of(
                            TenantId.of(tenantId),
                            true,
                            socialLogins,
                            roleHierarchy,
                            sessionConfig,
                            rateLimitConfig);

            // when
            TenantConfigEntity result = mapper.toTenantConfigEntity(tenantConfig);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getAllowedSocialLogins())
                    .containsExactlyInAnyOrder("KAKAO", "GOOGLE");
            assertThat(result.getRoleHierarchy())
                    .containsKey("USER")
                    .containsEntry("USER", Set.of("READ"));
            assertThat(result.getSessionConfig().getMaxActiveSessions()).isEqualTo(3);
            assertThat(result.getSessionConfig().getAccessTokenTTLSeconds()).isEqualTo(1800L);
            assertThat(result.getSessionConfig().getRefreshTokenTTLSeconds()).isEqualTo(43200L);
            assertThat(result.getRateLimitConfig().getLoginAttemptsPerHour()).isEqualTo(200);
            assertThat(result.getRateLimitConfig().getOtpRequestsPerHour()).isEqualTo(100);
        }

        @Test
        @DisplayName("최소 필드만 포함된 Domain을 Entity로 변환")
        void shouldConvertMinimalDomainToEntity() {
            // given
            TenantConfig tenantConfig = TenantConfig.of(TenantId.of("tenant-11"), false);

            // when
            TenantConfigEntity result = mapper.toTenantConfigEntity(tenantConfig);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("tenant-11");
            assertThat(result.isMfaRequired()).isFalse();
            assertThat(result.getAllowedSocialLogins()).isEmpty();
            assertThat(result.getRoleHierarchy()).isEmpty();
            // Default session config and rate limit config
            assertThat(result.getSessionConfig()).isNotNull();
            assertThat(result.getRateLimitConfig()).isNotNull();
        }

        @Test
        @DisplayName("모든 소셜 프로바이더가 포함된 Domain 변환")
        void shouldConvertAllSocialProviders() {
            // given
            Set<SocialProvider> allProviders =
                    Set.of(SocialProvider.KAKAO, SocialProvider.NAVER, SocialProvider.GOOGLE);
            TenantConfig tenantConfig =
                    TenantConfig.of(
                            TenantId.of("tenant-12"), false, allProviders, Map.of(), null, null);

            // when
            TenantConfigEntity result = mapper.toTenantConfigEntity(tenantConfig);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAllowedSocialLogins())
                    .containsExactlyInAnyOrder("KAKAO", "NAVER", "GOOGLE");
        }
    }

    @Nested
    @DisplayName("양방향 변환 일관성 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("Domain → Entity → Domain 왕복 변환 시 동일성 유지")
        void shouldMaintainConsistencyOnRoundTrip() {
            // given
            String tenantId = "tenant-999";
            Set<SocialProvider> socialLogins = Set.of(SocialProvider.KAKAO);
            Map<String, Set<String>> roleHierarchy = Map.of("ADMIN", Set.of("ALL"));
            SessionConfig sessionConfig = SessionConfig.ofSeconds(10, 7200L, 172800L);
            TenantRateLimitConfig rateLimitConfig = TenantRateLimitConfig.of(500, 250);

            TenantConfig original =
                    TenantConfig.of(
                            TenantId.of(tenantId),
                            true,
                            socialLogins,
                            roleHierarchy,
                            sessionConfig,
                            rateLimitConfig);

            // when
            TenantConfigEntity entity = mapper.toTenantConfigEntity(original);
            TenantConfig restored = mapper.toTenantConfig(entity);

            // then
            assertThat(restored.getTenantIdValue()).isEqualTo(original.getTenantIdValue());
            assertThat(restored.isMfaRequired()).isEqualTo(original.isMfaRequired());
            assertThat(restored.getAllowedSocialLogins())
                    .containsExactlyInAnyOrderElementsOf(original.getAllowedSocialLogins());
            assertThat(restored.getRoleHierarchy()).isEqualTo(original.getRoleHierarchy());
            assertThat(restored.getSessionConfig().maxActiveSessions())
                    .isEqualTo(original.getSessionConfig().maxActiveSessions());
            assertThat(restored.getSessionConfig().accessTokenTTLSeconds())
                    .isEqualTo(original.getSessionConfig().accessTokenTTLSeconds());
            assertThat(restored.getSessionConfig().refreshTokenTTLSeconds())
                    .isEqualTo(original.getSessionConfig().refreshTokenTTLSeconds());
            assertThat(restored.getRateLimitConfig().loginAttemptsPerHour())
                    .isEqualTo(original.getRateLimitConfig().loginAttemptsPerHour());
            assertThat(restored.getRateLimitConfig().otpRequestsPerHour())
                    .isEqualTo(original.getRateLimitConfig().otpRequestsPerHour());
        }
    }
}
