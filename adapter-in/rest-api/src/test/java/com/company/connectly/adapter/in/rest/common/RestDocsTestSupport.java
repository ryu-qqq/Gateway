package com.company.connectly.adapter.in.rest.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

/**
 * Spring REST Docs 테스트 지원 추상 클래스
 *
 * <p>모든 REST Docs 테스트는 이 클래스를 상속받아 작성합니다.</p>
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>RestDocumentationExtension 자동 적용</li>
 *   <li>MockMvc 자동 설정 (Pretty Print)</li>
 *   <li>ObjectMapper 자동 주입</li>
 * </ul>
 *
 * <h2>사용 예시:</h2>
 * <pre>{@code
 * @WebMvcTest(OrderCommandController.class)
 * @DisplayName("OrderCommandController REST Docs")
 * class OrderCommandControllerDocsTest extends RestDocsTestSupport {
 *
 *     @MockBean
 *     private CreateOrderUseCase createOrderUseCase;
 *
 *     @Test
 *     @DisplayName("POST /api/v1/orders - 주문 생성 API 문서")
 *     void createOrder() throws Exception {
 *         mockMvc.perform(post("/api/v1/orders")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(objectMapper.writeValueAsString(request)))
 *             .andExpect(status().isCreated())
 *             .andDo(document("order-create",
 *                 requestFields(...),
 *                 responseFields(...)
 *             ));
 *     }
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 * @see org.springframework.restdocs.RestDocumentationExtension
 * @see org.springframework.test.web.servlet.MockMvc
 */
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsTestSupport {

    /**
     * MockMvc - HTTP 요청 시뮬레이션
     *
     * <p>하위 클래스에서 HTTP 요청 테스트 시 사용합니다.</p>
     */
    protected MockMvc mockMvc;

    /**
     * ObjectMapper - JSON 변환
     *
     * <p>Request/Response JSON 생성 시 사용합니다.</p>
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * MockMvc 초기화 및 REST Docs 설정
     *
     * <p>각 테스트 실행 전 자동으로 호출되어 MockMvc를 설정합니다.</p>
     *
     * <h3>설정 내용:</h3>
     * <ul>
     *   <li>REST Docs Configuration 적용</li>
     *   <li>Request/Response Pretty Print 활성화</li>
     *   <li>웹 애플리케이션 컨텍스트 연결</li>
     * </ul>
     *
     * @param webApplicationContext 웹 애플리케이션 컨텍스트
     * @param restDocumentation REST Docs 컨텍스트 제공자
     */
    @BeforeEach
    void setUp(
        WebApplicationContext webApplicationContext,
        RestDocumentationContextProvider restDocumentation
    ) {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(documentationConfiguration(restDocumentation)
                .operationPreprocessors()
                .withRequestDefaults(prettyPrint())
                .withResponseDefaults(prettyPrint()))
            .build();
    }
}
