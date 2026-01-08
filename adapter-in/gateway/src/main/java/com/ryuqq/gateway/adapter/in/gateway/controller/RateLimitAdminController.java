package com.ryuqq.gateway.adapter.in.gateway.controller;

import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.application.ratelimit.dto.response.BlockedIpResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.command.ResetRateLimitUseCase;
import com.ryuqq.gateway.application.ratelimit.port.in.query.GetBlockedIpsUseCase;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
 * <p><strong>보안</strong>: Actuator 경로로 노출되므로 Spring Security 또는 네트워크 레벨에서 접근 제어 필요
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/actuator/rate-limit")
public class RateLimitAdminController {

    private final GetBlockedIpsUseCase getBlockedIpsUseCase;
    private final IpBlockCommandPort ipBlockCommandPort;
    private final ResetRateLimitUseCase resetRateLimitUseCase;

    public RateLimitAdminController(
            GetBlockedIpsUseCase getBlockedIpsUseCase,
            IpBlockCommandPort ipBlockCommandPort,
            ResetRateLimitUseCase resetRateLimitUseCase) {
        this.getBlockedIpsUseCase = getBlockedIpsUseCase;
        this.ipBlockCommandPort = ipBlockCommandPort;
        this.resetRateLimitUseCase = resetRateLimitUseCase;
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
     * @param ip 차단 해제할 IP 주소
     * @return Mono&lt;ResponseEntity&lt;ApiResponse&lt;String&gt;&gt;&gt;
     */
    @DeleteMapping("/blocked-ips/{ip}")
    public Mono<ResponseEntity<ApiResponse<String>>> unblockIp(@PathVariable String ip) {
        return ipBlockCommandPort
                .unblock(ip)
                .map(
                        success -> {
                            if (success) {
                                return ResponseEntity.ok(
                                        ApiResponse.ofSuccess("IP " + ip + " 차단이 해제되었습니다."));
                            } else {
                                return ResponseEntity.ok(
                                        ApiResponse.ofSuccess("IP " + ip + "는 차단되어 있지 않습니다."));
                            }
                        });
    }
}
