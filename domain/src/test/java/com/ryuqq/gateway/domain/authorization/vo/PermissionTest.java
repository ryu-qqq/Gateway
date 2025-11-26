package com.ryuqq.gateway.domain.authorization.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Permission 단위 테스트")
class PermissionTest {

    @Nested
    @DisplayName("생성 및 팩토리 메서드")
    class Creation {

        @Test
        @DisplayName("유효한 권한 문자열로 Permission 생성 성공")
        void createPermissionWithValidValue() {
            // given
            String validPermission = "order:read";

            // when
            Permission permission = Permission.of(validPermission);

            // then
            assertThat(permission.value()).isEqualTo(validPermission);
        }

        @Test
        @DisplayName("와일드카드 권한 생성 성공")
        void createWildcardPermission() {
            // given
            String wildcardPermission = "order:*";

            // when
            Permission permission = Permission.of(wildcardPermission);

            // then
            assertThat(permission.value()).isEqualTo(wildcardPermission);
            assertThat(permission.isWildcard()).isTrue();
        }

        @Test
        @DisplayName("null 값으로 생성 시 예외 발생")
        void throwExceptionWhenValueIsNull() {
            // when & then
            assertThatThrownBy(() -> Permission.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Permission value cannot be null");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("빈 문자열로 생성 시 예외 발생")
        void throwExceptionWhenValueIsBlank(String blankValue) {
            // when & then
            if (blankValue == null) {
                assertThatThrownBy(() -> Permission.of(blankValue))
                        .isInstanceOf(NullPointerException.class);
            } else {
                assertThatThrownBy(() -> Permission.of(blankValue))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Permission value cannot be blank");
            }
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "invalid", // 콜론 없음
                    "order", // 액션 없음
                    ":read", // 리소스 없음
                    "order:", // 액션 빈 값
                    "Order:read", // 대문자 시작
                    "order:Read", // 액션 대문자
                    "order-read", // 콜론 대신 하이픈
                    "order::read", // 콜론 중복
                    "order:read:extra", // 추가 세그먼트
                    "123order:read", // 숫자로 시작
                    "order:123read" // 액션이 숫자로 시작
                })
        @DisplayName("잘못된 형식으로 생성 시 예외 발생")
        void throwExceptionWhenFormatIsInvalid(String invalidFormat) {
            // when & then
            assertThatThrownBy(() -> Permission.of(invalidFormat))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid permission format");
        }
    }

    @Nested
    @DisplayName("리소스 및 액션 추출")
    class ResourceAndAction {

        @Test
        @DisplayName("리소스 부분 추출")
        void extractResource() {
            // given
            Permission permission = Permission.of("order:read");

            // when
            String resource = permission.resource();

            // then
            assertThat(resource).isEqualTo("order");
        }

        @Test
        @DisplayName("액션 부분 추출")
        void extractAction() {
            // given
            Permission permission = Permission.of("order:read");

            // when
            String action = permission.action();

            // then
            assertThat(action).isEqualTo("read");
        }

        @Test
        @DisplayName("와일드카드 액션 추출")
        void extractWildcardAction() {
            // given
            Permission permission = Permission.of("order:*");

            // when
            String action = permission.action();

            // then
            assertThat(action).isEqualTo("*");
        }

        @Test
        @DisplayName("하이픈이 포함된 리소스 추출")
        void extractResourceWithHyphen() {
            // given
            Permission permission = Permission.of("order-item:read");

            // when
            String resource = permission.resource();

            // then
            assertThat(resource).isEqualTo("order-item");
        }
    }

    @Nested
    @DisplayName("와일드카드 확인")
    class WildcardCheck {

        @Test
        @DisplayName("와일드카드 권한인 경우 true 반환")
        void returnTrueForWildcardPermission() {
            // given
            Permission permission = Permission.of("order:*");

            // when
            boolean isWildcard = permission.isWildcard();

            // then
            assertThat(isWildcard).isTrue();
        }

        @Test
        @DisplayName("일반 권한인 경우 false 반환")
        void returnFalseForNormalPermission() {
            // given
            Permission permission = Permission.of("order:read");

            // when
            boolean isWildcard = permission.isWildcard();

            // then
            assertThat(isWildcard).isFalse();
        }
    }

    @Nested
    @DisplayName("권한 포함 여부 확인 (와일드카드 매칭)")
    class IncludesCheck {

        @Test
        @DisplayName("와일드카드 권한은 같은 리소스의 모든 액션 포함")
        void wildcardIncludesAllActionsOfSameResource() {
            // given
            Permission wildcard = Permission.of("order:*");
            Permission read = Permission.of("order:read");
            Permission create = Permission.of("order:create");
            Permission delete = Permission.of("order:delete");

            // when & then
            assertThat(wildcard.includes(read)).isTrue();
            assertThat(wildcard.includes(create)).isTrue();
            assertThat(wildcard.includes(delete)).isTrue();
        }

        @Test
        @DisplayName("와일드카드 권한은 다른 리소스의 액션 포함하지 않음")
        void wildcardDoesNotIncludeDifferentResource() {
            // given
            Permission orderWildcard = Permission.of("order:*");
            Permission productRead = Permission.of("product:read");

            // when & then
            assertThat(orderWildcard.includes(productRead)).isFalse();
        }

        @Test
        @DisplayName("일반 권한은 동일한 권한만 포함")
        void normalPermissionIncludesOnlyExactMatch() {
            // given
            Permission orderRead = Permission.of("order:read");
            Permission samePermission = Permission.of("order:read");
            Permission orderCreate = Permission.of("order:create");

            // when & then
            assertThat(orderRead.includes(samePermission)).isTrue();
            assertThat(orderRead.includes(orderCreate)).isFalse();
        }

        @Test
        @DisplayName("null 권한은 포함하지 않음")
        void doesNotIncludeNullPermission() {
            // given
            Permission permission = Permission.of("order:read");

            // when & then
            assertThat(permission.includes(null)).isFalse();
        }

        @Test
        @DisplayName("리소스가 다르면 포함하지 않음")
        void doesNotIncludeWhenResourceDiffers() {
            // given
            Permission orderRead = Permission.of("order:read");
            Permission productRead = Permission.of("product:read");

            // when & then
            assertThat(orderRead.includes(productRead)).isFalse();
        }

        @Test
        @DisplayName("와일드카드 권한끼리 비교")
        void wildcardIncludesWildcard() {
            // given
            Permission wildcard1 = Permission.of("order:*");
            Permission wildcard2 = Permission.of("order:*");

            // when & then
            assertThat(wildcard1.includes(wildcard2)).isTrue();
        }
    }

    @Nested
    @DisplayName("동등성 및 해시코드")
    class EqualityAndHashCode {

        @Test
        @DisplayName("같은 값을 가진 Permission은 동등함")
        void equalPermissionsWithSameValue() {
            // given
            Permission permission1 = Permission.of("order:read");
            Permission permission2 = Permission.of("order:read");

            // when & then
            assertThat(permission1).isEqualTo(permission2);
            assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 Permission은 동등하지 않음")
        void notEqualPermissionsWithDifferentValue() {
            // given
            Permission permission1 = Permission.of("order:read");
            Permission permission2 = Permission.of("order:create");

            // when & then
            assertThat(permission1).isNotEqualTo(permission2);
        }
    }
}
