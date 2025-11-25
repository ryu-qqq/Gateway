package com.ryuqq.gateway.application.authentication.dto.response;

import com.ryuqq.gateway.domain.authentication.vo.PublicKey;

/**
 * Public Key 조회 Response DTO
 *
 * <p>Public Key 조회 결과를 담는 불변 Response 객체
 *
 * @param publicKey Public Key VO
 */
public record GetPublicKeyResponse(PublicKey publicKey) { }
