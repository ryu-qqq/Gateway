package com.ryuqq.gateway.bootstrap.config;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry 노이즈 이벤트 필터링 설정
 *
 * <p>다음 유형의 이벤트를 필터링하여 Sentry 노이즈를 감소시킵니다:
 *
 * <ul>
 *   <li>Actuator 엔드포인트 관련 에러 (/actuator/*)
 *   <li>UnsupportedOperationException (Spring WebFlux + Sentry SDK 호환성 문제)
 *   <li>HTTP 404 응답 (정상적인 클라이언트 에러)
 *   <li>ReadOnlyHttpHeaders 관련 에러
 *   <li>RejectedExecutionException (Netty 종료 시점 이슈)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see <a href="https://github.com/ryu-qqq/Gateway/issues/24">GitHub Issue #24</a>
 */
@Configuration
public class SentryConfig {

    private static final List<String> IGNORED_PATHS =
            List.of(
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/actuator/metrics",
                    "/actuator/info");

    private static final List<String> IGNORED_EXCEPTION_TYPES =
            List.of(
                    "java.lang.UnsupportedOperationException",
                    "io.netty.util.concurrent.RejectedExecutionException");

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            if (isActuatorPath(event)) {
                return null;
            }

            if (isIgnoredException(event)) {
                return null;
            }

            if (is404Response(event)) {
                return null;
            }

            if (isReadOnlyHeadersError(event)) {
                return null;
            }

            return event;
        };
    }

    private boolean isActuatorPath(SentryEvent event) {
        String transaction = event.getTransaction();
        if (transaction == null) {
            return false;
        }
        return IGNORED_PATHS.stream().anyMatch(transaction::contains);
    }

    private boolean isIgnoredException(SentryEvent event) {
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions == null || exceptions.isEmpty()) {
            return false;
        }
        return exceptions.stream().anyMatch(ex -> IGNORED_EXCEPTION_TYPES.contains(ex.getType()));
    }

    private boolean is404Response(SentryEvent event) {
        String statusCode = event.getTag("http.status_code");
        return "404".equals(statusCode);
    }

    private boolean isReadOnlyHeadersError(SentryEvent event) {
        String message = event.getMessage() != null ? event.getMessage().getMessage() : null;
        if (message != null && message.contains("ReadOnlyHttpHeaders")) {
            return true;
        }
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            return exceptions.stream()
                    .anyMatch(
                            ex -> {
                                String value = ex.getValue();
                                return value != null && value.contains("ReadOnlyHttpHeaders");
                            });
        }
        return false;
    }
}
