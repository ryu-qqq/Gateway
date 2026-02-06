package com.ryuqq.gateway.application.authentication.dto.query;

/**
 * Public Key 조회 Query DTO
 *
 * <p>JWT Header의 kid를 기반으로 Public Key를 조회하는 Query 객체
 *
 * @param kid JWT Header의 Key ID
 */
public record GetPublicKeyQuery(String kid) {

    /**
     * 정적 팩토리 메서드
     *
     * @param kid JWT Header의 Key ID
     * @return GetPublicKeyQuery 인스턴스
     */
    public static GetPublicKeyQuery of(String kid) {
        return new GetPublicKeyQuery(kid);
    }
}
