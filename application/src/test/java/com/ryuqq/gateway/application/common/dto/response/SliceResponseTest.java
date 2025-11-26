package com.ryuqq.gateway.application.common.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SliceResponse 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("SliceResponse 테스트")
class SliceResponseTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 값으로 SliceResponse 생성")
        void shouldCreateSliceResponseWithValidValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2", "item3");
            int size = 20;
            boolean hasNext = true;
            String nextCursor = "cursor-123";

            // when
            SliceResponse<String> response =
                    new SliceResponse<>(content, size, hasNext, nextCursor);

            // then
            assertThat(response.content()).hasSize(3);
            assertThat(response.content()).containsExactly("item1", "item2", "item3");
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo("cursor-123");
        }

        @Test
        @DisplayName("null content로 생성 시 빈 리스트로 변환")
        void shouldConvertNullContentToEmptyList() {
            // when
            SliceResponse<String> response = new SliceResponse<>(null, 10, false, null);

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
            SliceResponse<String> response =
                    new SliceResponse<>(originalList, 10, true, "cursor");

            // then
            assertThat(response.content()).isNotSameAs(originalList);
            assertThat(response.content()).containsExactlyElementsOf(originalList);
        }
    }

    @Nested
    @DisplayName("of 메서드 테스트 (커서 포함)")
    class OfWithCursorTest {

        @Test
        @DisplayName("커서 포함 SliceResponse 생성")
        void shouldCreateSliceResponseWithCursor() {
            // given
            List<Integer> content = Arrays.asList(1, 2, 3, 4, 5);
            int size = 5;
            boolean hasNext = true;
            String nextCursor = "next-cursor-456";

            // when
            SliceResponse<Integer> response =
                    SliceResponse.of(content, size, hasNext, nextCursor);

            // then
            assertThat(response.content()).hasSize(5);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo("next-cursor-456");
        }

        @Test
        @DisplayName("hasNext가 false일 때")
        void shouldCreateSliceResponseWithNoNext() {
            // given
            List<String> content = Arrays.asList("last1", "last2");

            // when
            SliceResponse<String> response = SliceResponse.of(content, 10, false, null);

            // then
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("of 메서드 테스트 (커서 없음)")
    class OfWithoutCursorTest {

        @Test
        @DisplayName("커서 없이 SliceResponse 생성")
        void shouldCreateSliceResponseWithoutCursor() {
            // given
            List<String> content = Arrays.asList("a", "b", "c");

            // when
            SliceResponse<String> response = SliceResponse.of(content, 20, true);

            // then
            assertThat(response.content()).hasSize(3);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("마지막 페이지일 때 커서 없이 생성")
        void shouldCreateLastSliceWithoutCursor() {
            // given
            List<String> content = Arrays.asList("final1", "final2");

            // when
            SliceResponse<String> response = SliceResponse.of(content, 10, false);

            // then
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("empty 메서드 테스트")
    class EmptyTest {

        @Test
        @DisplayName("빈 SliceResponse 생성")
        void shouldCreateEmptySliceResponse() {
            // when
            SliceResponse<String> response = SliceResponse.empty(15);

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.size()).isEqualTo(15);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("다양한 size로 빈 SliceResponse 생성")
        void shouldCreateEmptySliceResponseWithVariousSize() {
            // when
            SliceResponse<Integer> response1 = SliceResponse.empty(10);
            SliceResponse<Integer> response2 = SliceResponse.empty(50);
            SliceResponse<Integer> response3 = SliceResponse.empty(100);

            // then
            assertThat(response1.size()).isEqualTo(10);
            assertThat(response2.size()).isEqualTo(50);
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
        @DisplayName("동일한 값을 가진 SliceResponse는 동등함")
        void shouldBeEqualWithSameValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2");
            SliceResponse<String> response1 =
                    SliceResponse.of(content, 10, true, "cursor");
            SliceResponse<String> response2 =
                    SliceResponse.of(content, 10, true, "cursor");

            // then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 SliceResponse는 동등하지 않음")
        void shouldNotBeEqualWithDifferentValues() {
            // given
            List<String> content = Arrays.asList("item1", "item2");
            SliceResponse<String> response1 =
                    SliceResponse.of(content, 10, true, "cursor1");
            SliceResponse<String> response2 =
                    SliceResponse.of(content, 10, true, "cursor2");

            // then
            assertThat(response1).isNotEqualTo(response2);
        }
    }
}
