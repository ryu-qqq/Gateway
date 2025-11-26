package com.ryuqq.gateway.application.common.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PageResponse 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PageResponse 테스트")
class PageResponseTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 값으로 PageResponse 생성")
        void shouldCreatePageResponseWithValidValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2", "item3");
            int page = 0;
            int size = 20;
            long totalElements = 100L;
            int totalPages = 5;
            boolean first = true;
            boolean last = false;

            // when
            PageResponse<String> response =
                    new PageResponse<>(content, page, size, totalElements, totalPages, first, last);

            // then
            assertThat(response.content()).hasSize(3);
            assertThat(response.content()).containsExactly("item1", "item2", "item3");
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.totalElements()).isEqualTo(100L);
            assertThat(response.totalPages()).isEqualTo(5);
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isFalse();
        }

        @Test
        @DisplayName("null content로 생성 시 빈 리스트로 변환")
        void shouldConvertNullContentToEmptyList() {
            // when
            PageResponse<String> response =
                    new PageResponse<>(null, 0, 10, 0L, 0, true, true);

            // then
            assertThat(response.content()).isNotNull();
            assertThat(response.content()).isEmpty();
        }

        @Test
        @DisplayName("content는 불변 복사본으로 저장됨")
        void shouldStoreContentAsImmutableCopy() {
            // given
            List<String> originalList = Arrays.asList("item1", "item2");

            // when
            PageResponse<String> response =
                    new PageResponse<>(originalList, 0, 10, 2L, 1, true, true);

            // then
            assertThat(response.content()).isNotSameAs(originalList);
            assertThat(response.content()).containsExactlyElementsOf(originalList);
        }

        @Test
        @DisplayName("마지막 페이지 속성 확인")
        void shouldCreateLastPage() {
            // given
            List<String> content = Arrays.asList("lastItem1", "lastItem2");

            // when
            PageResponse<String> response =
                    new PageResponse<>(content, 4, 20, 100L, 5, false, true);

            // then
            assertThat(response.page()).isEqualTo(4);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isTrue();
        }
    }

    @Nested
    @DisplayName("of 메서드 테스트")
    class OfTest {

        @Test
        @DisplayName("첫 페이지 생성")
        void shouldCreateFirstPage() {
            // given
            List<Integer> content = Arrays.asList(1, 2, 3, 4, 5);

            // when
            PageResponse<Integer> response =
                    PageResponse.of(content, 0, 5, 25L, 5, true, false);

            // then
            assertThat(response.content()).hasSize(5);
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalElements()).isEqualTo(25L);
            assertThat(response.totalPages()).isEqualTo(5);
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isFalse();
        }

        @Test
        @DisplayName("중간 페이지 생성")
        void shouldCreateMiddlePage() {
            // given
            List<String> content = Arrays.asList("mid1", "mid2", "mid3");

            // when
            PageResponse<String> response =
                    PageResponse.of(content, 2, 10, 50L, 5, false, false);

            // then
            assertThat(response.page()).isEqualTo(2);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isFalse();
        }

        @Test
        @DisplayName("마지막 페이지 생성")
        void shouldCreateLastPage() {
            // given
            List<String> content = Arrays.asList("last1", "last2");

            // when
            PageResponse<String> response =
                    PageResponse.of(content, 4, 10, 42L, 5, false, true);

            // then
            assertThat(response.page()).isEqualTo(4);
            assertThat(response.first()).isFalse();
            assertThat(response.last()).isTrue();
            assertThat(response.totalElements()).isEqualTo(42L);
        }

        @Test
        @DisplayName("단일 페이지 (첫이자 마지막)")
        void shouldCreateSinglePage() {
            // given
            List<String> content = Arrays.asList("only1", "only2", "only3");

            // when
            PageResponse<String> response =
                    PageResponse.of(content, 0, 10, 3L, 1, true, true);

            // then
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isTrue();
            assertThat(response.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("empty 메서드 테스트")
    class EmptyTest {

        @Test
        @DisplayName("빈 PageResponse 생성")
        void shouldCreateEmptyPageResponse() {
            // when
            PageResponse<String> response = PageResponse.empty(0, 20);

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.first()).isTrue();
            assertThat(response.last()).isTrue();
        }

        @Test
        @DisplayName("다양한 페이지와 사이즈로 빈 PageResponse 생성")
        void shouldCreateEmptyPageResponseWithVariousPageAndSize() {
            // when
            PageResponse<Integer> response1 = PageResponse.empty(0, 10);
            PageResponse<Integer> response2 = PageResponse.empty(5, 50);
            PageResponse<Integer> response3 = PageResponse.empty(10, 100);

            // then
            assertThat(response1.page()).isZero();
            assertThat(response1.size()).isEqualTo(10);

            assertThat(response2.page()).isEqualTo(5);
            assertThat(response2.size()).isEqualTo(50);

            assertThat(response3.page()).isEqualTo(10);
            assertThat(response3.size()).isEqualTo(100);

            assertThat(response1.content()).isEmpty();
            assertThat(response2.content()).isEmpty();
            assertThat(response3.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record 동등성 테스트")
    class EqualityTest {

        @Test
        @DisplayName("동일한 값을 가진 PageResponse는 동등함")
        void shouldBeEqualWithSameValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2");
            PageResponse<String> response1 =
                    PageResponse.of(content, 0, 10, 100L, 10, true, false);
            PageResponse<String> response2 =
                    PageResponse.of(content, 0, 10, 100L, 10, true, false);

            // then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 PageResponse는 동등하지 않음")
        void shouldNotBeEqualWithDifferentValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2");
            PageResponse<String> response1 =
                    PageResponse.of(content, 0, 10, 100L, 10, true, false);
            PageResponse<String> response2 =
                    PageResponse.of(content, 1, 10, 100L, 10, false, false);

            // then
            assertThat(response1).isNotEqualTo(response2);
        }
    }
}
