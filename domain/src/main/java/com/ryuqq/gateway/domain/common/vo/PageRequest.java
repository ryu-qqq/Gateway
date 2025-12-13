package com.ryuqq.gateway.domain.common.vo;

/**
 * PageRequest - 오프셋 기반 페이징 Value Object
 *
 * <p>전통적인 페이지 번호 기반 페이징을 위한 요청 객체입니다.
 *
 * <p><strong>사용 시나리오:</strong>
 *
 * <ul>
 *   <li>페이지 번호가 있는 어드민 목록 UI
 *   <li>전체 페이지 수를 표시해야 하는 경우
 *   <li>임의의 페이지로 점프가 필요한 경우
 * </ul>
 *
 * <p><strong>PageRequest vs CursorPageRequest:</strong>
 *
 * <ul>
 *   <li>PageRequest: 페이지 번호 기반, COUNT 쿼리 필요, 대량 데이터 시 성능 저하
 *   <li>CursorPageRequest: 커서 기반, COUNT 불필요, 대량 데이터에 적합
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 기본 사용
 * PageRequest pageRequest = PageRequest.of(0, 20);
 *
 * // 첫 페이지
 * PageRequest firstPage = PageRequest.first(20);
 *
 * // Criteria에서 사용
 * OrderSearchCriteria criteria = new OrderSearchCriteria(
 *     memberId,
 *     dateRange,
 *     sortKey,
 *     sortDirection,
 *     PageRequest.of(page, size)
 * );
 * }</pre>
 *
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기 (한 페이지당 항목 수)
 * @author development-team
 * @since 1.0.0
 */
public record PageRequest(int page, int size) {

    /** 기본 페이지 크기 */
    public static final int DEFAULT_SIZE = 20;

    /** 최대 페이지 크기 */
    public static final int MAX_SIZE = 100;

    /** Compact Constructor - 유효성 검증 및 정규화 */
    public PageRequest {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }

    /**
     * PageRequest 생성
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return PageRequest
     */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    /**
     * 첫 페이지 요청 생성
     *
     * @param size 페이지 크기
     * @return PageRequest (page=0)
     */
    public static PageRequest first(int size) {
        return new PageRequest(0, size);
    }

    /**
     * 기본 설정 PageRequest 생성
     *
     * @return PageRequest (page=0, size=DEFAULT_SIZE)
     */
    public static PageRequest defaultPage() {
        return new PageRequest(0, DEFAULT_SIZE);
    }

    /**
     * 오프셋 계산
     *
     * <p>SQL OFFSET 값으로 사용됩니다.
     *
     * @return offset (page * size)
     */
    public long offset() {
        return (long) page * size;
    }

    /**
     * 다음 페이지 요청 생성
     *
     * @return 다음 페이지 PageRequest
     */
    public PageRequest next() {
        return new PageRequest(page + 1, size);
    }

    /**
     * 이전 페이지 요청 생성
     *
     * @return 이전 페이지 PageRequest (첫 페이지면 그대로)
     */
    public PageRequest previous() {
        return page > 0 ? new PageRequest(page - 1, size) : this;
    }

    /**
     * 첫 페이지인지 확인
     *
     * @return 첫 페이지면 true
     */
    public boolean isFirst() {
        return page == 0;
    }

    /**
     * 전체 페이지 수 계산
     *
     * @param totalElements 전체 항목 수
     * @return 전체 페이지 수
     */
    public int totalPages(long totalElements) {
        return (int) Math.ceil((double) totalElements / size);
    }

    /**
     * 마지막 페이지인지 확인
     *
     * @param totalElements 전체 항목 수
     * @return 마지막 페이지면 true
     */
    public boolean isLast(long totalElements) {
        return page >= totalPages(totalElements) - 1;
    }
}
