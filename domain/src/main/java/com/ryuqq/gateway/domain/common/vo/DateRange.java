package com.ryuqq.gateway.domain.common.vo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * DateRange - 날짜 범위 Value Object
 *
 * <p>시작일과 종료일을 포함하는 날짜 범위를 표현합니다.
 *
 * <p><strong>사용 시나리오:</strong>
 *
 * <ul>
 *   <li>어드민 주문 조회: 주문일 범위 필터
 *   <li>회원 가입일 기준 조회
 *   <li>정산 기간 조회
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 특정 기간
 * DateRange range = DateRange.of(startDate, endDate);
 *
 * // 최근 7일
 * DateRange lastWeek = DateRange.lastDays(7);
 *
 * // 이번 달
 * DateRange thisMonth = DateRange.thisMonth();
 *
 * // Criteria에서 사용
 * OrderCriteria criteria = new OrderCriteria(
 *     memberId,
 *     DateRange.lastDays(30),
 *     ...
 * );
 * }</pre>
 *
 * @param startDate 시작일 (포함, nullable - null이면 제한 없음)
 * @param endDate 종료일 (포함, nullable - null이면 제한 없음)
 * @author development-team
 * @since 1.0.0
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

    /** Compact Constructor - 유효성 검증 */
    public DateRange {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    String.format("시작일(%s)은 종료일(%s)보다 이전이어야 합니다", startDate, endDate));
        }
    }

    /**
     * 날짜 범위 생성
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return DateRange
     */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    /**
     * 최근 N일 범위 생성
     *
     * @param days 일수
     * @return DateRange (오늘 기준 N일 전 ~ 오늘)
     */
    public static DateRange lastDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days는 0 이상이어야 합니다");
        }
        LocalDate today = LocalDate.now();
        return new DateRange(today.minusDays(days), today);
    }

    /**
     * 이번 달 범위 생성
     *
     * @return DateRange (이번 달 1일 ~ 말일)
     */
    public static DateRange thisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        return new DateRange(firstDay, lastDay);
    }

    /**
     * 지난 달 범위 생성
     *
     * @return DateRange (지난 달 1일 ~ 말일)
     */
    public static DateRange lastMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = today.withDayOfMonth(1).minusDays(1);
        return new DateRange(firstDayLastMonth, lastDayLastMonth);
    }

    /**
     * 시작일 없음 (종료일까지)
     *
     * @param endDate 종료일
     * @return DateRange
     */
    public static DateRange until(LocalDate endDate) {
        return new DateRange(null, endDate);
    }

    /**
     * 종료일 없음 (시작일부터)
     *
     * @param startDate 시작일
     * @return DateRange
     */
    public static DateRange from(LocalDate startDate) {
        return new DateRange(startDate, null);
    }

    /**
     * 시작일을 Instant로 변환 (00:00:00, 시스템 기본 ZoneId 사용)
     *
     * <p><strong>주의</strong>: 시스템 기본 ZoneId를 사용하여 변환합니다.
     *
     * <p>Domain Layer에서는 Instant 사용이 필수이므로, 날짜 범위를 Instant로 변환할 때 사용합니다.
     *
     * @return 시작 일시 (null이면 null 반환)
     */
    public Instant startInstant() {
        return startDate != null
                ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;
    }

    /**
     * 종료일을 Instant로 변환 (23:59:59.999999999, 시스템 기본 ZoneId 사용)
     *
     * <p><strong>주의</strong>: 시스템 기본 ZoneId를 사용하여 변환합니다.
     *
     * <p>Domain Layer에서는 Instant 사용이 필수이므로, 날짜 범위를 Instant로 변환할 때 사용합니다.
     *
     * @return 종료 일시 (null이면 null 반환)
     */
    public Instant endInstant() {
        if (endDate == null) {
            return null;
        }
        // LocalDateTime을 거치지 않고 직접 Instant로 변환
        // endDate의 다음날 00:00:00에서 1나노초 빼서 23:59:59.999999999 표현
        return endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1);
    }

    /**
     * 범위가 비어있는지 확인 (시작일, 종료일 모두 null)
     *
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return startDate == null && endDate == null;
    }

    /**
     * 특정 날짜가 범위 내에 있는지 확인
     *
     * @param date 확인할 날짜
     * @return 범위 내에 있으면 true
     */
    public boolean contains(LocalDate date) {
        if (date == null) {
            return false;
        }
        boolean afterStart = startDate == null || !date.isBefore(startDate);
        boolean beforeEnd = endDate == null || !date.isAfter(endDate);
        return afterStart && beforeEnd;
    }
}
