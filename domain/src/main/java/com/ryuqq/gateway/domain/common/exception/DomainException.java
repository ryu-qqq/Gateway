package com.ryuqq.gateway.domain.common.exception;

/**
 * DomainException - Domain Layer 기본 예외 클래스
 *
 * <p>모든 Domain Layer 예외는 이 클래스를 상속해야 합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>✅ ErrorCode 객체를 통한 일관된 에러 정보 제공
 *   <li>✅ RuntimeException (Unchecked Exception) 상속
 *   <li>✅ 상세 정보(detail)를 통한 디버깅 지원
 * </ul>
 *
 * <p><strong>구현 예시:</strong>
 *
 * <pre>{@code
 * public class OrderNotFoundException extends DomainException {
 *     public OrderNotFoundException(Long orderId) {
 *         super(OrderErrorCode.ORDER_NOT_FOUND, "orderId: " + orderId);
 *     }
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 에러 코드 (ErrorCode 구현체)
     * @author development-team
     * @since 1.0.0
     */
    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * ErrorCode와 상세 정보로 예외 생성
     *
     * @param errorCode 에러 코드 (ErrorCode 구현체)
     * @param detail 상세 정보 (디버깅용)
     * @author development-team
     * @since 1.0.0
     */
    protected DomainException(ErrorCode errorCode, String detail) {
        super(detail != null ? errorCode.getMessage() + ": " + detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * ErrorCode 반환
     *
     * @return ErrorCode 객체
     * @author development-team
     * @since 1.0.0
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 문자열 반환
     *
     * @return 에러 코드 문자열 (예: AUTH-001)
     * @author development-team
     * @since 1.0.0
     */
    public String getCode() {
        return errorCode.getCode();
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드 (예: 401, 404)
     * @author development-team
     * @since 1.0.0
     */
    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    /**
     * 상세 정보 반환
     *
     * @return 상세 정보 문자열 (없으면 null)
     * @author development-team
     * @since 1.0.0
     */
    public String getDetail() {
        return detail;
    }
}
