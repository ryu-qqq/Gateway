# REST API Layer — **HTTP Adapter & RESTful Design**

> 이 문서는 `adapter-in/rest-api` 레이어의 **요약 가이드**입니다.
>
> **핵심 원칙**, **패키징 구조**, 그리고 각 영역별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **Thin Controller**: Controller는 **HTTP 요청/응답 처리만**. 비즈니스 로직은 UseCase에 위임.
* **Pure Java 원칙**: Lombok **절대 금지**. Request/Response DTO는 Java 21 Record 사용.
* **DI Mapper**: Mapper는 **@Component**로 DI. 의존성 필요 시 주입 가능 (MessageSource 등).
* **Bean Validation 필수**: 모든 Request DTO에 `@Valid` + 제약 조건 어노테이션.
* **RESTful 설계**: 리소스 기반 URI (명사 복수형) + HTTP 메서드 활용.
* **CQRS 분리**: Command/Query Controller 분리. 의존성 관리 용이.
* **UseCase 직접 의존**: Controller는 **UseCase 직접 주입**. 5-10개 의존성은 정상.
* **RFC 7807 준수**: Error Response는 Problem Details 표준 준수.
* **API DTO 분리**: Domain/Application DTO 직접 노출 금지. API 전용 DTO 사용.

### 금지사항

* **Lombok 전면 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter` 등 모든 어노테이션
* **RPC 스타일 URI**: 동사 기반 URI (`/createOrder`, `/getOrders` 등)
* **Domain 직접 노출**: Controller에서 Domain Entity 반환 금지
* **비즈니스 로직**: Controller/Mapper에 비즈니스 로직 구현 금지

---

## 2) 패키징 구조 (Bounded Context 예)

```
adapter-in/rest-api/
├─ config/
│  └─ properties/
│     └─ ApiEndpointProperties.java
│
├─ common/
│  ├─ dto/
│  │  ├─ ApiResponse.java          # 성공 응답 Wrapper
│  │  ├─ ErrorInfo.java             # 에러 응답 상세
│  │  ├─ SliceApiResponse.java      # Slice 페이징 응답
│  │  └─ PageApiResponse.java       # Page 페이징 응답
│  ├─ controller/
│  │  └─ GlobalExceptionHandler.java
│  ├─ error/
│  │  └─ ErrorMapperRegistry.java   # ErrorMapper 레지스트리
│  ├─ mapper/
│  │  └─ ErrorMapper.java           # ErrorMapper 인터페이스
│  └─ filter/
│     └─ LoggingFilter.java
│
└─ [boundedContext]/                # 예: order
   ├─ controller/
   │  ├─ OrderCommandController.java  # POST, PUT, PATCH, DELETE
   │  └─ OrderQueryController.java    # GET
   │
   ├─ dto/
   │  ├─ command/                    # Command 요청 DTO (상태 변경)
   │  │   ├─ CreateOrderApiRequest.java
   │  │   ├─ UpdateOrderStatusApiRequest.java
   │  │   └─ CancelOrderApiRequest.java
   │  ├─ query/                      # Query 요청 DTO (조회 조건)
   │  │   ├─ OrderSearchApiRequest.java
   │  │   └─ OrderDetailApiRequest.java
   │  └─ response/                   # 응답 DTO
   │      ├─ OrderApiResponse.java
   │      └─ OrderSummaryApiResponse.java
   │
   ├─ mapper/
   │  └─ OrderApiMapper.java         # ← @Component (DI)
   │
   └─ error/
      └─ OrderApiErrorMapper.java    # ← @Component
