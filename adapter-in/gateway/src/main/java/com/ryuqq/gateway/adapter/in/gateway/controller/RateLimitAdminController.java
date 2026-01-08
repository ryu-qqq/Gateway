package com.ryuqq.gateway.adapter.in.gateway.controller;

import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.application.ratelimit.dto.response.BlockedIpResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.command.UnblockIpUseCase;
import com.ryuqq.gateway.application.ratelimit.port.in.query.GetBlockedIpsUseCase;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 어드민 Controller
 *
 * <p>차단된 IP 목록 조회 및 관리를 위한 어드민 엔드포인트
 *
 * <p><strong>엔드포인트</strong>:
 *
 * <ul>
 *   <li>GET /actuator/rate-limit/blocked-ips - 차단된 IP 목록 조회
 *   <li>DELETE /actuator/rate-limit/blocked-ips/{ip} - IP 차단 해제
 * </ul>
 *
 * <p><strong>보안</strong>: 이 엔드포인트는 네트워크 레벨(ALB/WAF/Security Group)에서 내부망 또는 관리자 IP만 접근 가능하도록 제한해야
 * 합니다. Spring Security가 활성화된 경우 @PreAuthorize("hasRole('ADMIN')") 적용을 권장합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/actuator/rate-limit")
@Validated
public class RateLimitAdminController {

    private final GetBlockedIpsUseCase getBlockedIpsUseCase;
    private final UnblockIpUseCase unblockIpUseCase;

    public RateLimitAdminController(
            GetBlockedIpsUseCase getBlockedIpsUseCase, UnblockIpUseCase unblockIpUseCase) {
        this.getBlockedIpsUseCase = getBlockedIpsUseCase;
        this.unblockIpUseCase = unblockIpUseCase;
    }

    /**
     * 차단된 IP 목록 조회
     *
     * <p>현재 Redis에 저장된 모든 차단된 IP와 남은 차단 시간을 반환합니다.
     *
     * @return Mono&lt;ResponseEntity&lt;ApiResponse&lt;List&lt;BlockedIpResponse&gt;&gt;&gt;&gt;
     */
    @GetMapping("/blocked-ips")
    public Mono<ResponseEntity<ApiResponse<List<BlockedIpResponse>>>> getBlockedIps() {
        return getBlockedIpsUseCase
                .execute()
                .collectList()
                .map(blockedIps -> ResponseEntity.ok(ApiResponse.ofSuccess(blockedIps)));
    }

    /**
     * IP 차단 해제
     *
     * <p>특정 IP의 차단을 해제합니다.
     *
     * @param ip 차단 해제할 IP 주소 (IPv4 형식)
     * @return Mono&lt;ResponseEntity&lt;ApiResponse&lt;String&gt;&gt;&gt;
     */
    @DeleteMapping("/blocked-ips/{ip}")
    public Mono<ResponseEntity<ApiResponse<String>>> unblockIp(
            @PathVariable
                    @Pattern(
                            regexp =
                                    "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
                            message = "유효한 IPv4 주소 형식이 아닙니다")
                    String ip) {
        return unblockIpUseCase
                .execute(ip)
                .map(
                        success -> {
                            if (success) {
                                return ResponseEntity.ok(
                                        ApiResponse.ofSuccess("IP " + ip + " 차단이 해제되었습니다."));
                            } else {
                                return ResponseEntity.noContent().build();
                            }
                        });
    }
}
