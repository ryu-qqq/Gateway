package com.ryuqq.gateway.adapter.in.gateway.error;

import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.domain.ratelimit.exception.AccountLockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Rate Limit 에러 핸들러
 *
 * <p>Rate Limit 관련 예외를 처리하고 적절한 HTTP 응답을 반환합니다.
 *
 * <p><strong>처리 예외</strong>:
 *
 * <ul>
 *   <li>RateLimitExceededException → 429 Too Many Requests
 *   <li>IpBlockedException → 403 Forbidden
 *   <li>AccountLockedException → 403 Forbidden
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestControllerAdvice
public class RateLimitErrorHandler {

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    /**
     * RateLimitExceededException 처리
     *
     * @param ex RateLimitExceededException
     * @return 429 Too Many Requests
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(
            RateLimitExceededException ex) {
        ErrorInfo error = new ErrorInfo("RATE_LIMIT_EXCEEDED", "요청 빈도가 너무 높습니다. 잠시 후 다시 시도해주세요.");
        ApiResponse<Void> response = ApiResponse.ofFailure(error);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(X_RATE_LIMIT_LIMIT_HEADER, String.valueOf(ex.getLimit()))
                .header(X_RATE_LIMIT_REMAINING_HEADER, String.valueOf(ex.getRemaining()))
                .header(RETRY_AFTER_HEADER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    /**
     * IpBlockedException 처리
     *
     * @param ex IpBlockedException
     * @return 403 Forbidden
     */
    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleIpBlocked(IpBlockedException ex) {
        ErrorInfo error = new ErrorInfo("IP_BLOCKED", "비정상적인 요청 패턴이 감지되어 일시적으로 차단되었습니다.");
        ApiResponse<Void> response = ApiResponse.ofFailure(error);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(RETRY_AFTER_HEADER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    /**
     * AccountLockedException 처리
     *
     * @param ex AccountLockedException
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException ex) {
        ErrorInfo error = new ErrorInfo("ACCOUNT_LOCKED", "계정이 일시적으로 잠금되었습니다. 잠시 후 다시 시도해주세요.");
        ApiResponse<Void> response = ApiResponse.ofFailure(error);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(RETRY_AFTER_HEADER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }
}
