package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.trace.TraceIdMdcContext;
import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.dto.response.GenerateTraceIdResponse;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("TraceIdFilter 테스트")
class TraceIdFilterTest {

    private static final String VALID_TRACE_ID =
            "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

    private TraceIdFilter filter;
    private GenerateTraceIdUseCase generateTraceIdUseCase;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        generateTraceIdUseCase = mock(GenerateTraceIdUseCase.class);
        filter = new TraceIdFilter(generateTraceIdUseCase);
        chain = mock(GatewayFilterChain.class);
    }

    @Nested
    @DisplayName("상수 테스트")
    class ConstantsTest {

        @Test
        @DisplayName("X_TRACE_ID_HEADER가 'X-Trace-Id'임")
        void shouldHaveCorrectHeaderName() {
            assertThat(TraceIdFilter.X_TRACE_ID_HEADER).isEqualTo("X-Trace-Id");
        }

        @Test
        @DisplayName("TRACE_ID_ATTRIBUTE가 'traceId'임")
        void shouldHaveCorrectAttributeName() {
            assertThat(TraceIdFilter.TRACE_ID_ATTRIBUTE).isEqualTo("traceId");
        }
    }

    @Nested
    @DisplayName("getOrder() 메서드 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("Filter Order가 TRACE_ID_FILTER임")
        void shouldReturnCorrectOrder() {
            assertThat(filter.getOrder()).isEqualTo(GatewayFilterOrder.TRACE_ID_FILTER);
        }

        @Test
        @DisplayName("가장 높은 우선순위 (HIGHEST_PRECEDENCE)임")
        void shouldHaveHighestPrecedence() {
            assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("filter() 메서드 테스트")
    class FilterTest {

        @Test
        @DisplayName("Request Header에 X-Trace-Id 추가")
        void shouldAddTraceIdToRequestHeader() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - ArgumentCaptor로 chain.filter에 전달된 exchange 검증
            ArgumentCaptor<ServerWebExchange> exchangeCaptor =
                    ArgumentCaptor.forClass(ServerWebExchange.class);
            verify(chain).filter(exchangeCaptor.capture());
            ServerWebExchange capturedExchange = exchangeCaptor.getValue();
            assertThat(
                            capturedExchange
                                    .getRequest()
                                    .getHeaders()
                                    .getFirst(TraceIdFilter.X_TRACE_ID_HEADER))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("Exchange Attribute에 traceId 저장")
        void shouldStoreTraceIdInExchangeAttribute() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            filter.filter(exchange, chain).block();

            // then
            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("Reactor Context에 traceId 추가")
        void shouldAddTraceIdToReactorContext() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // Capture context
            when(chain.filter(any()))
                    .thenAnswer(
                            invocation -> {
                                return Mono.deferContextual(
                                        ctx -> {
                                            assertThat(ctx.hasKey(TraceIdMdcContext.TRACE_ID_KEY))
                                                    .isTrue();
                                            assertThat(
                                                            ctx.<String>get(
                                                                    TraceIdMdcContext.TRACE_ID_KEY))
                                                    .isEqualTo(VALID_TRACE_ID);
                                            return Mono.empty();
                                        });
                            });

            // when & then
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }

        @Test
        @DisplayName("chain.filter() 호출")
        void shouldCallChainFilter() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - chain.filter가 정확히 1회 호출됨
            verify(chain).filter(any(ServerWebExchange.class));
        }
    }

    @Nested
    @DisplayName("GlobalFilter 인터페이스 구현 테스트")
    class GlobalFilterImplementationTest {

        @Test
        @DisplayName("GlobalFilter를 구현함")
        void shouldImplementGlobalFilter() {
            assertThat(filter)
                    .isInstanceOf(org.springframework.cloud.gateway.filter.GlobalFilter.class);
        }

        @Test
        @DisplayName("Ordered를 구현함")
        void shouldImplementOrdered() {
            assertThat(filter).isInstanceOf(org.springframework.core.Ordered.class);
        }
    }

    @Nested
    @DisplayName("다양한 요청 시나리오 테스트")
    class RequestScenarioTest {

        @Test
        @DisplayName("GET 요청 처리")
        void shouldHandleGetRequest() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("POST 요청 처리")
        void shouldHandlePostRequest() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request =
                    MockServerHttpRequest.post("/api/v1/users")
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
                            .body("{\"name\":\"test\"}");
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("기존 헤더가 있는 요청 처리")
        void shouldHandleRequestWithExistingHeaders() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/test")
                            .header("Authorization", "Bearer token123")
                            .header("Custom-Header", "custom-value")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }
    }

    @Nested
    @DisplayName("X-Trace-Id 헤더 재사용 테스트")
    class TraceIdReuseTest {

        @Test
        @DisplayName("유효한 기존 X-Trace-Id 헤더가 있으면 재사용")
        void shouldReuseValidExistingTraceId() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/test")
                            .header(TraceIdFilter.X_TRACE_ID_HEADER, VALID_TRACE_ID)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - UseCase가 호출되지 않아야 함 (기존 헤더 재사용)
            verify(generateTraceIdUseCase, never()).execute(any(GenerateTraceIdCommand.class));

            // Exchange Attribute에 기존 traceId가 저장됨
            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("유효하지 않은 X-Trace-Id 헤더가 있으면 새로 생성")
        void shouldGenerateNewTraceIdWhenExistingIsInvalid() {
            // given
            String invalidTraceId = "invalid-trace-id-format";
            String newTraceId = "20250124123456789-b1c2d3e4-f5a6-4890-abcd-ef1234567890";

            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(newTraceId)));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/test")
                            .header(TraceIdFilter.X_TRACE_ID_HEADER, invalidTraceId)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - UseCase가 호출되어 새 Trace-ID 생성
            verify(generateTraceIdUseCase).execute(any(GenerateTraceIdCommand.class));

            // Exchange Attribute에 새로 생성된 traceId가 저장됨
            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(newTraceId);
        }

        @Test
        @DisplayName("빈 X-Trace-Id 헤더가 있으면 새로 생성")
        void shouldGenerateNewTraceIdWhenExistingIsBlank() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/test")
                            .header(TraceIdFilter.X_TRACE_ID_HEADER, "   ")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - UseCase가 호출되어 새 Trace-ID 생성
            verify(generateTraceIdUseCase).execute(any(GenerateTraceIdCommand.class));

            // Exchange Attribute에 새로 생성된 traceId가 저장됨
            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("X-Trace-Id 헤더가 없으면 새로 생성")
        void shouldGenerateNewTraceIdWhenNoHeader() {
            // given
            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(VALID_TRACE_ID)));

            MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            // then - UseCase가 호출되어 새 Trace-ID 생성
            verify(generateTraceIdUseCase).execute(any(GenerateTraceIdCommand.class));

            // Exchange Attribute에 새로 생성된 traceId가 저장됨
            assertThat((String) exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(VALID_TRACE_ID);
        }
    }

    @Nested
    @DisplayName("매 요청마다 새 Trace-ID 생성 테스트")
    class UniqueTraceIdTest {

        @Test
        @DisplayName("매 요청마다 다른 Trace-ID 사용")
        void shouldUseDifferentTraceIdForEachRequest() {
            // given
            String traceId1 = "20250124123456789-11111111-1111-1111-1111-111111111111";
            String traceId2 = "20250124123456789-22222222-2222-2222-2222-222222222222";

            when(generateTraceIdUseCase.execute(any(GenerateTraceIdCommand.class)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(traceId1)))
                    .thenReturn(Mono.just(new GenerateTraceIdResponse(traceId2)));

            MockServerWebExchange exchange1 =
                    MockServerWebExchange.from(MockServerHttpRequest.get("/test1").build());
            MockServerWebExchange exchange2 =
                    MockServerWebExchange.from(MockServerHttpRequest.get("/test2").build());

            when(chain.filter(any())).thenReturn(Mono.empty());

            // when
            filter.filter(exchange1, chain).block();
            filter.filter(exchange2, chain).block();

            // then
            assertThat((String) exchange1.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(traceId1);
            assertThat((String) exchange2.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .isEqualTo(traceId2);
        }
    }
}
