package com.ryuqq.gateway.adapter.in.gateway.restdocs;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

/**
 * RestDocs 공통 설정
 *
 * <p>Gateway 내부 API 문서화를 위한 RestDocs 설정
 *
 * @author development-team
 * @since 1.0.0
 */
@TestConfiguration
public class RestDocsConfiguration {

    /**
     * Request Preprocessor
     *
     * <p>요청 포맷팅 및 민감 헤더 제거
     */
    @Bean
    public OperationRequestPreprocessor operationRequestPreprocessor() {
        return Preprocessors.preprocessRequest(
                modifyHeaders().remove("Host").remove("Content-Length"),
                prettyPrint());
    }

    /**
     * Response Preprocessor
     *
     * <p>응답 포맷팅
     */
    @Bean
    public OperationResponsePreprocessor operationResponsePreprocessor() {
        return Preprocessors.preprocessResponse(prettyPrint());
    }
}
