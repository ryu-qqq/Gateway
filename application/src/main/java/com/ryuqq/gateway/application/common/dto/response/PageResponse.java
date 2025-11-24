package com.ryuqq.gateway.application.common.dto.response;

import java.util.List;

/**
 * PageResponse - 오프셋 기반 페이징 응답
 *
 * <p>전통적인 페이지 번호 기반 페이징을 위한 응답 구조입니다.
 *
 * <p><strong>Application Layer DTO:</strong>
 *
 * <ul>
 *   <li>Application Layer와 Adapter Layer 모두에서 사용 가능
 *   <li>페이징 로직의 재사용성 향상
 *   <li>Layer 간 일관된 페이징 인터페이스 제공
 * </ul>
 *
 * <p><strong>사용 시나리오:</strong>
 *
 * <ul>
 *   <li>페이지 번호가 있는 일반적인 페이징 UI
 *   <li>전체 페이지 수를 알아야 하는 경우
 *   <li>임의의 페이지로 점프가 필요한 경우
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // UseCase에서 사용
 * PageResponse<TenantResponse> pageResponse = PageResponse.of(
 *     tenantList,     // content
 *     0,              // page
 *     20,             // size
 *     100L,           // totalElements
 *     5,              // totalPages
 *     true,           // first
 *     false           // last
 * );
 *
 * // Controller에서 사용
 * return ResponseEntity.ok(ApiResponse.ofSuccess(pageResponse));
 * }</pre>
 *
 * <p><strong>응답 형식:</strong>
 *
 * <pre>{@code
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 100,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false
 * }
 * }</pre>
 *
 * @param <T> 콘텐츠 타입
 * @param content 현재 페이지의 데이터 목록
 * @param page 현재 페이지 번호 (0부터 시작)
 * @param size 페이지 크기 (한 페이지당 항목 수)
 * @param totalElements 전체 데이터 개수
 * @param totalPages 전체 페이지 수
 * @param first 첫 페이지 여부
 * @param last 마지막 페이지 여부
 * @author ryu-qqq
 * @since 2025-10-23
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    /**
     * PageResponse 생성 (정적 팩토리 메서드)
     *
     * @param content 현재 페이지 데이터
     * @param page 현재 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 데이터 개수
     * @param totalPages 전체 페이지 수
     * @param first 첫 페이지 여부
     * @param last 마지막 페이지 여부
     * @param <T> 콘텐츠 타입
     * @return PageResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> PageResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {
        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last);
    }

    /**
     * 빈 PageResponse 생성
     *
     * @param page 현재 페이지 번호
     * @param size 페이지 크기
     * @param <T> 콘텐츠 타입
     * @return 빈 PageResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0L, 0, true, true);
    }
}
