package com.ryuqq.gateway.application.trace.port.out.client;

/**
 * IdGeneratorPort - ID 생성 포트
 *
 * @author development-team
 * @since 1.0.0
 */
public interface IdGeneratorPort {

    /**
     * UUID 생성
     *
     * @return UUID 문자열
     */
    String generateUuid();
}
