package com.company.connectly.adapter.in.rest.config;

import java.util.List;

import com.company.connectly.adapter.in.rest.common.error.ErrorMapperRegistry;
import com.company.connectly.adapter.in.rest.common.mapper.ErrorMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 에러 핸들링 설정
 *
 * <p>도메인 예외를 HTTP 응답으로 변환하는 ErrorMapper들을 관리합니다.</p>
 *
 * <p><strong>주요 기능:</strong></p>
 * <ul>
 *   <li>ErrorMapperRegistry를 Spring Bean으로 등록</li>
 *   <li>모든 ErrorMapper 구현체를 자동으로 수집하여 Registry에 등록</li>
 *   <li>도메인별로 ErrorMapper를 추가하면 자동으로 감지되어 등록됨</li>
 * </ul>
 *
 * <p><strong>사용 방법:</strong></p>
 * <pre>{@code
 * // 1. ErrorMapper 구현체 생성
 * @Component
 * public class ExampleErrorMapper implements ErrorMapper {
 *     @Override
 *     public boolean supports(String code) {
 *         return code.startsWith("EXAMPLE_");
 *     }
 *
 *     @Override
 *     public MappedError map(DomainException ex, Locale locale) {
 *         // 매핑 로직
 *     }
 * }
 *
 * // 2. 자동으로 ErrorMapperRegistry에 등록됨
 * // 3. GlobalExceptionHandler에서 자동으로 사용 가능
 * }</pre>
 *
 * @author windsurf
 * @since 1.0.0
 */
@Configuration
public class ErrorHandlingConfig {

    /**
     * ErrorMapperRegistry를 Spring Bean으로 등록
     *
     * <p>Spring에 등록된 모든 ErrorMapper 빈들을 자동으로 수집하여 Registry에 등록합니다.</p>
     *
     * <p><strong>작동 방식:</strong></p>
     * <ul>
     *   <li>Spring이 모든 ErrorMapper 구현체를 찾아서 List로 주입</li>
     *   <li>ErrorMapperRegistry가 이 List를 받아서 관리</li>
     *   <li>GlobalExceptionHandler가 ErrorMapperRegistry를 통해 적절한 Mapper 선택</li>
     * </ul>
     *
     * @param mappers Spring에 등록된 모든 ErrorMapper 빈들
     * @return ErrorMapperRegistry 빈
     */
    @Bean
    public ErrorMapperRegistry errorMapperRegistry(List<ErrorMapper> mappers) {
        return new ErrorMapperRegistry(mappers);
    }
}
