package com.ryuqq.gateway.domain.tenant.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SocialProvider Enum 테스트")
class SocialProviderTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValueTest {

        @Test
        @DisplayName("KAKAO 코드와 표시명 확인")
        void shouldHaveCorrectKakaoValues() {
            // when & then
            assertThat(SocialProvider.KAKAO.getCode()).isEqualTo("kakao");
            assertThat(SocialProvider.KAKAO.getDisplayName()).isEqualTo("Kakao");
        }

        @Test
        @DisplayName("NAVER 코드와 표시명 확인")
        void shouldHaveCorrectNaverValues() {
            // when & then
            assertThat(SocialProvider.NAVER.getCode()).isEqualTo("naver");
            assertThat(SocialProvider.NAVER.getDisplayName()).isEqualTo("Naver");
        }

        @Test
        @DisplayName("GOOGLE 코드와 표시명 확인")
        void shouldHaveCorrectGoogleValues() {
            // when & then
            assertThat(SocialProvider.GOOGLE.getCode()).isEqualTo("google");
            assertThat(SocialProvider.GOOGLE.getDisplayName()).isEqualTo("Google");
        }

        @Test
        @DisplayName("전체 values() 개수 확인")
        void shouldHaveThreeProviders() {
            // when & then
            assertThat(SocialProvider.values()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("fromCode() 메서드 테스트")
    class FromCodeTest {

        @Test
        @DisplayName("소문자 코드로 KAKAO 조회")
        void shouldFindKakaoByLowercaseCode() {
            // when
            SocialProvider provider = SocialProvider.fromCode("kakao");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("소문자 코드로 NAVER 조회")
        void shouldFindNaverByLowercaseCode() {
            // when
            SocialProvider provider = SocialProvider.fromCode("naver");

            // then
            assertThat(provider).isEqualTo(SocialProvider.NAVER);
        }

        @Test
        @DisplayName("소문자 코드로 GOOGLE 조회")
        void shouldFindGoogleByLowercaseCode() {
            // when
            SocialProvider provider = SocialProvider.fromCode("google");

            // then
            assertThat(provider).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("대문자 코드도 조회 성공 (case insensitive)")
        void shouldFindByUppercaseCode() {
            // when
            SocialProvider provider = SocialProvider.fromCode("KAKAO");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("혼합 대소문자 코드도 조회 성공")
        void shouldFindByMixedCaseCode() {
            // when
            SocialProvider provider = SocialProvider.fromCode("KaKaO");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("null 코드면 예외 발생")
        void shouldThrowExceptionWhenCodeIsNull() {
            assertThatThrownBy(() -> SocialProvider.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열 코드면 예외 발생")
        void shouldThrowExceptionWhenCodeIsEmpty() {
            assertThatThrownBy(() -> SocialProvider.fromCode(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있는 코드면 예외 발생")
        void shouldThrowExceptionWhenCodeIsBlank() {
            assertThatThrownBy(() -> SocialProvider.fromCode("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("존재하지 않는 코드면 예외 발생")
        void shouldThrowExceptionWhenCodeUnknown() {
            assertThatThrownBy(() -> SocialProvider.fromCode("facebook"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown social provider code");
        }
    }

    @Nested
    @DisplayName("fromName() 메서드 테스트")
    class FromNameTest {

        @Test
        @DisplayName("대문자 이름으로 KAKAO 조회")
        void shouldFindKakaoByUppercaseName() {
            // when
            SocialProvider provider = SocialProvider.fromName("KAKAO");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("대문자 이름으로 NAVER 조회")
        void shouldFindNaverByUppercaseName() {
            // when
            SocialProvider provider = SocialProvider.fromName("NAVER");

            // then
            assertThat(provider).isEqualTo(SocialProvider.NAVER);
        }

        @Test
        @DisplayName("대문자 이름으로 GOOGLE 조회")
        void shouldFindGoogleByUppercaseName() {
            // when
            SocialProvider provider = SocialProvider.fromName("GOOGLE");

            // then
            assertThat(provider).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("소문자 이름도 조회 성공 (case insensitive)")
        void shouldFindByLowercaseName() {
            // when
            SocialProvider provider = SocialProvider.fromName("kakao");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("혼합 대소문자 이름도 조회 성공")
        void shouldFindByMixedCaseName() {
            // when
            SocialProvider provider = SocialProvider.fromName("kAkAo");

            // then
            assertThat(provider).isEqualTo(SocialProvider.KAKAO);
        }

        @Test
        @DisplayName("null 이름이면 예외 발생")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThatThrownBy(() -> SocialProvider.fromName(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열 이름이면 예외 발생")
        void shouldThrowExceptionWhenNameIsEmpty() {
            assertThatThrownBy(() -> SocialProvider.fromName(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있는 이름이면 예외 발생")
        void shouldThrowExceptionWhenNameIsBlank() {
            assertThatThrownBy(() -> SocialProvider.fromName("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("존재하지 않는 이름이면 예외 발생")
        void shouldThrowExceptionWhenNameUnknown() {
            assertThatThrownBy(() -> SocialProvider.fromName("FACEBOOK"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown social provider name");
        }
    }

    @Nested
    @DisplayName("Enum 기본 동작 테스트")
    class EnumBasicsTest {

        @Test
        @DisplayName("name() 메서드 반환값 확인")
        void shouldReturnCorrectName() {
            // when & then
            assertThat(SocialProvider.KAKAO.name()).isEqualTo("KAKAO");
            assertThat(SocialProvider.NAVER.name()).isEqualTo("NAVER");
            assertThat(SocialProvider.GOOGLE.name()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("ordinal() 메서드 반환값 확인")
        void shouldReturnCorrectOrdinal() {
            // when & then
            assertThat(SocialProvider.KAKAO.ordinal()).isZero();
            assertThat(SocialProvider.NAVER.ordinal()).isEqualTo(1);
            assertThat(SocialProvider.GOOGLE.ordinal()).isEqualTo(2);
        }

        @Test
        @DisplayName("valueOf() 메서드로 조회 가능")
        void shouldFindByValueOf() {
            // when & then
            assertThat(SocialProvider.valueOf("KAKAO")).isEqualTo(SocialProvider.KAKAO);
            assertThat(SocialProvider.valueOf("NAVER")).isEqualTo(SocialProvider.NAVER);
            assertThat(SocialProvider.valueOf("GOOGLE")).isEqualTo(SocialProvider.GOOGLE);
        }
    }
}
