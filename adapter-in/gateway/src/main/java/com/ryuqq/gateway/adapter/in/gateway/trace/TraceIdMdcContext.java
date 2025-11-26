package com.ryuqq.gateway.adapter.in.gateway.trace;

import org.slf4j.MDC;

/**
 * TraceIdMdcContext - Trace-ID MDC 관리 유틸리티
 *
 * <p>Reactive 환경에서 로그에 Trace-ID를 자동 추가하기 위한 MDC 관리 클래스입니다.
 *
 * <p><strong>MDC (Mapped Diagnostic Context)</strong>:
 *
 * <ul>
 *   <li>로그에 컨텍스트 정보를 자동 추가하는 메커니즘
 *   <li>traceId를 MDC에 저장하면 모든 로그에 자동 포함
 * </ul>
 *
 * <p><strong>주의</strong>: Reactive 환경에서는 Reactor Context와 함께 사용해야 합니다. Spring Cloud Sleuth가 Reactor
 * Context → MDC 전파를 자동 처리합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TraceIdMdcContext {

    /** MDC에서 사용할 Trace-ID 키 */
    public static final String TRACE_ID_KEY = "traceId";

    private TraceIdMdcContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * MDC에 Trace-ID 추가
     *
     * @param traceId 저장할 Trace-ID
     */
    public static void put(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /** MDC에서 Trace-ID 제거 */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * MDC에서 Trace-ID 조회
     *
     * @return 저장된 Trace-ID 또는 null
     */
    public static String get() {
        return MDC.get(TRACE_ID_KEY);
    }
}
