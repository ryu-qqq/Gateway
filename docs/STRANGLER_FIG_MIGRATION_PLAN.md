# Strangler Fig 마이그레이션 계획

## 개요

레거시 시스템에서 새 시스템으로 점진적 마이그레이션을 위한 Strangler Fig 패턴 적용 계획.
API 레벨 어댑터를 사용하여 스키마 차이를 해결하면서 점진적으로 전환.

---

## 현재 상황

```
┌─────────────────────────────────────────────────────────────┐
│                      현재 아키텍처                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  레거시 서버                    새 서버                      │
│  (bootstrap-legacy-web-api)    (rest-api, rest-api-admin)   │
│         │                              │                    │
│         ▼                              ▼                    │
│    레거시 DB                        새 DB                   │
│   (구 스키마)                    (개선된 스키마)             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 문제점
- 레거시 스키마와 신규 스키마가 다름
- 한 번에 전환 시 리스크 높음
- 롤백 어려움

---

## 목표 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Gateway                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  /api/v2/products/** → 새 서버                          ││
│  │  /api/v2/orders/**   → 새 서버                          ││
│  │  /api/v1/**          → 레거시 (점진적 축소)              ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
          새 서버                       레거시 서버
     ┌─────────────────┐           ┌─────────────────┐
     │ Application     │           │                 │
     │ Layer           │           │                 │
     ├─────────────────┤           │                 │
     │ Adapter Layer   │           │                 │
     │ ┌─────┐ ┌─────┐ │           │                 │
     │ │Leg  │ │New  │ │           │                 │
     │ │Repo │ │Repo │ │           │                 │
     │ └──┬──┘ └──┬──┘ │           │                 │
     └────┼───────┼────┘           └────────┬────────┘
          │       │                         │
          ▼       ▼                         ▼
     레거시 DB  새 DB                   레거시 DB
```

---

## 마이그레이션 전략: API 레벨 어댑터

### 핵심 개념

새 서버의 Application Layer는 새 스키마 기준으로 작성하고,
Adapter Layer에서 레거시 DB 접근 시 스키마 변환을 처리.

```java
// Application Layer - 새 스키마 기준
public class ProductService {
    private final ProductRepository productRepository;

    public Product getProduct(Long id) {
        return productRepository.findById(id);
    }
}

// Adapter Layer - 레거시 DB용 구현
@Repository
public class LegacyProductRepositoryAdapter implements ProductRepository {

    private final LegacyProductJpaRepository legacyRepo;

    @Override
    public Product findById(Long id) {
        LegacyProductEntity legacy = legacyRepo.findById(id);
        return convertToNewSchema(legacy);  // 스키마 변환!
    }

    private Product convertToNewSchema(LegacyProductEntity legacy) {
        // 구린 스키마 → 새 스키마 변환 로직
        return Product.builder()
            .id(legacy.getProductId())
            .name(legacy.getProductNm())  // 컬럼명 다름
            .price(Money.of(legacy.getPrc(), legacy.getCurrCd()))  // 분리된 컬럼
            .build();
    }
}
```

---

## 마이그레이션 Phase

### Phase 1: 새 서버 + 레거시 DB (현재 목표)

```
┌─────────────────────────────────────────┐
│              새 서버                     │
│  ┌─────────────────────────────────┐   │
│  │ Application (새 스키마 기준)     │   │
│  └──────────────┬──────────────────┘   │
│                 │                       │
│  ┌──────────────┴──────────────────┐   │
│  │ LegacyRepositoryAdapter         │   │
│  │ (레거시 스키마 → 새 스키마 변환) │   │
│  └──────────────┬──────────────────┘   │
└─────────────────┼───────────────────────┘
                  ▼
             레거시 DB
```

**작업 내용**:
- [ ] 새 서버에서 레거시 DB 연결 설정
- [ ] 도메인별 LegacyRepositoryAdapter 구현
- [ ] 스키마 변환 로직 작성
- [ ] Gateway에서 특정 API 라우팅 변경

**장점**:
- 데이터 마이그레이션 불필요
- 롤백 쉬움 (Gateway 라우팅만 변경)
- 점진적 적용 가능

---

### Phase 2: 이중 쓰기 (Dual Write)

```
┌─────────────────────────────────────────┐
│              새 서버                     │
│  ┌─────────────────────────────────┐   │
│  │ Application (새 스키마 기준)     │   │
│  └──────────────┬──────────────────┘   │
│                 │                       │
│  ┌──────────────┴──────────────────┐   │
│  │ DualWriteRepositoryAdapter      │   │
│  │ Read: 레거시 DB                  │   │
│  │ Write: 레거시 DB + 새 DB         │   │
│  └───────┬─────────────┬───────────┘   │
└──────────┼─────────────┼────────────────┘
           ▼             ▼
       레거시 DB       새 DB
```

**작업 내용**:
- [ ] 새 DB 스키마 생성
- [ ] DualWriteRepositoryAdapter 구현
- [ ] Write 시 양쪽 DB에 동시 저장
- [ ] Read는 여전히 레거시 DB

---

### Phase 3: 새 DB로 Read 전환

