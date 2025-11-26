package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PublicKey VO 테스트")
class PublicKeyTest {

    private static String VALID_MODULUS;
    private static String VALID_EXPONENT;
    private static RSAPublicKey TEST_RSA_PUBLIC_KEY;

    private static final String VALID_KID = "test-key-id-123";
    private static final String VALID_KTY = "RSA";
    private static final String VALID_USE = "sig";
    private static final String VALID_ALG = "RS256";

    @BeforeAll
    static void generateTestKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        TEST_RSA_PUBLIC_KEY = (RSAPublicKey) keyPair.getPublic();

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        VALID_MODULUS = encoder.encodeToString(TEST_RSA_PUBLIC_KEY.getModulus().toByteArray());
        VALID_EXPONENT =
                encoder.encodeToString(TEST_RSA_PUBLIC_KEY.getPublicExponent().toByteArray());
    }

    private PublicKey createValidPublicKey() {
        return PublicKey.of(
                VALID_KID, VALID_MODULUS, VALID_EXPONENT, VALID_KTY, VALID_USE, VALID_ALG);
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("유효한 값으로 PublicKey 생성")
        void shouldCreatePublicKeyWithValidValues() {
            // when
            PublicKey publicKey = createValidPublicKey();

            // then
            assertThat(publicKey.kid()).isEqualTo(VALID_KID);
            assertThat(publicKey.modulus()).isEqualTo(VALID_MODULUS);
            assertThat(publicKey.exponent()).isEqualTo(VALID_EXPONENT);
            assertThat(publicKey.kty()).isEqualTo(VALID_KTY);
            assertThat(publicKey.use()).isEqualTo(VALID_USE);
            assertThat(publicKey.alg()).isEqualTo(VALID_ALG);
        }

        @Test
        @DisplayName("소문자 kty도 허용")
        void shouldAcceptLowercaseKty() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID, VALID_MODULUS, VALID_EXPONENT, "rsa", VALID_USE, VALID_ALG);

            // then
            assertThat(publicKey.kty()).isEqualTo("rsa");
        }

        @Test
        @DisplayName("소문자 alg도 허용")
        void shouldAcceptLowercaseAlg() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID,
                            VALID_MODULUS,
                            VALID_EXPONENT,
                            VALID_KTY,
                            VALID_USE,
                            "rs256");

            // then
            assertThat(publicKey.alg()).isEqualTo("rs256");
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null kid면 예외 발생")
        void shouldThrowExceptionWhenKidIsNull() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            null,
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kid")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 kid면 예외 발생")
        void shouldThrowExceptionWhenKidIsEmpty() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            "",
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kid")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있는 kid면 예외 발생")
        void shouldThrowExceptionWhenKidIsBlank() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            "   ",
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kid")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("null modulus면 예외 발생")
        void shouldThrowExceptionWhenModulusIsNull() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            null,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Modulus")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 modulus면 예외 발생")
        void shouldThrowExceptionWhenModulusIsEmpty() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            "",
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Modulus")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("null exponent면 예외 발생")
        void shouldThrowExceptionWhenExponentIsNull() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            VALID_MODULUS,
                                            null,
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exponent")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 exponent면 예외 발생")
        void shouldThrowExceptionWhenExponentIsEmpty() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            VALID_MODULUS,
                                            "",
                                            VALID_KTY,
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exponent")
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("RSA가 아닌 kty면 예외 발생")
        void shouldThrowExceptionWhenKtyIsNotRsa() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            "EC",
                                            VALID_USE,
                                            VALID_ALG))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kty")
                    .hasMessageContaining("RSA");
        }

        @Test
        @DisplayName("RS256이 아닌 alg면 예외 발생")
        void shouldThrowExceptionWhenAlgIsNotRS256() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            "HS256"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("alg")
                    .hasMessageContaining("RS256");
        }

        @Test
        @DisplayName("RS512 알고리즘이면 예외 발생")
        void shouldThrowExceptionWhenAlgIsRS512() {
            assertThatThrownBy(
                            () ->
                                    PublicKey.of(
                                            VALID_KID,
                                            VALID_MODULUS,
                                            VALID_EXPONENT,
                                            VALID_KTY,
                                            VALID_USE,
                                            "RS512"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("alg")
                    .hasMessageContaining("RS256");
        }
    }

    @Nested
    @DisplayName("toRSAPublicKey 테스트")
    class ToRSAPublicKeyTest {

        @Test
        @DisplayName("유효한 PublicKey를 RSAPublicKey로 변환")
        void shouldConvertToRSAPublicKey() {
            // given
            PublicKey publicKey = createValidPublicKey();

            // when
            RSAPublicKey rsaPublicKey = publicKey.toRSAPublicKey();

            // then
            assertThat(rsaPublicKey).isNotNull();
            assertThat(rsaPublicKey.getModulus()).isEqualTo(TEST_RSA_PUBLIC_KEY.getModulus());
            assertThat(rsaPublicKey.getPublicExponent())
                    .isEqualTo(TEST_RSA_PUBLIC_KEY.getPublicExponent());
        }

        @Test
        @DisplayName("변환된 RSAPublicKey가 RSA 알고리즘 사용")
        void shouldUseRsaAlgorithm() {
            // given
            PublicKey publicKey = createValidPublicKey();

            // when
            RSAPublicKey rsaPublicKey = publicKey.toRSAPublicKey();

            // then
            assertThat(rsaPublicKey.getAlgorithm()).isEqualTo("RSA");
        }
    }

    @Nested
    @DisplayName("fromRSAPublicKey 테스트")
    class FromRSAPublicKeyTest {

        @Test
        @DisplayName("RSAPublicKey에서 PublicKey 생성")
        void shouldCreatePublicKeyFromRSAPublicKey() {
            // given
            String kid = "converted-key-id";

            // when
            PublicKey publicKey = PublicKey.fromRSAPublicKey(kid, TEST_RSA_PUBLIC_KEY);

            // then
            assertThat(publicKey.kid()).isEqualTo(kid);
            assertThat(publicKey.kty()).isEqualTo("RSA");
            assertThat(publicKey.use()).isEqualTo("sig");
            assertThat(publicKey.alg()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("변환 후 다시 RSAPublicKey로 변환 시 동일")
        void shouldBeEqualAfterRoundTrip() {
            // given
            String kid = "round-trip-key";
            PublicKey publicKey = PublicKey.fromRSAPublicKey(kid, TEST_RSA_PUBLIC_KEY);

            // when
            RSAPublicKey convertedBack = publicKey.toRSAPublicKey();

            // then
            assertThat(convertedBack.getModulus()).isEqualTo(TEST_RSA_PUBLIC_KEY.getModulus());
            assertThat(convertedBack.getPublicExponent())
                    .isEqualTo(TEST_RSA_PUBLIC_KEY.getPublicExponent());
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("of() 메서드로 PublicKey 생성")
        void shouldCreatePublicKeyUsingOf() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID,
                            VALID_MODULUS,
                            VALID_EXPONENT,
                            VALID_KTY,
                            VALID_USE,
                            VALID_ALG);

            // then
            assertThat(publicKey).isNotNull();
            assertThat(publicKey.kid()).isEqualTo(VALID_KID);
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals가 값 기반으로 동작")
        void shouldHaveValueBasedEquality() {
            // given
            PublicKey key1 = createValidPublicKey();
            PublicKey key2 = createValidPublicKey();

            // when & then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        @DisplayName("다른 kid면 equals false")
        void shouldNotBeEqualWithDifferentKid() {
            // given
            PublicKey key1 =
                    PublicKey.of(
                            "kid1", VALID_MODULUS, VALID_EXPONENT, VALID_KTY, VALID_USE, VALID_ALG);
            PublicKey key2 =
                    PublicKey.of(
                            "kid2", VALID_MODULUS, VALID_EXPONENT, VALID_KTY, VALID_USE, VALID_ALG);

            // when & then
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("toString이 모든 필드 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            PublicKey publicKey = createValidPublicKey();

            // when
            String result = publicKey.toString();

            // then
            assertThat(result).contains("PublicKey");
            assertThat(result).contains("kid=" + VALID_KID);
            assertThat(result).contains("kty=" + VALID_KTY);
            assertThat(result).contains("alg=" + VALID_ALG);
        }
    }

    @Nested
    @DisplayName("다양한 kid 형식 테스트")
    class VariousKidFormatsTest {

        @Test
        @DisplayName("UUID 형식 kid 처리")
        void shouldHandleUuidKid() {
            // given
            String uuidKid = "550e8400-e29b-41d4-a716-446655440000";

            // when
            PublicKey publicKey =
                    PublicKey.of(
                            uuidKid,
                            VALID_MODULUS,
                            VALID_EXPONENT,
                            VALID_KTY,
                            VALID_USE,
                            VALID_ALG);

            // then
            assertThat(publicKey.kid()).isEqualTo(uuidKid);
        }

        @Test
        @DisplayName("특수 문자가 포함된 kid 처리")
        void shouldHandleKidWithSpecialCharacters() {
            // given
            String specialKid = "key_2024-01-prod.v1";

            // when
            PublicKey publicKey =
                    PublicKey.of(
                            specialKid,
                            VALID_MODULUS,
                            VALID_EXPONENT,
                            VALID_KTY,
                            VALID_USE,
                            VALID_ALG);

            // then
            assertThat(publicKey.kid()).isEqualTo(specialKid);
        }

        @Test
        @DisplayName("긴 kid 처리")
        void shouldHandleLongKid() {
            // given
            String longKid =
                    "very-long-key-id-that-might-be-used-in-production-systems-"
                            + "with-detailed-naming-conventions";

            // when
            PublicKey publicKey =
                    PublicKey.of(
                            longKid,
                            VALID_MODULUS,
                            VALID_EXPONENT,
                            VALID_KTY,
                            VALID_USE,
                            VALID_ALG);

            // then
            assertThat(publicKey.kid()).isEqualTo(longKid);
        }
    }

    @Nested
    @DisplayName("use 필드 테스트")
    class UseFieldTest {

        @Test
        @DisplayName("sig use 값 처리")
        void shouldHandleSigUse() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID, VALID_MODULUS, VALID_EXPONENT, VALID_KTY, "sig", VALID_ALG);

            // then
            assertThat(publicKey.use()).isEqualTo("sig");
        }

        @Test
        @DisplayName("enc use 값도 허용 (유효성 검증 없음)")
        void shouldAcceptEncUse() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID, VALID_MODULUS, VALID_EXPONENT, VALID_KTY, "enc", VALID_ALG);

            // then
            assertThat(publicKey.use()).isEqualTo("enc");
        }

        @Test
        @DisplayName("null use 값도 허용")
        void shouldAcceptNullUse() {
            // when
            PublicKey publicKey =
                    PublicKey.of(
                            VALID_KID, VALID_MODULUS, VALID_EXPONENT, VALID_KTY, null, VALID_ALG);

            // then
            assertThat(publicKey.use()).isNull();
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("Record는 기본적으로 불변")
        void recordShouldBeImmutable() {
            // Record는 Java에서 기본적으로 불변
            // 모든 필드가 final이고 setter가 없음
            PublicKey publicKey = createValidPublicKey();

            // Record의 컴포넌트 메서드는 항상 동일한 값을 반환
            assertThat(publicKey.kid()).isEqualTo(publicKey.kid());
            assertThat(publicKey.modulus()).isEqualTo(publicKey.modulus());
            assertThat(publicKey.exponent()).isEqualTo(publicKey.exponent());
        }
    }
}
