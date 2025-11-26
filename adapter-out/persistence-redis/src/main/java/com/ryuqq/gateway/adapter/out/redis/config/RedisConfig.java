package com.ryuqq.gateway.adapter.out.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
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
 * Redis Configuration
 *
 * <p>Redis Reactive 연결 및 Template 설정
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
 */
@Configuration
public class RedisConfig {

    /** Lettuce Client Resources */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.create();
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
}
