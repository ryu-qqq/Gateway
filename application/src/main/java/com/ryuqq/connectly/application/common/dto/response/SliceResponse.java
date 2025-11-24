package com.ryuqq.connectly.application.common.dto.response;

import java.util.List;

/**
 * SliceResponse - 커서 기반 페이징 응답 (무한 스크롤)
 *
 * <p>전체 개수를 세지 않고 다음 페이지 존재 여부만 확인하는 효율적인 페이징 방식입니다.
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
 *   <li>무한 스크롤 UI
 *   <li>대량의 데이터에서 COUNT 쿼리 부담이 큰 경우
 *   <li>실시간 피드, 타임라인 등
 * </ul>
 *
 * <p><strong>PageResponse vs SliceResponse:</strong>
 *
 * <ul>
 *   <li>PageResponse: totalElements, totalPages 제공 (COUNT 쿼리 필요)
 *   <li>SliceResponse: hasNext만 제공 (LIMIT+1로 효율적 조회)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // UseCase에서 사용
 * SliceResponse<TenantResponse> sliceResponse = SliceResponse.of(
 *     tenantList,     // content
 *     20,             // size
 *     true,           // hasNext
 *     "cursor-xyz"    // nextCursor (optional)
 * );
 *
 * // Controller에서 사용
 * return ResponseEntity.ok(ApiResponse.ofSuccess(sliceResponse));
 * }</pre>
 *
 * <p><strong>응답 형식:</strong>
 *
 * <pre>{@code
 * {
 *   "content": [...],
 *   "size": 20,
 *   "hasNext": true,
 *   "nextCursor": "cursor-xyz"
 * }
 * }</pre>
 *
 * @param <T> 콘텐츠 타입
 * @param content 현재 슬라이스의 데이터 목록
 * @param size 슬라이스 크기 (한 번에 조회한 항목 수)
 * @param hasNext 다음 슬라이스 존재 여부
 * @param nextCursor 다음 슬라이스를 조회하기 위한 커서 (optional)
 * @author ryu-qqq
 * @since 2025-10-23
 */
public record SliceResponse<T>(List<T> content, int size, boolean hasNext, String nextCursor) {

    /**
     * SliceResponse 생성 (커서 있음)
     *
     * @param content 현재 슬라이스 데이터
     * @param size 슬라이스 크기
     * @param hasNext 다음 슬라이스 존재 여부
     * @param nextCursor 다음 커서
     * @param <T> 콘텐츠 타입
     * @return SliceResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> SliceResponse<T> of(
            List<T> content, int size, boolean hasNext, String nextCursor) {
        return new SliceResponse<>(content, size, hasNext, nextCursor);
    }

    /**
     * SliceResponse 생성 (커서 없음)
     *
     * <p>커서 대신 offset 기반으로 사용할 경우
     *
     * @param content 현재 슬라이스 데이터
     * @param size 슬라이스 크기
     * @param hasNext 다음 슬라이스 존재 여부
     * @param <T> 콘텐츠 타입
     * @return SliceResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> SliceResponse<T> of(List<T> content, int size, boolean hasNext) {
        return new SliceResponse<>(content, size, hasNext, null);
    }

    /**
     * 빈 SliceResponse 생성
     *
     * @param size 슬라이스 크기
     * @param <T> 콘텐츠 타입
     * @return 빈 SliceResponse
     * @author ryu-qqq
     * @since 2025-10-23
     */
    public static <T> SliceResponse<T> empty(int size) {
        return new SliceResponse<>(List.of(), size, false, null);
    }
}
