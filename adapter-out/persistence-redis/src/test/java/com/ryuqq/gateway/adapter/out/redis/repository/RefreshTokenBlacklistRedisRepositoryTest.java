package com.ryuqq.gateway.adapter.out.redis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("RefreshTokenBlacklistRedisRepository 테스트")
class RefreshTokenBlacklistRedisRepositoryTest {

    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private RefreshTokenBlacklistRedisRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        reactiveStringRedisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        repository = new RefreshTokenBlacklistRedisRepository(reactiveStringRedisTemplate);

        given(reactiveStringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("addToBlacklist() 테스트")
    class AddToBlacklistTest {

        @Test
        @DisplayName("Token을 Blacklist에 성공적으로 등록")
        void shouldAddTokenToBlacklistSuccessfully() {
            // given
            String tenantId = "tenant-1";
            String tokenValue = "refresh-token-value-12345";
            Duration ttl = Duration.ofDays(7);

            given(valueOperations.set(anyString(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Token Hash가 Key에 포함되어 저장")
        void shouldIncludeTokenHashInKey() {
            // given
            String tenantId = "tenant-2";
            String tokenValue = "my-refresh-token";
            Duration ttl = Duration.ofHours(24);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            given(valueOperations.set(keyCaptor.capture(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(true));

            // when
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            // then
            String capturedKey = keyCaptor.getValue();
            assertThat(capturedKey).startsWith("tenant:tenant-2:refresh:blacklist:");
            assertThat(capturedKey).hasSize("tenant:tenant-2:refresh:blacklist:".length() + 64);
        }

        @Test
        @DisplayName("다양한 TTL 값으로 등록")
        void shouldSupportVariousTtlValues() {
            // given
            String tenantId = "tenant-3";
            String tokenValue = "token-for-ttl-test";
            Duration shortTtl = Duration.ofMinutes(30);

            given(valueOperations.set(anyString(), eq("blacklisted"), eq(shortTtl)))
                    .willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, shortTtl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("등록 실패 시 false 반환")
        void shouldReturnFalseWhenAddFails() {
            // given
            String tenantId = "tenant-4";
            String tokenValue = "failing-token";
            Duration ttl = Duration.ofDays(1);

            given(valueOperations.set(anyString(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(false));

            // when & then
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("동일 Token은 동일 Hash 생성")
        void shouldGenerateSameHashForSameToken() {
            // given
            String tenantId = "tenant-5";
            String tokenValue = "consistent-token";
            Duration ttl = Duration.ofDays(7);

            ArgumentCaptor<String> keyCaptor1 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor2 = ArgumentCaptor.forClass(String.class);

            given(valueOperations.set(keyCaptor1.capture(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(true));

            // when - first call
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            String firstKey = keyCaptor1.getValue();

            // reset mock for second call
            given(valueOperations.set(keyCaptor2.capture(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(true));

            // when - second call with same token
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            String secondKey = keyCaptor2.getValue();

            // then - keys should be identical
            assertThat(firstKey).isEqualTo(secondKey);
        }
    }

    @Nested
    @DisplayName("isBlacklisted() 테스트")
    class IsBlacklistedTest {

        @Test
        @DisplayName("Blacklist에 존재하는 Token 확인 시 true 반환")
        void shouldReturnTrueWhenTokenIsBlacklisted() {
            // given
            String tenantId = "tenant-1";
            String tokenValue = "blacklisted-token";

            given(reactiveStringRedisTemplate.hasKey(anyString())).willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(repository.isBlacklisted(tenantId, tokenValue))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Blacklist에 존재하지 않는 Token 확인 시 false 반환")
        void shouldReturnFalseWhenTokenIsNotBlacklisted() {
            // given
            String tenantId = "tenant-2";
            String tokenValue = "valid-token";

            given(reactiveStringRedisTemplate.hasKey(anyString())).willReturn(Mono.just(false));

            // when & then
            StepVerifier.create(repository.isBlacklisted(tenantId, tokenValue))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("올바른 Key Format으로 조회")
        void shouldQueryWithCorrectKeyFormat() {
            // given
            String tenantId = "tenant-3";
            String tokenValue = "token-for-key-check";

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            given(reactiveStringRedisTemplate.hasKey(keyCaptor.capture()))
                    .willReturn(Mono.just(false));

            // when
            StepVerifier.create(repository.isBlacklisted(tenantId, tokenValue))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();

            // then
            String capturedKey = keyCaptor.getValue();
            assertThat(capturedKey).startsWith("tenant:tenant-3:refresh:blacklist:");
            assertThat(capturedKey).hasSize("tenant:tenant-3:refresh:blacklist:".length() + 64);
        }

        @Test
        @DisplayName("다른 Tenant의 동일 Token은 다른 Key 생성")
        void shouldGenerateDifferentKeysForDifferentTenants() {
            // given
            String tenant1 = "tenant-A";
            String tenant2 = "tenant-B";
            String sameToken = "same-token-value";

            ArgumentCaptor<String> keyCaptor1 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor2 = ArgumentCaptor.forClass(String.class);

            given(reactiveStringRedisTemplate.hasKey(keyCaptor1.capture()))
                    .willReturn(Mono.just(false));

            // when - first tenant
            StepVerifier.create(repository.isBlacklisted(tenant1, sameToken))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();

            String key1 = keyCaptor1.getValue();

            // reset for second call
            given(reactiveStringRedisTemplate.hasKey(keyCaptor2.capture()))
                    .willReturn(Mono.just(false));

            // when - second tenant
            StepVerifier.create(repository.isBlacklisted(tenant2, sameToken))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();

            String key2 = keyCaptor2.getValue();

            // then - keys should be different due to different tenantIds
            assertThat(key1).isNotEqualTo(key2);
            assertThat(key1).contains("tenant-A");
            assertThat(key2).contains("tenant-B");
        }
    }

    @Nested
    @DisplayName("Hash 일관성 테스트")
    class HashConsistencyTest {

        @Test
        @DisplayName("addToBlacklist와 isBlacklisted가 동일 Key 사용")
        void shouldUseSameKeyForAddAndCheck() {
            // given
            String tenantId = "tenant-consistency";
            String tokenValue = "consistency-test-token";
            Duration ttl = Duration.ofDays(7);

            ArgumentCaptor<String> addKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> checkKeyCaptor = ArgumentCaptor.forClass(String.class);

            given(valueOperations.set(addKeyCaptor.capture(), eq("blacklisted"), eq(ttl)))
                    .willReturn(Mono.just(true));
            given(reactiveStringRedisTemplate.hasKey(checkKeyCaptor.capture()))
                    .willReturn(Mono.just(true));

            // when - add
            StepVerifier.create(repository.addToBlacklist(tenantId, tokenValue, ttl))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            // when - check
            StepVerifier.create(repository.isBlacklisted(tenantId, tokenValue))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            // then - keys should be identical
            assertThat(addKeyCaptor.getValue()).isEqualTo(checkKeyCaptor.getValue());
        }

        @Test
        @DisplayName("다른 Token은 다른 Hash 생성")
        void shouldGenerateDifferentHashesForDifferentTokens() {
            // given
            String tenantId = "tenant-hash";
            String token1 = "first-token";
            String token2 = "second-token";

            ArgumentCaptor<String> keyCaptor1 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor2 = ArgumentCaptor.forClass(String.class);

            given(reactiveStringRedisTemplate.hasKey(keyCaptor1.capture()))
                    .willReturn(Mono.just(false));

            // when - first token
            StepVerifier.create(repository.isBlacklisted(tenantId, token1))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();

            String key1 = keyCaptor1.getValue();

            // reset for second call
            given(reactiveStringRedisTemplate.hasKey(keyCaptor2.capture()))
                    .willReturn(Mono.just(false));

            // when - second token
            StepVerifier.create(repository.isBlacklisted(tenantId, token2))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();

            String key2 = keyCaptor2.getValue();

            // then - keys should be different due to different token hashes
            assertThat(key1).isNotEqualTo(key2);
        }
    }
}
