package com.ryuqq.gateway.application.authentication.dto.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * GetPublicKeyQuery 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("GetPublicKeyQuery 테스트")
class GetPublicKeyQueryTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 kid로 생성")
        void shouldCreateWithValidKid() {
            // given
            String kid = "key-id-123";

            // when
            GetPublicKeyQuery query = new GetPublicKeyQuery(kid);

            // then
            assertThat(query.kid()).isEqualTo("key-id-123");
        }

        @Test
        @DisplayName("UUID 형태의 kid로 생성")
        void shouldCreateWithUuidKid() {
            // given
            String kid = "550e8400-e29b-41d4-a716-446655440000";

            // when
            GetPublicKeyQuery query = new GetPublicKeyQuery(kid);

            // then
            assertThat(query.kid()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열 kid로 생성 시 예외 발생")
        void shouldThrowExceptionForNullOrEmptyKid(String invalidKid) {
            // when & then
            assertThatThrownBy(() -> new GetPublicKeyQuery(invalidKid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Kid cannot be null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t", "\n", "  \t  \n  "})
        @DisplayName("공백 문자열 kid로 생성 시 예외 발생")
        void shouldThrowExceptionForBlankKid(String blankKid) {
            // when & then
            assertThatThrownBy(() -> new GetPublicKeyQuery(blankKid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Kid cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Record 동등성 테스트")
    class EqualityTest {

        @Test
        @DisplayName("동일한 kid를 가진 쿼리는 동등함")
        void shouldBeEqualWithSameKid() {
            // given
            GetPublicKeyQuery query1 = new GetPublicKeyQuery("same-kid");
            GetPublicKeyQuery query2 = new GetPublicKeyQuery("same-kid");

            // then
            assertThat(query1).isEqualTo(query2);
            assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
        }

        @Test
        @DisplayName("다른 kid를 가진 쿼리는 동등하지 않음")
        void shouldNotBeEqualWithDifferentKid() {
            // given
            GetPublicKeyQuery query1 = new GetPublicKeyQuery("kid-1");
            GetPublicKeyQuery query2 = new GetPublicKeyQuery("kid-2");

            // then
            assertThat(query1).isNotEqualTo(query2);
        }
    }
}
