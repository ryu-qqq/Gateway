package com.ryuqq.gateway.adapter.out.redis.mapper;

import static org.assertj.core.api.Assertions.*;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PublicKeyMapper 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PublicKeyMapper 단위 테스트")
class PublicKeyMapperTest {

    private PublicKeyMapper publicKeyMapper;

    @BeforeEach
    void setUp() {
        publicKeyMapper = new PublicKeyMapper();
    }

    @Nested
    @DisplayName("toPublicKey 메서드")
    class ToPublicKeyTest {

        @Test
        @DisplayName("Entity를 Domain VO로 변환해야 한다")
        void shouldConvertEntityToPublicKey() {
            // given
            PublicKeyEntity entity =
                    new PublicKeyEntity(
                            "test-kid", "modulus-value", "exponent-value", "RSA", "sig", "RS256");

            // when
            PublicKey result = publicKeyMapper.toPublicKey(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.kid()).isEqualTo("test-kid");
            assertThat(result.modulus()).isEqualTo("modulus-value");
            assertThat(result.exponent()).isEqualTo("exponent-value");
            assertThat(result.kty()).isEqualTo("RSA");
            assertThat(result.use()).isEqualTo("sig");
            assertThat(result.alg()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("null Entity는 null을 반환해야 한다")
        void shouldReturnNullForNullEntity() {
            // when
            PublicKey result = publicKeyMapper.toPublicKey(null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toPublicKeyEntity 메서드")
    class ToPublicKeyEntityTest {

        @Test
        @DisplayName("Domain VO를 Entity로 변환해야 한다")
        void shouldConvertPublicKeyToEntity() {
            // given
            PublicKey publicKey =
                    PublicKey.of(
                            "test-kid", "modulus-value", "exponent-value", "RSA", "sig", "RS256");

            // when
            PublicKeyEntity result = publicKeyMapper.toPublicKeyEntity(publicKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getKid()).isEqualTo("test-kid");
            assertThat(result.getModulus()).isEqualTo("modulus-value");
            assertThat(result.getExponent()).isEqualTo("exponent-value");
            assertThat(result.getKty()).isEqualTo("RSA");
            assertThat(result.getUse()).isEqualTo("sig");
            assertThat(result.getAlg()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("null Domain VO는 null을 반환해야 한다")
        void shouldReturnNullForNullPublicKey() {
            // when
            PublicKeyEntity result = publicKeyMapper.toPublicKeyEntity(null);

            // then
            assertThat(result).isNull();
        }
    }
}
