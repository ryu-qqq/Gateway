package com.ryuqq.gateway.domain.common.vo;

/**
 * SortDirection - 정렬 방향 Value Object
 *
 * <p>조회 시 정렬 방향을 지정합니다.
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * OrderCriteria criteria = new OrderCriteria(
 *     memberId,
 *     dateRange,
 *     OrderSortKey.ORDER_DATE,
 *     SortDirection.DESC,  // 최신순
 *     pageRequest
 * );
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum SortDirection {

    /**
     * 오름차순 (Ascending)
     *
     * <p>작은 값 → 큰 값 순서
     *
     * <ul>
     *   <li>날짜: 오래된 순
     *   <li>숫자: 낮은 순
     *   <li>문자: A → Z
     * </ul>
     */
    ASC("오름차순"),

    /**
     * 내림차순 (Descending)
     *
     * <p>큰 값 → 작은 값 순서
     *
     * <ul>
     *   <li>날짜: 최신순
     *   <li>숫자: 높은 순
     *   <li>문자: Z → A
     * </ul>
     */
    DESC("내림차순");

    private final String displayName;

    SortDirection(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 한글 표시명 반환
     *
     * @return 표시명
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 기본 정렬 방향 (내림차순)
     *
     * @return DESC
     */
    public static SortDirection defaultDirection() {
        return DESC;
    }

    /**
     * 오름차순인지 확인
     *
     * @return ASC이면 true
     */
    public boolean isAscending() {
        return this == ASC;
    }

    /**
     * 내림차순인지 확인
     *
     * @return DESC이면 true
     */
    public boolean isDescending() {
        return this == DESC;
    }

    /**
     * 반대 방향 반환
     *
     * @return ASC ↔ DESC
     */
    public SortDirection reverse() {
        return this == ASC ? DESC : ASC;
    }

    /**
     * 문자열로부터 SortDirection 파싱 (대소문자 무관)
     *
     * @param value 문자열 ("asc", "ASC", "desc", "DESC")
     * @return SortDirection (null이거나 유효하지 않으면 DESC 반환)
     */
    public static SortDirection fromString(String value) {
        if (value == null || value.isBlank()) {
            return defaultDirection();
        }
        try {
            return SortDirection.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return defaultDirection();
        }
    }
}
