package com.ryuqq.gateway.adapter.out.redis.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PublicKeyEntity 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PublicKeyEntity 테스트")
class PublicKeyEntityTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("모든 필드로 엔티티 생성")
        void shouldCreateEntityWithAllFields() {
            // given
            String kid = "test-kid-123";
            String modulus = "n-value-modulus";
            String exponent = "AQAB";
            String kty = "RSA";
            String use = "sig";
            String alg = "RS256";

            // when
            PublicKeyEntity entity = new PublicKeyEntity(kid, modulus, exponent, kty, use, alg);

            // then
            assertThat(entity.getKid()).isEqualTo("test-kid-123");
            assertThat(entity.getModulus()).isEqualTo("n-value-modulus");
            assertThat(entity.getExponent()).isEqualTo("AQAB");
            assertThat(entity.getKty()).isEqualTo("RSA");
            assertThat(entity.getUse()).isEqualTo("sig");
            assertThat(entity.getAlg()).isEqualTo("RS256");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString은 주요 필드를 포함해야 함")
        void shouldIncludeKeyFieldsInToString() {
            // given
            PublicKeyEntity entity =
                    new PublicKeyEntity("kid-123", "modulus-value", "AQAB", "RSA", "sig", "RS256");

            // when
            String result = entity.toString();

            // then
            assertThat(result).contains("PublicKeyEntity");
            assertThat(result).contains("kid='kid-123'");
            assertThat(result).contains("kty='RSA'");
            assertThat(result).contains("alg='RS256'");
        }
    }
}
