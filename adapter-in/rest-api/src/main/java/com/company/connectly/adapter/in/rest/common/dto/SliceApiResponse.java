package com.company.connectly.adapter.in.rest.common.dto;


import com.company.connectly.application.common.dto.response.SliceResponse;

import java.util.List;
import java.util.function.Function;

/**
 * SliceApiResponse - 슬라이스 조회 REST API 응답 DTO (Cursor 기반)
 *
 * <p>REST API Layer 전용 응답 DTO로, Application Layer의 SliceResponse를 변환하여 사용합니다.</p>
 *
 * <p><strong>Cursor 기반 페이지네이션:</strong></p>
 * <ul>
 *   <li>무한 스크롤 UI에 적합</li>
 *   <li>COUNT 쿼리 불필요 (고성능)</li>
 *   <li>다음 페이지 존재 여부만 제공</li>
 *   <li>일반 사용자 페이지에 적합</li>
 * </ul>
 *
 * <p><strong>응답 형식:</strong></p>
 * <pre>{@code
 * {
 *   "content": [...],
 *   "size": 20,
 *   "hasNext": true,
 *   "nextCursor": "xyz"
 * }
 * }</pre>
 *
 * @param <T> 콘텐츠 타입
 * @param content 현재 슬라이스의 데이터 목록
 * @param size 슬라이스 크기
 * @param hasNext 다음 슬라이스 존재 여부
 * @param nextCursor 다음 슬라이스 조회를 위한 커서
 * @author windsurf
 * @since 1.0.0
 */
public record SliceApiResponse<T>(
    List<T> content,
    int size,
    boolean hasNext,
    String nextCursor
) {

    /**
     * Compact Constructor - Defensive Copy
     */
    public SliceApiResponse {
        content = List.copyOf(content);  // Immutability 보장
    }

    /**
     * Application Layer SliceResponse로부터 REST API SliceApiResponse 생성
     *
     * <p>Application Layer의 SliceResponse를 Adapter-In Layer의 SliceApiResponse로 변환합니다.</p>
     * <p>콘텐츠는 그대로 전달되며, 각 컨트롤러에서 적절한 ApiResponse DTO로 변환해야 합니다.</p>
     *
     * @param <T> 콘텐츠 타입
     * @param appSliceResponse Application Layer의 SliceResponse
     * @return REST API Layer의 SliceApiResponse
     * @author windsurf
     * @since 1.0.0
     */
    public static <T> SliceApiResponse<T> from(
            SliceResponse<T> appSliceResponse) {

        return new SliceApiResponse<>(
            appSliceResponse.content(),
            appSliceResponse.size(),
            appSliceResponse.hasNext(),
            appSliceResponse.nextCursor()
        );
    }

    /**
     * Application Layer SliceResponse로부터 REST API SliceApiResponse 생성 (매퍼 함수 적용)
     *
     * <p>Application Layer의 SliceResponse를 Adapter-In Layer의 SliceApiResponse로 변환하면서,</p>
     * <p>각 콘텐츠 항목을 매퍼 함수를 통해 ApiResponse DTO로 변환합니다.</p>
     *
     * @param <S> Application Layer 콘텐츠 타입
     * @param <T> REST API Layer 콘텐츠 타입
     * @param appSliceResponse Application Layer의 SliceResponse
     * @param mapper 콘텐츠 변환 함수 (Application Response → API Response)
     * @return REST API Layer의 SliceApiResponse
     * @author windsurf
     * @since 1.0.0
     */
    public static <S, T> SliceApiResponse<T> from(
            SliceResponse<S> appSliceResponse,
            Function<S, T> mapper) {

        List<T> content = appSliceResponse.content()
            .stream()
            .map(mapper)
            .toList();

        return new SliceApiResponse<>(
            content,
            appSliceResponse.size(),
            appSliceResponse.hasNext(),
            appSliceResponse.nextCursor()
        );
    }
}
