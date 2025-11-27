package com.ryuqq.gateway.domain.authentication.vo;

import java.util.Objects;

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
 * @author development-team
 * @since 1.0.0
 */
public final class ExpiredTokenInfo {

    private final boolean expired;
    private final Long userId;
    private final String tenantId;

    private ExpiredTokenInfo(boolean expired, Long userId, String tenantId) {
        this.expired = expired;
        this.userId = userId;
        this.tenantId = tenantId;
    }

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

    /**
     * 사용자 ID 조회
     *
     * @return 사용자 ID
     */
    public Long userId() {
        return userId;
    }

    /**
     * 테넌트 ID 조회
     *
     * @return 테넌트 ID
     */
    public String tenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpiredTokenInfo that = (ExpiredTokenInfo) o;
        return expired == that.expired
                && Objects.equals(userId, that.userId)
                && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expired, userId, tenantId);
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
