package com.ryuqq.gateway.domain.trace.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryuqq.gateway.domain.common.util.ClockHolder;
import com.ryuqq.gateway.domain.trace.exception.InvalidTraceIdException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TraceId VO 테스트")
class TraceIdTest {

    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile(
                    "^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    private ClockHolder createFixedClockHolder(String instant) {
        return () -> Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
    }

    @Nested
    @DisplayName("generate() 메서드 테스트")
    class GenerateTest {

        @Test
        @DisplayName("ClockHolder를 사용하여 TraceId 생성")
        void shouldGenerateTraceIdWithClockHolder() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.789Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId).isNotNull();
            assertThat(traceId.getValue()).isNotNull();
            assertThat(traceId.getValue()).hasSize(54);
        }

        @Test
        @DisplayName("생성된 TraceId가 올바른 형식을 가짐")
        void shouldGenerateValidFormat() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.789Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getValue()).matches(TRACE_ID_PATTERN);
        }

        @Test
        @DisplayName("생성된 TraceId의 Timestamp가 ClockHolder 시간과 일치")
        void shouldHaveCorrectTimestamp() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.789Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getTimestamp()).isEqualTo("20250124123456789");
        }

        @Test
        @DisplayName("null ClockHolder 시 NullPointerException 발생")
        void shouldThrowExceptionWhenClockHolderIsNull() {
            assertThatThrownBy(() -> TraceId.generate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ClockHolder cannot be null");
        }

        @Test
        @DisplayName("여러 번 생성 시 서로 다른 UUID 사용")
        void shouldGenerateDifferentUuids() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.789Z");

            // when
            TraceId traceId1 = TraceId.generate(clockHolder);
            TraceId traceId2 = TraceId.generate(clockHolder);

            // then
            assertThat(traceId1.getUuid()).isNotEqualTo(traceId2.getUuid());
            assertThat(traceId1.getValue()).isNotEqualTo(traceId2.getValue());
        }

        @Test
        @DisplayName("동시에 1000개 생성 시 모두 유일함")
        void shouldGenerateUniqueTraceIds() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.789Z");
            Set<String> traceIds = new HashSet<>();

            // when
            for (int i = 0; i < 1000; i++) {
                TraceId traceId = TraceId.generate(clockHolder);
                traceIds.add(traceId.getValue());
            }

            // then
            assertThat(traceIds).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("from() 메서드 테스트")
    class FromTest {

        @Test
        @DisplayName("유효한 TraceId 문자열로 TraceId 생성")
        void shouldCreateFromValidString() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

            // when
            TraceId traceId = TraceId.from(validTraceId);

            // then
            assertThat(traceId).isNotNull();
            assertThat(traceId.getValue()).isEqualTo(validTraceId);
        }

        @Test
        @DisplayName("null 값이면 예외 발생")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> TraceId.from(null))
                    .isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> TraceId.from("")).isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("공백만 있으면 예외 발생")
        void shouldThrowExceptionWhenBlank() {
            assertThatThrownBy(() -> TraceId.from("   "))
                    .isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("길이가 54자가 아니면 예외 발생")
        void shouldThrowExceptionWhenInvalidLength() {
            assertThatThrownBy(() -> TraceId.from("short-trace-id"))
                    .isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("형식이 올바르지 않으면 예외 발생")
        void shouldThrowExceptionWhenInvalidFormat() {
            // given - 길이는 맞지만 형식이 다른 경우
            String invalidFormat = "abcdefghijklmnopq-12345678-1234-1234-1234-123456789012";

            assertThatThrownBy(() -> TraceId.from(invalidFormat))
                    .isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("UUID 부분이 대문자면 예외 발생")
        void shouldThrowExceptionWhenUuidIsUppercase() {
            // given - UUID가 대문자
            String uppercaseUuid = "20250124123456789-A1B2C3D4-E5F6-4789-ABCD-EF0123456789";

            assertThatThrownBy(() -> TraceId.from(uppercaseUuid))
                    .isInstanceOf(InvalidTraceIdException.class);
        }

        @Test
        @DisplayName("Timestamp에 문자가 포함되면 예외 발생")
        void shouldThrowExceptionWhenTimestampContainsLetters() {
            // given
            String invalidTimestamp = "2025012412345abc-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

            assertThatThrownBy(() -> TraceId.from(invalidTimestamp))
                    .isInstanceOf(InvalidTraceIdException.class);
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getValue()가 전체 TraceId 반환")
        void shouldReturnFullValue() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceId traceId = TraceId.from(validTraceId);

            // when & then
            assertThat(traceId.getValue()).isEqualTo(validTraceId);
        }

        @Test
        @DisplayName("getTimestamp()가 17자리 Timestamp 반환")
        void shouldReturnTimestamp() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceId traceId = TraceId.from(validTraceId);

            // when & then
            assertThat(traceId.getTimestamp()).isEqualTo("20250124123456789");
            assertThat(traceId.getTimestamp()).hasSize(17);
        }

        @Test
        @DisplayName("getUuid()가 UUID 부분 반환")
        void shouldReturnUuid() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceId traceId = TraceId.from(validTraceId);

            // when & then
            assertThat(traceId.getUuid()).isEqualTo("a1b2c3d4-e5f6-4789-abcd-ef0123456789");
            assertThat(traceId.getUuid()).hasSize(36);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValue() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceId traceId1 = TraceId.from(validTraceId);
            TraceId traceId2 = TraceId.from(validTraceId);

            // when & then
            assertThat(traceId1).isEqualTo(traceId2);
            assertThat(traceId1.hashCode()).isEqualTo(traceId2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValue() {
            // given
            TraceId traceId1 =
                    TraceId.from("20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789");
            TraceId traceId2 =
                    TraceId.from("20250124123456789-11111111-2222-3333-4444-555555555555");

            // when & then
            assertThat(traceId1).isNotEqualTo(traceId2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            TraceId traceId =
                    TraceId.from("20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789");

            // when & then
            assertThat(traceId).isEqualTo(traceId);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            TraceId traceId =
                    TraceId.from("20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789");

            // when & then
            assertThat(traceId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            TraceId traceId =
                    TraceId.from("20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789");

            // when & then
            assertThat(traceId).isNotEqualTo("not a TraceId");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 TraceId 정보 포함")
        void shouldIncludeTraceIdInToString() {
            // given
            String validTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceId traceId = TraceId.from(validTraceId);

            // when
            String result = traceId.toString();

            // then
            assertThat(result).contains("TraceId");
            assertThat(result).contains(validTraceId);
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(TraceId.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = TraceId.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.isSynthetic()
                        && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                            .as("Field '%s' should be final", field.getName())
                            .isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("다양한 시간대 테스트")
    class TimeZoneTest {

        @Test
        @DisplayName("UTC 시간대에서 올바른 Timestamp 생성")
        void shouldGenerateCorrectTimestampInUtc() {
            // given
            ClockHolder clockHolder =
                    () -> Clock.fixed(Instant.parse("2025-06-15T08:30:45.123Z"), ZoneId.of("UTC"));

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getTimestamp()).isEqualTo("20250615083045123");
        }

        @Test
        @DisplayName("다른 시간대에서도 올바른 Timestamp 생성")
        void shouldGenerateCorrectTimestampInOtherTimeZone() {
            // given - Asia/Seoul (+09:00)
            ClockHolder clockHolder =
                    () ->
                            Clock.fixed(
                                    Instant.parse("2025-06-15T08:30:45.123Z"),
                                    ZoneId.of("Asia/Seoul"));

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then - UTC 08:30 -> Seoul 17:30
            assertThat(traceId.getTimestamp()).isEqualTo("20250615173045123");
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("밀리초가 000인 경우")
        void shouldHandleZeroMilliseconds() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.000Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getTimestamp()).isEqualTo("20250124123456000");
        }

        @Test
        @DisplayName("밀리초가 999인 경우")
        void shouldHandleMaxMilliseconds() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2025-01-24T12:34:56.999Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getTimestamp()).isEqualTo("20250124123456999");
        }

        @Test
        @DisplayName("연도가 다른 경우")
        void shouldHandleDifferentYears() {
            // given
            ClockHolder clockHolder = createFixedClockHolder("2030-12-31T23:59:59.999Z");

            // when
            TraceId traceId = TraceId.generate(clockHolder);

            // then
            assertThat(traceId.getTimestamp()).isEqualTo("20301231235959999");
        }
    }
}
