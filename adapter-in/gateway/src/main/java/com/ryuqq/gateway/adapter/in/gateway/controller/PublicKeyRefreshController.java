package com.ryuqq.gateway.adapter.in.gateway.controller;

import com.ryuqq.gateway.application.authentication.port.in.command.RefreshPublicKeysUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Public Key Refresh Controller
 *
 * <p>Public Key Cache를 갱신하는 Actuator 엔드포인트
 *
 * <p><strong>엔드포인트</strong>:
 *
 * <ul>
 *   <li>POST /actuator/refresh-public-keys - Public Key Cache 전체 갱신
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/actuator")
public class PublicKeyRefreshController {

    private final RefreshPublicKeysUseCase refreshPublicKeysUseCase;

    public PublicKeyRefreshController(RefreshPublicKeysUseCase refreshPublicKeysUseCase) {
        this.refreshPublicKeysUseCase = refreshPublicKeysUseCase;
    }

    /**
     * Public Key Cache 전체 갱신
     *
     * @return Mono&lt;ResponseEntity&lt;Void&gt;&gt; 성공 시 200 OK
     */
    @PostMapping("/refresh-public-keys")
    public Mono<ResponseEntity<Void>> refreshPublicKeys() {
        return refreshPublicKeysUseCase.execute().then(Mono.just(ResponseEntity.ok().build()));
    }
}
