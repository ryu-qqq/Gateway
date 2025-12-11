package com.ryuqq.gateway.domain.authentication.vo;

/**
 * ExpiredTokenInfo - 만료된 JWT에서 추출한 정보
 *
 * <p>서명은 유효하지만 만료된 JWT에서 사용자 정보를 추출한 결과입니다.
 *
 * <p><strong>용도:</strong>
 *
 * <ul>
 *   <li>Token Refresh 시 만료된 Access Token에서 userId/tenantId 추출
 *   <li>서명 검증 후 만료 여부와 관계없이 사용자 식별 가능
 * </ul>
 *
 * @param expired JWT 만료 여부
 * @param userId 사용자 ID
 * @param tenantId 테넌트 ID
 * @author development-team
 * @since 1.0.0
 */
public record ExpiredTokenInfo(boolean expired, Long userId, String tenantId) {

    /**
     * ExpiredTokenInfo 생성
     *
     * @param expired JWT 만료 여부
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @return ExpiredTokenInfo 인스턴스
     */
    public static ExpiredTokenInfo of(boolean expired, Long userId, String tenantId) {
        return new ExpiredTokenInfo(expired, userId, tenantId);
    }

    /**
     * JWT가 만료되었는지 확인
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return expired;
    }

    @Override
    public String toString() {
        return "ExpiredTokenInfo{expired="
                + expired
                + ", userId="
                + userId
                + ", tenantId='"
                + tenantId
                + "'}";
    }
}
