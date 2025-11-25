package com.ryuqq.gateway.application.authentication.dto.response;

import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;

/**
 * JWT 검증 Response DTO
 *
 * <p>JWT 검증 결과를 담는 불변 Response 객체
 *
 * @param jwtClaims JWT Claims (검증된 Claims 정보)
 * @param isValid 검증 성공 여부
 */
public record ValidateJwtResponse(JwtClaims jwtClaims, boolean isValid) { }