```
┌─────────────────────────────────────────┐
│              새 서버                     │
│  ┌─────────────────────────────────┐   │
│  │ Application (새 스키마 기준)     │   │
│  └──────────────┬──────────────────┘   │
│                 │                       │
│  ┌──────────────┴──────────────────┐   │
│  │ NewPrimaryRepositoryAdapter     │   │
│  │ Read: 새 DB (Primary)            │   │
│  │ Write: 새 DB + 레거시 DB (Sync)  │   │
│  └───────┬─────────────┬───────────┘   │
└──────────┼─────────────┼────────────────┘
           ▼             ▼
        새 DB        레거시 DB
       (Primary)     (Secondary)
```

**작업 내용**:
- [ ] 데이터 정합성 검증
- [ ] Read를 새 DB로 전환
- [ ] 레거시 DB는 동기화용으로만 유지

---

### Phase 4: 레거시 DB 제거

```
┌─────────────────────────────────────────┐
│              새 서버                     │
│  ┌─────────────────────────────────┐   │
│  │ Application (새 스키마 기준)     │   │
│  └──────────────┬──────────────────┘   │
│                 │                       │
│  ┌──────────────┴──────────────────┐   │
│  │ NewRepository (최종)             │   │
│  └──────────────┬──────────────────┘   │
└─────────────────┼───────────────────────┘
                  ▼
               새 DB
```

**작업 내용**:
- [ ] 레거시 DB 동기화 제거
- [ ] 레거시 서버 제거
- [ ] 레거시 DB 아카이브/삭제

---

## Gateway 라우팅 전략

### 라우팅 우선순위

```yaml
gateway:
  routing:
    services:
      # 1️⃣ 마이그레이션 완료된 API (구체적 경로 먼저)
      - id: commerce-new
        uri: http://commerce-web-api-prod.connectly.local:8080
        paths:
          - /api/v2/**           # 새 버전 API
          - /api/v1/products/**  # 마이그레이션 완료된 v1 API
        hosts:
          - stage.set-of.com
          - set-of.com

      # 2️⃣ 레거시로 폴백 (catch-all)
      - id: legacy-web
        uri: http://setof-commerce-legacy-api-prod.connectly.local:8080
        paths:
          - /**
        hosts:
          - stage.set-of.com
          - set-of.com
```

### 마이그레이션 진행에 따른 라우팅 변경

| 단계 | 새 서버로 라우팅 | 레거시로 라우팅 |
|------|-----------------|----------------|
| Week 1 | `/api/v1/products/**` | 나머지 전부 |
| Week 2 | + `/api/v1/categories/**` | 나머지 전부 |
| Week 3 | + `/api/v1/brands/**` | 나머지 전부 |
| ... | 점점 추가 | 점점 축소 |
| Final | 전부 | 없음 |

---

## 롤백 전략

### 즉시 롤백 (Gateway 라우팅)

문제 발생 시 Gateway 설정에서 해당 경로를 레거시로 복귀:

```yaml
# 롤백 전
- /api/v1/products/** → 새 서버

# 롤백 후
- /api/v1/products/** → (제거, catch-all로 레거시 사용)
```

### Circuit Breaker (자동 폴백)

```java
@Bean
public RouteLocator routesWithCircuitBreaker(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("products-with-fallback", r -> r
            .path("/api/v1/products/**")
            .filters(f -> f
                .circuitBreaker(config -> config
                    .setName("productsCircuitBreaker")
                    .setFallbackUri("forward:/fallback/legacy")))
            .uri("http://new-server"))
        .build();
}
```

---

## 도메인별 마이그레이션 우선순위 (예시)

| 우선순위 | 도메인 | 이유 | 예상 난이도 |
|---------|--------|------|------------|
| 1 | Products | 조회 위주, 리스크 낮음 | 낮음 |
| 2 | Categories | 단순 구조 | 낮음 |
| 3 | Brands | 단순 구조 | 낮음 |
| 4 | Users | 인증 연동 필요 | 중간 |
| 5 | Orders | 복잡한 상태 관리 | 높음 |
| 6 | Payments | 외부 연동, 리스크 높음 | 높음 |

---

## 체크리스트

### Phase 1 시작 전
- [ ] 새 서버에서 레거시 DB 접속 가능 확인
- [ ] 스키마 차이점 문서화
- [ ] 어댑터 인터페이스 설계
- [ ] 첫 번째 마이그레이션 도메인 선정

### 각 도메인 마이그레이션 시
- [ ] LegacyRepositoryAdapter 구현
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] Gateway 라우팅 변경
- [ ] 모니터링 설정
- [ ] 롤백 계획 확인

---

## 모니터링

### 주요 메트릭

- **응답 시간**: 새 서버 vs 레거시 서버 비교
- **에러율**: 새 서버 에러율 모니터링
- **트래픽 비율**: 새 서버로 가는 트래픽 %

### 알림 설정

- 새 서버 에러율 > 1% → 알림
- 응답 시간 > 500ms → 알림
- Circuit Breaker OPEN → 즉시 알림

---

## 참고 자료

- [Strangler Fig Pattern - Martin Fowler](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Branch by Abstraction](https://martinfowler.com/bliki/BranchByAbstraction.html)
