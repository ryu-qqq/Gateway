package com.ryuqq.gateway.adapter.out.redis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Integration Test용 Spring Configuration
 *
 * <p>TestContainers Redis와 연결하기 위한 최소 설정
 * <p>필요한 Repository만 Bean으로 명시적으로 등록 (ComponentScan 사용하지 않음)
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
public class RedisIntegrationTestConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @Primary
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean(name = "reactiveStringRedisTemplate")
    @Primary
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @Bean(name = "tenantConfigRedisTemplate")
    public ReactiveRedisTemplate<String, TenantConfigEntity> tenantConfigRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<TenantConfigEntity> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, TenantConfigEntity.class);

        RedisSerializationContext<String, TenantConfigEntity> context =
                RedisSerializationContext.<String, TenantConfigEntity>newSerializationContext(
                                keySerializer)
                        .value(valueSerializer)
                        .hashKey(keySerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    // ========================================
    // Repository Beans (명시적 등록)
    // ========================================

    @Bean
    public RateLimitRedisRepository rateLimitRedisRepository(
            ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        return new RateLimitRedisRepository(reactiveStringRedisTemplate);
    }

    @Bean
    public AccountLockRedisRepository accountLockRedisRepository(
            ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        return new AccountLockRedisRepository(reactiveStringRedisTemplate);
    }

    @Bean
    public IpBlockRedisRepository ipBlockRedisRepository(
            ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        return new IpBlockRedisRepository(reactiveStringRedisTemplate);
    }

    @Bean
    public TenantConfigRedisRepository tenantConfigRedisRepository(
            ReactiveRedisTemplate<String, TenantConfigEntity> tenantConfigRedisTemplate) {
        return new TenantConfigRedisRepository(tenantConfigRedisTemplate);
    }
}
