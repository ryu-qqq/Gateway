package com.ryuqq.gateway.application.authentication.dto.query;

/**
 * Public Key 조회 Query DTO
 *
 * <p>JWT Header의 kid를 기반으로 Public Key를 조회하는 Query 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>kid는 null 또는 blank일 수 없다
 * </ul>
 *
 * @param kid JWT Header의 Key ID (null/blank 불가)
 */
public record GetPublicKeyQuery(String kid) {

    /** Compact Constructor - 검증 로직 */
    public GetPublicKeyQuery {
        if (kid == null || kid.isBlank()) {
            throw new IllegalArgumentException("Kid cannot be null or blank");
        }
    }
}
