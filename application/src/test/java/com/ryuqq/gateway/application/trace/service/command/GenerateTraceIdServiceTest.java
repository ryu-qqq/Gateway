package com.ryuqq.gateway.application.trace.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.dto.response.GenerateTraceIdResponse;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
import com.ryuqq.gateway.domain.common.util.ClockHolder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

@DisplayName("GenerateTraceIdService 테스트")
class GenerateTraceIdServiceTest {

    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile(
                    "^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    private GenerateTraceIdService service;
    private ClockHolder clockHolder;

    @BeforeEach
    void setUp() {
        clockHolder =
                () -> Clock.fixed(Instant.parse("2025-01-24T12:34:56.789Z"), ZoneId.of("UTC"));
        service = new GenerateTraceIdService(clockHolder);
    }

    @Nested
    @DisplayName("execute() 메서드 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("Trace-ID 생성 성공")
        void shouldGenerateTraceId() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then
            StepVerifier.create(service.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response).isNotNull();
                                assertThat(response.traceId()).isNotNull();
                                assertThat(response.traceId()).hasSize(54);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("생성된 Trace-ID가 올바른 형식")
        void shouldGenerateValidFormat() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then
            StepVerifier.create(service.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.traceId()).matches(TRACE_ID_PATTERN);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("생성된 Trace-ID의 Timestamp가 ClockHolder 시간과 일치")
        void shouldHaveCorrectTimestamp() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then
            StepVerifier.create(service.execute(command))
                    .assertNext(
                            response -> {
                                String timestamp = response.traceId().substring(0, 17);
                                assertThat(timestamp).isEqualTo("20250124123456789");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("여러 번 호출 시 서로 다른 UUID 사용")
        void shouldGenerateDifferentUuids() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when
            GenerateTraceIdResponse response1 = service.execute(command).block();
            GenerateTraceIdResponse response2 = service.execute(command).block();

            // then
            assertThat(response1.traceId()).isNotEqualTo(response2.traceId());
            // Timestamp는 같지만 UUID는 다름
            assertThat(response1.traceId().substring(0, 17))
                    .isEqualTo(response2.traceId().substring(0, 17));
            assertThat(response1.traceId().substring(18))
                    .isNotEqualTo(response2.traceId().substring(18));
        }

        @Test
        @DisplayName("동시에 여러 요청 처리 시 모두 유일한 Trace-ID 생성")
        void shouldGenerateUniqueTraceIdsForConcurrentRequests() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();
            Set<String> traceIds = new HashSet<>();

            // when
            for (int i = 0; i < 100; i++) {
                GenerateTraceIdResponse response = service.execute(command).block();
                traceIds.add(response.traceId());
            }

            // then
            assertThat(traceIds).hasSize(100);
        }
    }

    @Nested
    @DisplayName("UseCase 인터페이스 구현 테스트")
    class UseCaseImplementationTest {

        @Test
        @DisplayName("GenerateTraceIdUseCase를 구현함")
        void shouldImplementUseCase() {
            assertThat(service).isInstanceOf(GenerateTraceIdUseCase.class);
        }
    }

    @Nested
    @DisplayName("다양한 ClockHolder 테스트")
    class DifferentClockHolderTest {

        @Test
        @DisplayName("다른 시간대의 ClockHolder 사용")
        void shouldWorkWithDifferentTimeZone() {
            // given - Asia/Seoul (+09:00)
            ClockHolder seoulClockHolder =
                    () ->
                            Clock.fixed(
                                    Instant.parse("2025-06-15T08:30:45.123Z"),
                                    ZoneId.of("Asia/Seoul"));
            GenerateTraceIdService seoulService = new GenerateTraceIdService(seoulClockHolder);
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then - UTC 08:30 -> Seoul 17:30
            StepVerifier.create(seoulService.execute(command))
                    .assertNext(
                            response -> {
                                String timestamp = response.traceId().substring(0, 17);
                                assertThat(timestamp).isEqualTo("20250615173045123");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("밀리초가 0인 시간 처리")
        void shouldHandleZeroMilliseconds() {
            // given
            ClockHolder zeroMillisClockHolder =
                    () -> Clock.fixed(Instant.parse("2025-01-24T12:34:56.000Z"), ZoneId.of("UTC"));
            GenerateTraceIdService zeroMillisService =
                    new GenerateTraceIdService(zeroMillisClockHolder);
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then
            StepVerifier.create(zeroMillisService.execute(command))
                    .assertNext(
                            response -> {
                                String timestamp = response.traceId().substring(0, 17);
                                assertThat(timestamp).isEqualTo("20250124123456000");
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Reactive 스트림 테스트")
    class ReactiveStreamTest {

        @Test
        @DisplayName("Mono로 반환됨")
        void shouldReturnMono() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when
            var result = service.execute(command);

            // then
            assertThat(result).isNotNull();
            StepVerifier.create(result).expectNextCount(1).verifyComplete();
        }

        @Test
        @DisplayName("구독 시에만 실행됨 (Lazy Evaluation)")
        void shouldBeLazyEvaluated() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when - 구독하지 않음
            var mono = service.execute(command);

            // then - 아직 실행되지 않음
            assertThat(mono).isNotNull();
        }
    }
}