```

**패키지 네이밍 규칙**:
- Bounded Context: 소문자, 단수형 (`order`, `product`)
- DTO 접미사:
  - Command: `*ApiRequest` (예: `CreateOrderApiRequest`)
  - Query: `*ApiRequest` (예: `OrderSearchApiRequest`)
  - Response: `*ApiResponse` (예: `OrderApiResponse`)
- Controller: `*CommandController`, `*QueryController`
- Mapper: `*ApiMapper` (API DTO ↔ UseCase DTO 변환)
- Error Mapper: `*ApiErrorMapper` (Domain Exception → HTTP 변환)

**패키지 배치 원칙**:
- `config/`: 전역 설정 (Properties, Interceptor 등)
- `common/`: 공통 컴포넌트 (전체 BC에서 재사용)
- `{bc}/controller/`: HTTP 요청 진입점 (CQRS 분리)
- `{bc}/dto/command/`: 상태 변경 요청 (POST, PUT, PATCH, DELETE)
- `{bc}/dto/query/`: 조회 조건 요청 (GET)
- `{bc}/dto/response/`: 응답 데이터
- `{bc}/mapper/`: API ↔ UseCase DTO 변환
- `{bc}/error/`: BC 전용 에러 매핑

---

## 3) 영역별 상세 가이드 링크

### 🎯 **Controller 설계**
* [Controller Guide](./controller/controller-guide.md) - Controller 작성 가이드 (Thin Controller, ResponseEntity 래핑)
  * [ArchUnit Guide](./controller/controller-archunit.md) - 아키텍처 검증 가이드

### 📦 **DTO 패턴**
* [**DTO Industry Standards**](./dto/dto-industry-standards.md) - **업계 표준 가이드** (ISO 8601, RFC 7807, 버전 관리, Idempotency)
* [Command DTO Guide](./dto/command/command-dto-guide.md) - 상태 변경 요청 DTO (POST, PUT, PATCH, DELETE)
  * [Test Guide](./dto/command/command-dto-test-guide.md) - 단위 테스트 가이드
  * [ArchUnit Guide](./dto/command/command-dto-archunit.md) - 아키텍처 검증 가이드
* [Query DTO Guide](./dto/query/query-dto-guide.md) - 조회 조건 요청 DTO (GET)
  * [Test Guide](./dto/query/query-dto-test-guide.md) - 단위 테스트 가이드
  * [ArchUnit Guide](./dto/query/query-dto-archunit.md) - 아키텍처 검증 가이드
* [Response DTO Guide](./dto/response/response-dto-guide.md) - HTTP 응답 DTO (SliceApiResponse/PageApiResponse)
  * [Test Guide](./dto/response/response-dto-test-guide.md) - 단위 테스트 가이드
  * [ArchUnit Guide](./dto/response/response-dto-archunit.md) - 아키텍처 검증 가이드

### 🔄 **Mapper 패턴**
* [Mapper Guide](./mapper/mapper-guide.md) - API DTO ↔ Application DTO 변환 가이드 (@Component Bean, Static 금지)

### 🚨 **예외 처리**
* [Error Guide](./error/error-guide.md) - 에러 처리 가이드 (RFC 7807, GlobalExceptionHandler, ErrorMapper)
  * [Test Guide](./error/error-test-guide.md) - 테스트 가이드
  * [ArchUnit Guide](./error/error-archunit.md) - 아키텍처 검증 가이드

### 📖 **OpenAPI/Swagger**
* [OpenAPI Guide](./openapi/openapi-guide.md) - OpenAPI 3.0 어노테이션 가이드 (@Operation, @Schema, @ApiResponse)
  * [ArchUnit Guide](./openapi/openapi-archunit.md) - 아키텍처 검증 규칙 (12개)

### ⚙️ **Configuration**
* [Endpoint Properties Guide](./config/endpoint-properties-guide.md) - 엔드포인트 중앙 관리 가이드 (ApiEndpointProperties)

### 🧪 **Testing**
* [Controller Unit Test Guide](controller/controller-unit-test-guide.md) - @WebMvcTest 단위 테스트 가이드
* [Controller REST Docs Guide](controller/controller-restdocs-guide.md) - 테스트 기반 API 문서 자동 생성 가이드

### 📚 **전체 가이드**
* TBD (작업 예정)

### 🌐 **i18n**
* TBD (작업 예정)

---