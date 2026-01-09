package com.ryuqq.gateway.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SentryConfig Unit Test
 *
 * <p>Sentry 노이즈 필터링 로직 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("SentryConfig 단위 테스트")
class SentryConfigTest {

    private SentryConfig sentryConfig;
    private SentryOptions.BeforeSendCallback callback;

    @BeforeEach
    void setUp() {
        sentryConfig = new SentryConfig();
        callback = sentryConfig.beforeSendCallback();
    }

    @Nested
    @DisplayName("Actuator 경로 필터링")
    class ActuatorPathFilteringTest {

        @Test
        @DisplayName("/actuator/health 경로는 필터링된다")
        void shouldFilterActuatorHealthPath() {
            // given
            SentryEvent event = createEventWithTransaction("/actuator/health");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("/actuator/prometheus 경로는 필터링된다")
        void shouldFilterActuatorPrometheusPath() {
            // given
            SentryEvent event = createEventWithTransaction("/actuator/prometheus");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("/actuator/refresh-public-keys 경로는 필터링된다")
        void shouldFilterActuatorRefreshPublicKeysPath() {
            // given
            SentryEvent event = createEventWithTransaction("/actuator/refresh-public-keys");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("/api/v1/actuator/health 경로는 필터링되지 않는다 (오탐 방지)")
        void shouldNotFilterNonActuatorPath() {
            // given
            SentryEvent event = createEventWithTransaction("/api/v1/actuator/health");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("transaction이 null인 경우 필터링되지 않는다")
        void shouldNotFilterWhenTransactionIsNull() {
            // given
            SentryEvent event = mock(SentryEvent.class);
            when(event.getTransaction()).thenReturn(null);

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("예외 타입 필터링")
    class ExceptionTypeFilteringTest {

        @Test
        @DisplayName("UnsupportedOperationException은 필터링된다")
        void shouldFilterUnsupportedOperationException() {
            // given
            SentryEvent event =
                    createEventWithException(
                            "java.lang.UnsupportedOperationException", "Test message");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("RejectedExecutionException은 필터링된다")
        void shouldFilterRejectedExecutionException() {
            // given
            SentryEvent event =
                    createEventWithException(
                            "io.netty.util.concurrent.RejectedExecutionException", "Test message");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("다른 예외 타입은 필터링되지 않는다")
        void shouldNotFilterOtherExceptionTypes() {
            // given
            SentryEvent event =
                    createEventWithException("java.lang.RuntimeException", "Test message");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("예외가 없는 경우 필터링되지 않는다")
        void shouldNotFilterWhenNoExceptions() {
            // given
            SentryEvent event = mock(SentryEvent.class);
            when(event.getExceptions()).thenReturn(null);

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("HTTP 404 응답 필터링")
    class Http404ResponseFilteringTest {

        @Test
        @DisplayName("HTTP 404 응답은 필터링된다")
        void shouldFilter404Response() {
            // given
            SentryEvent event = createEventWithStatusCode("404");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("HTTP 500 응답은 필터링되지 않는다")
        void shouldNotFilter500Response() {
            // given
            SentryEvent event = createEventWithStatusCode("500");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("http.status_code 태그가 없는 경우 필터링되지 않는다")
        void shouldNotFilterWhenStatusCodeTagIsMissing() {
            // given
            SentryEvent event = mock(SentryEvent.class);
            when(event.getTag("http.status_code")).thenReturn(null);

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("ReadOnlyHttpHeaders 에러 필터링")
    class ReadOnlyHttpHeadersErrorFilteringTest {

        @Test
        @DisplayName("메시지에 ReadOnlyHttpHeaders가 포함된 경우 필터링된다")
        void shouldFilterReadOnlyHttpHeadersInMessage() {
            // given
            SentryEvent event =
                    createEventWithMessage("Error: ReadOnlyHttpHeaders cannot be modified");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예외 값에 ReadOnlyHttpHeaders가 포함된 경우 필터링된다")
        void shouldFilterReadOnlyHttpHeadersInExceptionValue() {
            // given
            SentryEvent event =
                    createEventWithException(
                            "java.lang.Exception", "ReadOnlyHttpHeaders error occurred");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ReadOnlyHttpHeaders가 없는 경우 필터링되지 않는다")
        void shouldNotFilterWhenReadOnlyHttpHeadersNotPresent() {
            // given
            SentryEvent event = createEventWithMessage("Normal error message");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("ServerHttpResponse already committed 에러 필터링")
    class ServerHttpResponseAlreadyCommittedFilteringTest {

        @Test
        @DisplayName("메시지에 ServerHttpResponse already committed가 포함된 경우 필터링된다")
        void shouldFilterServerHttpResponseAlreadyCommittedInMessage() {
            // given
            SentryEvent event = createEventWithMessage("ServerHttpResponse already committed");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예외 값에 ServerHttpResponse already committed가 포함된 경우 필터링된다")
        void shouldFilterServerHttpResponseAlreadyCommittedInExceptionValue() {
            // given
            SentryEvent event =
                    createEventWithException(
                            "java.lang.IllegalStateException",
                            "ServerHttpResponse already committed");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ServerHttpResponse already committed가 없는 경우 필터링되지 않는다")
        void shouldNotFilterWhenServerHttpResponseAlreadyCommittedNotPresent() {
            // given
            SentryEvent event = createEventWithMessage("Normal error message");

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("정상 이벤트 통과")
    class NormalEventPassThroughTest {

        @Test
        @DisplayName("필터링 조건에 해당하지 않는 정상 이벤트는 통과한다")
        void shouldPassThroughNormalEvent() {
            // given
            SentryEvent event = mock(SentryEvent.class);
            when(event.getTransaction()).thenReturn("/api/v1/users");
            when(event.getExceptions()).thenReturn(new ArrayList<>());
            when(event.getTag("http.status_code")).thenReturn("200");
            Message message = mock(Message.class);
            when(message.getMessage()).thenReturn("Normal error");
            when(event.getMessage()).thenReturn(message);

            // when
            SentryEvent result = callback.execute(event, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(event);
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private SentryEvent createEventWithTransaction(String transaction) {
        SentryEvent event = mock(SentryEvent.class);
        when(event.getTransaction()).thenReturn(transaction);
        when(event.getExceptions()).thenReturn(new ArrayList<>());
        when(event.getTag("http.status_code")).thenReturn(null);
        when(event.getMessage()).thenReturn(null);
        return event;
    }

    private SentryEvent createEventWithException(String exceptionType, String exceptionValue) {
        SentryEvent event = mock(SentryEvent.class);
        when(event.getTransaction()).thenReturn("/api/v1/test");
        when(event.getTag("http.status_code")).thenReturn(null);
        when(event.getMessage()).thenReturn(null);

        SentryException sentryException = mock(SentryException.class);
        when(sentryException.getType()).thenReturn(exceptionType);
        when(sentryException.getValue()).thenReturn(exceptionValue);

        List<SentryException> exceptions = new ArrayList<>();
        exceptions.add(sentryException);
        when(event.getExceptions()).thenReturn(exceptions);

        return event;
    }

    private SentryEvent createEventWithStatusCode(String statusCode) {
        SentryEvent event = mock(SentryEvent.class);
        when(event.getTransaction()).thenReturn("/api/v1/test");
        when(event.getTag("http.status_code")).thenReturn(statusCode);
        when(event.getExceptions()).thenReturn(new ArrayList<>());
        when(event.getMessage()).thenReturn(null);
        return event;
    }

    private SentryEvent createEventWithMessage(String messageText) {
        SentryEvent event = mock(SentryEvent.class);
        when(event.getTransaction()).thenReturn("/api/v1/test");
        when(event.getTag("http.status_code")).thenReturn(null);
        when(event.getExceptions()).thenReturn(new ArrayList<>());

        Message message = mock(Message.class);
        when(message.getMessage()).thenReturn(messageText);
        when(event.getMessage()).thenReturn(message);

        return event;
    }
}
