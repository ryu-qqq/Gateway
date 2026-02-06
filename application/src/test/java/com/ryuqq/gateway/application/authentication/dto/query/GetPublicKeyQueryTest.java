package com.ryuqq.gateway.application.authentication.dto.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GetPublicKeyQuery 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("GetPublicKeyQuery 단위 테스트")
class GetPublicKeyQueryTest {

    @Nested
    @DisplayName("생성 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 kid로 생성")
        void shouldCreateWithValidKid() {
            // given
            String kid = "test-kid";

            // when
            GetPublicKeyQuery query = new GetPublicKeyQuery(kid);

            // then
            assertThat(query.kid()).isEqualTo(kid);
        }

        @Test
        @DisplayName("null kid도 허용")
        void shouldAllowNullKid() {
            // when
            GetPublicKeyQuery query = new GetPublicKeyQuery(null);

            // then
            assertThat(query.kid()).isNull();
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("of() 메서드로 생성")
        void shouldCreateUsingStaticFactoryMethod() {
            // given
            String kid = "key-123";

            // when
            GetPublicKeyQuery query = GetPublicKeyQuery.of(kid);

            // then
            assertThat(query.kid()).isEqualTo(kid);
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals()가 올바르게 동작")
        void shouldHaveCorrectEquals() {
            // given
            GetPublicKeyQuery query1 = GetPublicKeyQuery.of("kid-1");
            GetPublicKeyQuery query2 = GetPublicKeyQuery.of("kid-1");
            GetPublicKeyQuery query3 = GetPublicKeyQuery.of("kid-2");

            // then
            assertThat(query1).isEqualTo(query2);
            assertThat(query1).isNotEqualTo(query3);
        }

        @Test
        @DisplayName("hashCode()가 올바르게 동작")
        void shouldHaveCorrectHashCode() {
            // given
            GetPublicKeyQuery query1 = GetPublicKeyQuery.of("kid-1");
            GetPublicKeyQuery query2 = GetPublicKeyQuery.of("kid-1");

            // then
            assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
        }

        @Test
        @DisplayName("toString()이 kid를 포함")
        void shouldIncludeKidInToString() {
            // given
            GetPublicKeyQuery query = GetPublicKeyQuery.of("test-kid-123");

            // then
            assertThat(query.toString()).contains("test-kid-123");
        }
    }
}
