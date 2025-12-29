package com.ryuqq.gateway.adapter.out.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Lettuce Configuration (캐싱 전용)
 *
 * <p>Redis Reactive 연결 및 Template 설정 (Lettuce 클라이언트 사용)
 *
 * <p><strong>역할</strong>:
 *
 * <ul>
 *   <li>ReactiveRedisConnectionFactory 생성
 *   <li>Entity별 ReactiveRedisTemplate 생성
 *   <li>JSON 직렬화 설정
 * </ul>
 *
 * <p><strong>Connection Pool 설정</strong>:
 *
 * <ul>
 *   <li>max-active: 16
 *   <li>max-idle: 8
 *   <li>min-idle: 4
 *   <li>max-wait: 1000ms
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see RedissonConfig 분산락 전용 설정
 */
@Configuration
public class LettuceConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Lettuce Client Resources with Micrometer Metrics
     *
     * <p>Micrometer를 통해 Redis 명령어 레이턴시 메트릭을 수집합니다.
     *
     * <p><strong>노출되는 메트릭</strong>:
     *
     * <ul>
     *   <li>lettuce_command_firstresponse_seconds - 첫 응답까지 시간
     *   <li>lettuce_command_completion_seconds - 전체 명령 완료 시간
     *   <li>lettuce_command_completion_seconds_bucket - 히스토그램 (P50/P95/P99)
     * </ul>
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources(MeterRegistry meterRegistry) {
        MicrometerOptions options = MicrometerOptions.builder().enable().histogram(true).build();

        return DefaultClientResources.builder()
                .commandLatencyRecorder(
                        new MicrometerCommandLatencyRecorder(meterRegistry, options))
                .build();
    }

    /** Reactive Redis Connection Factory */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(
            RedisProperties redisProperties, ClientResources clientResources) {
        // Lettuce Client Configuration
        LettuceClientConfiguration clientConfiguration =
                LettucePoolingClientConfiguration.builder()
                        .clientOptions(ClientOptions.builder().build())
                        .clientResources(clientResources)
                        .build();

        // Redis Standalone Configuration
        org.springframework.data.redis.connection.RedisStandaloneConfiguration redisConfig =
                new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());

        // Lettuce Connection Factory
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redisConfig, clientConfiguration);

        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    /** ReactiveRedisTemplate for PublicKeyEntity */
    @Bean
    public ReactiveRedisTemplate<String, PublicKeyEntity> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        // Jackson ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // JSON Serializer 설정
        Jackson2JsonRedisSerializer<PublicKeyEntity> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, PublicKeyEntity.class);

        // Serialization Context 설정
        RedisSerializationContext<String, PublicKeyEntity> context =
                RedisSerializationContext.<String, PublicKeyEntity>newSerializationContext(
                                new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .hashKey(new StringRedisSerializer())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    /** ReactiveRedisTemplate for PermissionSpecEntity */
    @Bean
    public ReactiveRedisTemplate<String, PermissionSpecEntity> permissionSpecRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<PermissionSpecEntity> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, PermissionSpecEntity.class);

        RedisSerializationContext<String, PermissionSpecEntity> context =
                RedisSerializationContext.<String, PermissionSpecEntity>newSerializationContext(
                                new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .hashKey(new StringRedisSerializer())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    /** ReactiveRedisTemplate for PermissionHashEntity */
    @Bean
    public ReactiveRedisTemplate<String, PermissionHashEntity> permissionHashRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<PermissionHashEntity> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, PermissionHashEntity.class);

        RedisSerializationContext<String, PermissionHashEntity> context =
                RedisSerializationContext.<String, PermissionHashEntity>newSerializationContext(
                                new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .hashKey(new StringRedisSerializer())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    /** ReactiveRedisTemplate for TenantConfigEntity */
    @Bean
    public ReactiveRedisTemplate<String, TenantConfigEntity> tenantConfigRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<TenantConfigEntity> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, TenantConfigEntity.class);

        RedisSerializationContext<String, TenantConfigEntity> context =
                RedisSerializationContext.<String, TenantConfigEntity>newSerializationContext(
                                new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .hashKey(new StringRedisSerializer())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }
}
