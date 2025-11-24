package com.company.connectly.adapter.in.rest.common.dto;


import com.company.connectly.application.common.dto.response.PageResponse;

import java.util.List;
import java.util.function.Function;

/**
 * PageApiResponse - 페이지 조회 REST API 응답 DTO (Offset 기반)
 *
 * <p>REST API Layer 전용 응답 DTO로, Application Layer의 PageResponse를 변환하여 사용합니다.</p>
 *
 * <p><strong>Offset 기반 페이지네이션:</strong></p>
 * <ul>
 *   <li>전통적인 페이지 번호 기반 페이징</li>
 *   <li>전체 개수와 페이지 정보 제공</li>
 *   <li>관리자 페이지에 적합</li>
 * </ul>
 *
 * <p><strong>응답 형식:</strong></p>
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
 * @param size 페이지 크기
 * @param totalElements 전체 데이터 개수
 * @param totalPages 전체 페이지 수
 * @param first 첫 페이지 여부
 * @param last 마지막 페이지 여부
 * @author windsurf
 * @since 1.0.0
 */
public record PageApiResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

    /**
     * Compact Constructor - Defensive Copy
     */
    public PageApiResponse {
        content = List.copyOf(content);  // Immutability 보장
    }

    /**
     * Application Layer PageResponse로부터 REST API PageApiResponse 생성
     *
     * <p>Application Layer의 PageResponse를 Adapter-In Layer의 PageApiResponse로 변환합니다.</p>
     * <p>콘텐츠는 그대로 전달되며, 각 컨트롤러에서 적절한 ApiResponse DTO로 변환해야 합니다.</p>
     *
     * @param <T> 콘텐츠 타입
     * @param appPageResponse Application Layer의 PageResponse
     * @return REST API Layer의 PageApiResponse
     * @author windsurf
     * @since 1.0.0
     */
    public static <T> PageApiResponse<T> from(
            PageResponse<T> appPageResponse) {

        return new PageApiResponse<>(
            appPageResponse.content(),
            appPageResponse.page(),
            appPageResponse.size(),
            appPageResponse.totalElements(),
            appPageResponse.totalPages(),
            appPageResponse.first(),
            appPageResponse.last()
        );
    }

    /**
     * Application Layer PageResponse로부터 REST API PageApiResponse 생성 (매퍼 함수 적용)
     *
     * <p>Application Layer의 PageResponse를 Adapter-In Layer의 PageApiResponse로 변환하면서,</p>
     * <p>각 콘텐츠 항목을 매퍼 함수를 통해 ApiResponse DTO로 변환합니다.</p>
     *
     * @param <S> Application Layer 콘텐츠 타입
     * @param <T> REST API Layer 콘텐츠 타입
     * @param appPageResponse Application Layer의 PageResponse
     * @param mapper 콘텐츠 변환 함수 (Application Response → API Response)
     * @return REST API Layer의 PageApiResponse
     * @author windsurf
     * @since 1.0.0
     */
    public static <S, T> PageApiResponse<T> from(
            PageResponse<S> appPageResponse,
            Function<S, T> mapper) {

        List<T> content = appPageResponse.content()
            .stream()
            .map(mapper)
            .toList();

        return new PageApiResponse<>(
            content,
            appPageResponse.page(),
            appPageResponse.size(),
            appPageResponse.totalElements(),
            appPageResponse.totalPages(),
            appPageResponse.first(),
            appPageResponse.last()
        );
    }
}
