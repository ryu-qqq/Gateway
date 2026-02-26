package com.ryuqq.gateway.domain.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * DomainException - Domain Layer 예외의 최상위 클래스
 *
 * <p>모든 비즈니스 예외는 이 클래스를 상속해야 합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>Spring 의존성 금지 (HttpStatus 대신 int 사용)
 *   <li>ErrorCode 객체 기반 (에러 코드, HTTP 상태, 메시지 캡슐화)
 *   <li>RuntimeException 상속 (Unchecked Exception)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * public class OrderNotFoundException extends DomainException {
 *     public OrderNotFoundException(Long orderId) {
 *         super(
 *             OrderErrorCode.ORDER_NOT_FOUND,
 *             String.format("Order not found: %d", orderId),
 *             Map.of("orderId", orderId)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @author ryu-qqq
 * @since 2025-10-31
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> args;

    /**
     * Constructor - ErrorCode 기반 예외 생성
     *
     * @param errorCode 에러 코드 (필수)
     */
    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = Collections.emptyMap();
    }

    /**
     * Constructor - ErrorCode + 상세 정보
     *
     * <p>기본 메시지에 상세 정보를 추가합니다: "{기본 메시지}: {상세 정보}"
     *
     * <p>detail이 null인 경우 기본 메시지만 사용합니다.
     *
     * @param errorCode 에러 코드 (필수)
     * @param detail 상세 정보 (기본 메시지에 추가됨, null 허용)
     */
    protected DomainException(ErrorCode errorCode, String detail) {
        super(detail != null ? errorCode.getMessage() + ": " + detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = Collections.emptyMap();
    }

    /**
     * Constructor - ErrorCode + 상세 정보 + 컨텍스트 정보
     *
     * <p>기본 메시지에 상세 정보를 추가합니다: "{기본 메시지}: {상세 정보}"
     *
     * <p>detail이 null인 경우 기본 메시지만 사용합니다.
     *
     * @param errorCode 에러 코드 (필수)
     * @param detail 상세 정보 (기본 메시지에 추가됨, null 허용)
     * @param args 디버깅용 컨텍스트 정보
     */
    protected DomainException(ErrorCode errorCode, String detail, Map<String, Object> args) {
        super(detail != null ? errorCode.getMessage() + ": " + detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = args != null ? Map.copyOf(args) : Collections.emptyMap();
    }

    /**
     * Constructor - ErrorCode + 원인 예외
     *
     * @param errorCode 에러 코드 (필수)
     * @param cause 원인 예외
     */
    protected DomainException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = Collections.emptyMap();
    }

    /**
     * 에러 코드 객체 반환
     *
     * @return ErrorCode 객체
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 문자열 반환 (편의 메서드)
     *
     * @return 에러 코드 문자열 (예: "ORDER-001")
     */
    public String code() {
        return errorCode.getCode();
    }

    /**
     * 에러 코드 문자열 반환 (ErrorCode 인터페이스 호환)
     *
     * @return 에러 코드 문자열 (예: "ORDER-001")
     */
    public String getCode() {
        return errorCode.getCode();
    }

    /**
     * HTTP 상태 코드 반환 (편의 메서드)
     *
     * @return HTTP 상태 코드 (예: 404, 400, 409)
     */
    public int httpStatus() {
        return errorCode.getHttpStatus();
    }

    /**
     * 컨텍스트 정보 반환
     *
     * @return 디버깅용 컨텍스트 정보 (불변 Map)
     */
    public Map<String, Object> args() {
        return args;
    }
}
