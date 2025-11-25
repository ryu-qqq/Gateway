package com.ryuqq.gateway.application.authentication.port.out.command;

import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Public Key Command Port
 *
 * <p>Public Key Cache 저장을 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>PublicKeyCommandAdapter (Redis Cache)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Public Key를 Redis에 저장
 *   <li>기존 Cache 삭제 후 새로운 데이터 저장
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PublicKeyCommandPort {

    /**
     * Public Key 목록을 Cache에 저장
     *
     * <p>기존 Cache를 모두 삭제하고 새로운 Public Key 목록을 저장합니다.
     *
     * @param publicKeys Public Key 목록
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> saveAll(List<PublicKey> publicKeys);
}
