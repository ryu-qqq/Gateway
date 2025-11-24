# Spring 헥사고날 아키텍처 코딩 컨벤션

> **Spring Boot 3.5.x + Java 21** 기반 헥사고날 아키텍처 엔터프라이즈 표준 가이드

---

## 🎯 목적 (Purpose)

이 컨벤션은 **유지보수 가능하고 확장 가능한 엔터프라이즈 애플리케이션**을 개발하기 위한 표준 규칙을 제공합니다.

### 핵심 목표

1. **일관성 (Consistency)**: 모든 팀원이 동일한 코딩 스타일과 패턴을 사용
2. **유지보수성 (Maintainability)**: 명시적이고 읽기 쉬운 코드로 장기적인 유지보수 비용 절감
3. **확장성 (Scalability)**: 헥사고날 아키텍처로 비즈니스 요구사항 변화에 유연하게 대응
4. **품질 (Quality)**: Zero-Tolerance 규칙으로 코드 품질 보장

---

## 📖 개요 (Overview)

### 아키텍처 원칙

이 프로젝트는 **헥사고날 아키텍처 (Ports & Adapters)** 를 기반으로 합니다:

- **Domain Layer**: 순수 비즈니스 로직 (기술 독립적)
- **Application Layer**: UseCase + Transaction 관리
- **Adapter Layer**: 외부 시스템 (REST API, Database) 연동
- **의존성 역전**: Domain ← Application ← Adapter

### 핵심 패턴

- **DDD (Domain-Driven Design)**: Aggregate 중심 설계
- **CQRS**: Command/Query 완전 분리
- **Port/Adapter Pattern**: 기술 독립적 비즈니스 로직
- **Pure Java 원칙**: Lombok, MapStruct 등 코드 생성 도구 금지

---

## ⚠️ Zero-Tolerance 규칙

다음 규칙은 **예외 없이 반드시 준수**해야 합니다:

| 번호 | 규칙 | 적용 레이어 | 이유 |
|------|------|-------------|------|
| 1 | **Lombok 전면 금지** | 전체 | 명시적 코드, 디버깅 용이성 |
| 2 | **Law of Demeter** | Domain | 캡슐화, 결합도 감소 |
| 3 | **Long FK 전략** | Persistence | N+1 회피, 성능 최적화 |
| 4 | **Transaction 경계** | Application | 외부 API 호출 격리 |
| 5 | **Spring Proxy 제약** | Application | @Transactional 정상 작동 보장 |
| 6 | **Javadoc 필수** | 전체 | 문서화, 협업 효율성 |
| 7 | **Scope 준수** | 전체 | 요청된 기능만 구현 |

### 상세 설명

#### 1️⃣ Lombok 전면 금지
```java
// ❌ 금지
@Data
@Builder
public class Order { ... }

// ✅ 권장
public class Order {
    private Long id;

    public Long getId() {
        return id;
    }
}
```

#### 2️⃣ Law of Demeter (Getter 체이닝 금지)
```java
// ❌ 금지
String zipCode = order.getCustomer().getAddress().getZipCode();

// ✅ 권장
String zipCode = order.getCustomerZipCode();
```

#### 3️⃣ Long FK 전략 (JPA 관계 금지)
```java
// ❌ 금지
@Entity
public class OrderJpaEntity {
    @ManyToOne
    private CustomerJpaEntity customer;
}

// ✅ 권장
@Entity
public class OrderJpaEntity {
    private Long customerId; // Long FK
}
```

#### 4️⃣ Transaction 경계
```java
// ❌ 금지
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    paymentClient.processPayment(order); // 외부 API 호출
}

// ✅ 권장
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
}

public void processPayment(Order order) {
    paymentClient.processPayment(order); // 트랜잭션 밖
}
```

---

## 📁 레이어별 가이드 구조

### 00. 프로젝트 설정
- [멀티모듈 구조](./00-project-setup/multi-module-structure.md) - 헥사고날 멀티모듈 구조 및 의존성 규칙
- [버전 관리](./00-project-setup/version-management.md) - gradle.properties 버전 관리 (하드코딩 금지)

### 01. Adapter-In Layer (REST API)
> **역할**: HTTP 요청/응답 처리 → UseCase 위임

**핵심 원칙**:
- Thin Controller (비즈니스 로직 금지)
- Pure Java (Lombok 금지, Record 사용)
- Bean Validation 필수
- RESTful 설계 (리소스 기반 URI)

**디렉토리 구조**:
```
01-adapter-in-layer/rest-api/
├── controller/          # HTTP 요청 진입점 (CQRS 분리)
├── dto/
│   ├── command/        # 상태 변경 요청 (POST, PUT, PATCH, DELETE)
│   ├── query/          # 조회 조건 요청 (GET)
│   └── response/       # HTTP 응답 DTO
├── config/             # 엔드포인트 설정
├── mapper/             # API DTO ↔ UseCase DTO 변환
├── filter/             # 인터셉터, 필터
└── error/              # 예외 처리
```

**상세 가이드**:
- [REST API 요약 가이드](./01-adapter-in-layer/rest-api/rest-api-guide.md)
- Controller: [가이드](./01-adapter-in-layer/rest-api/controller/controller-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/controller/controller-archunit.md)
- DTO:
  - Command: [가이드](./01-adapter-in-layer/rest-api/dto/command/command-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/command/command-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/command/command-dto-archunit.md)
  - Query: [가이드](./01-adapter-in-layer/rest-api/dto/query/query-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/query/query-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/query/query-dto-archunit.md)
  - Response: [가이드](./01-adapter-in-layer/rest-api/dto/response/response-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/response/response-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/response/response-dto-archunit.md)
- Configuration: [엔드포인트 Properties](./01-adapter-in-layer/rest-api/config/endpoint-properties-guide.md)
- Error Handling: [전략](./01-adapter-in-layer/rest-api/error/error-handling-strategy.md) | [매퍼 구현](./01-adapter-in-layer/rest-api/error/error-mapper-implementation-guide.md)

---

### 02. Domain Layer
> **역할**: 순수 비즈니스 로직 (기술 독립적)

**핵심 원칙**:
- Pure Java (Lombok 절대 금지)
- Law of Demeter 엄수 (Tell, Don't Ask)
- Aggregate 중심 설계
- 불변성 우선 (Setter 금지)
- 기술 독립성 (JPA, Spring 어노테이션 금지)

**디렉토리 구조**:
```
02-domain-layer/
├── aggregate/          # Aggregate Root + Entity
├── vo/                 # Value Object (불변 객체)
├── event/              # Domain Event (옵션)
└── exception/          # BC 전용 예외
```

**상세 가이드**:
- [Domain 요약 가이드](./02-domain-layer/domain-guide.md)
- Aggregate: [가이드](./02-domain-layer/aggregate/aggregate-guide.md) | [테스트](./02-domain-layer/aggregate/aggregate-test-guide.md) | [ArchUnit](./02-domain-layer/aggregate/aggregate-archunit.md)
- Value Object: [가이드](./02-domain-layer/vo/vo-guide.md) | [테스트](./02-domain-layer/vo/vo-test-guide.md) | [ArchUnit](./02-domain-layer/vo/vo-archunit.md)
- Exception: [가이드](./02-domain-layer/exception/exception-guide.md) | [테스트](./02-domain-layer/exception/exception-test-guide.md) | [ArchUnit](./02-domain-layer/exception/exception-archunit-guide.md)

---

### 03. Application Layer
> **역할**: UseCase + Transaction 관리

**핵심 원칙**:
- UseCase = 단일 비즈니스 트랜잭션
- Transaction 경계 엄격 (외부 API 호출 금지)
- CQRS 분리 고정 (Command/Query 절대 분리)
- Domain 중심 설계 (Domain 호출 Orchestrator)
- Port/Adapter 패턴
- Spring Proxy 제약 준수

**디렉토리 구조**:
```
03-application-layer/
├── assembler/          # DTO ↔ Domain 변환
├── dto/
│   ├── command/        # Command DTO
│   ├── query/          # Query DTO
│   └── response/       # Response DTO
├── facade/             # 여러 Transaction Manager 조합
├── manager/            # 단일 Transaction 처리
├── port/
│   ├── in/
│   │   ├── command/    # Command UseCase (상태 변경)
│   │   └── query/      # Query UseCase (조회)
│   └── out/
│       ├── command/    # Persistence Port (저장)
│       └── query/      # Query Port (조회)
├── service/            # UseCase 구현 (Transaction 경계)
├── listener/           # Domain Event Listener
└── scheduler/          # 배치 작업
```

**상세 가이드**:
- [Application 요약 가이드](./03-application-layer/application-guide.md)
- Assembler: [가이드](./03-application-layer/assembler/assembler-guide.md) | [테스트](./03-application-layer/assembler/assembler-test-guide.md) | [ArchUnit](./03-application-layer/assembler/assembler-archunit.md)
- DTO:
  - Command: [가이드](./03-application-layer/dto/command/command-dto-guide.md)
  - Query: [가이드](./03-application-layer/dto/query/query-dto-guide.md)
  - Response: [가이드](./03-application-layer/dto/response/response-dto-guide.md)
  - ArchUnit: [Record 규칙](./03-application-layer/dto/06_archunit-dto-record-rules.md)
- Facade: [가이드](./03-application-layer/facade/facade-guide.md) | [테스트](./03-application-layer/facade/facade-test-guide.md)
- Transaction Manager: [가이드](./03-application-layer/manager/transaction-manager-guide.md) | [테스트](./03-application-layer/manager/transaction-manager-test-guide.md)
- Port-In:
  - Command: [가이드](./03-application-layer/port/in/command/port-in-command-guide.md) | [ArchUnit](./03-application-layer/port/in/command/port-in-command-archunit.md)
  - Query: [가이드](./03-application-layer/port/in/query/port-in-query-guide.md) | [ArchUnit](./03-application-layer/port/in/query/port-in-query-archunit.md)
- Port-Out:
  - Command: [가이드](./03-application-layer/port/out/command/port-out-command-guide.md) | [ArchUnit](./03-application-layer/port/out/command/port-out-command-archunit.md)
  - Query: [가이드](./03-application-layer/port/out/query/port-out-query-guide.md) | [ArchUnit](./03-application-layer/port/out/query/port-out-query-archunit.md)

---

### 04. Persistence Layer
> **역할**: 저장소 (Database) 연동

**핵심 원칙**:
- 어댑터 = 비즈니스 로직 금지 (저장/조회만)
- CQRS 분리 고정 (Command=JPA, Query=QueryDSL)
- Transaction은 Application Layer 전용
- 엔티티 연관관계 금지 (Long FK 전략)
- Persist Port 통일 (생성/수정/소프트삭제)
- 매퍼는 순수 Java (MapStruct 금지)
- Open-in-View 비활성화

#### 04-1. MySQL (JPA/QueryDSL)

**디렉토리 구조**:
```
04-persistence-layer/mysql/
├── adapter/
│   ├── command/        # Command Adapter (저장)
│   └── query/          # Query Adapter (조회)
├── entity/             # JPA Entity (Long FK 전략)
├── mapper/             # Entity ↔ Domain 변환
├── repository/
│   ├── JpaRepository   # Command 전용 (저장)
│   └── QueryDslRepository # Query 전용 (조회)
└── config/             # Flyway, HikariCP 설정
```

**상세 가이드**:
- [MySQL Persistence 요약 가이드](./04-persistence-layer/mysql/persistence-mysql-guide.md)
- Adapter:
  - Command: [가이드](./04-persistence-layer/mysql/adapter/command/command-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/command/command-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/command/command-adapter-archunit.md)
  - Query: [가이드](./04-persistence-layer/mysql/adapter/query/query-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/query/query-adapter-test-guide.md) | [통합 테스트](./04-persistence-layer/mysql/adapter/query/query-adapter-integration-testing.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/query/query-adapter-archunit.md)
  - Lock Query: [가이드](./04-persistence-layer/mysql/adapter/query/lock-query-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/query/lock-query-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/query/lock-query-adapter-archunit.md)
- Entity: [가이드](./04-persistence-layer/mysql/entity/entity-guide.md) | [테스트](./04-persistence-layer/mysql/entity/entity-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/entity/entity-archunit.md)
- Mapper: [가이드](./04-persistence-layer/mysql/mapper/mapper-guide.md) | [테스트](./04-persistence-layer/mysql/mapper/mapper-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/mapper/mapper-archunit.md)
- Repository:
  - JPA: [가이드](./04-persistence-layer/mysql/repository/jpa-repository-guide.md) | [ArchUnit](./04-persistence-layer/mysql/repository/jpa-repository-archunit.md)
  - QueryDSL: [가이드](./04-persistence-layer/mysql/repository/querydsl-repository-guide.md) | [테스트](./04-persistence-layer/mysql/repository/querydsl-repository-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/repository/querydsl-repository-archunit.md)
- Configuration: [Flyway 테스트](./04-persistence-layer/mysql/config/flyway-testing-guide.md) | [HikariCP 설정](./04-persistence-layer/mysql/config/hikaricp-configuration.md)

#### 04-2. Redis (Cache)

**디렉토리 구조**:
```
04-persistence-layer/redis/
├── adapter/            # Cache Adapter
└── config/             # Lettuce 설정
```

**상세 가이드**:
- [Redis Persistence 요약 가이드](./04-persistence-layer/redis/persistence-redis-guide.md)
- Adapter: [가이드](./04-persistence-layer/redis/adapter/cache-adapter-guide.md) | [테스트](./04-persistence-layer/redis/adapter/cache-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/redis/adapter/cache-adapter-archunit.md)
- Configuration: [Lettuce 설정](./04-persistence-layer/redis/config/cache-configuration.md)

---

### 05. Testing
> **역할**: 테스트 전략 및 Test Fixtures

**핵심 원칙**:
- Test Fixtures = 재사용 가능한 테스트 데이터
- Layer별 독립적인 Fixture 모듈
- ArchUnit으로 아키텍처 규칙 자동 검증
- Pure Java 테스트 (Lombok 금지)

**디렉토리 구조**:
```
05-testing/
└── test-fixtures/      # Test Fixtures 가이드
```

**상세 가이드**:
- Test Fixtures: [가이드](./05-testing/test-fixtures/01_test-fixtures-guide.md) | [ArchUnit](./05-testing/test-fixtures/02_test-fixtures-archunit.md)

---

## 🔍 가이드 활용 방법

### 1. 신규 개발 시작
1. **프로젝트 설정**: [멀티모듈 구조](./00-project-setup/multi-module-structure.md) 참고
2. **Domain 설계**: [Domain 가이드](./02-domain-layer/domain-guide.md) 참고
3. **UseCase 구현**: [Application 가이드](./03-application-layer/application-guide.md) 참고
4. **REST API 개발**: [REST API 가이드](./01-adapter-in-layer/rest-api/rest-api-guide.md) 참고
5. **Persistence 연동**: [MySQL 가이드](./04-persistence-layer/mysql/persistence-mysql-guide.md) 참고

### 2. 코드 리뷰 시
1. **Zero-Tolerance 규칙** 위반 여부 확인
2. **Layer별 상세 가이드** 준수 여부 확인
3. **ArchUnit 테스트** 통과 여부 확인

### 3. 리팩토링 시
1. **현재 레이어의 가이드** 재확인
2. **의존성 규칙** 준수 여부 확인
3. **Pure Java 원칙** 위반 제거

---

## 📊 컨벤션 검증 체계

### 3-Tier 자동 검증 시스템

| Tier | 검증 시점 | 도구 | 목적 |
|------|----------|------|------|
| 1 | 코드 생성 직후 | validation-helper.py | 실시간 규칙 검증 (148ms) |
| 2 | Git Commit 시 | Git pre-commit hook | Transaction 경계 검증 |
| 3 | 빌드 시 | ArchUnit | 아키텍처 규칙 검증 |

### ArchUnit 검증 항목

**Domain Layer**:
- Lombok 사용 금지
- 외부 기술 의존성 금지 (JPA, Spring 등)
- Public Setter 금지

**Application Layer**:
- Port-In/Out 인터페이스 네이밍 규칙
- Transaction 경계 준수
- DTO Record 타입 강제

**Persistence Layer**:
- JPA 관계 어노테이션 금지 (@OneToMany, @ManyToOne 등)
- Entity Setter 금지
- CQRS 분리 (JPA vs QueryDSL)

---

## ✅ 체크리스트

코드 작성 시:
- [ ] Lombok 사용 여부 확인
- [ ] Law of Demeter 준수 여부
- [ ] Transaction 경계 준수 여부
- [ ] Javadoc 작성 여부 (public 클래스/메서드)
- [ ] ArchUnit 테스트 통과 여부
- [ ] Git pre-commit hook 통과 여부

---

## 📞 문의 및 기여

- **버그 리포트**: GitHub Issues
- **기여 가이드**: CONTRIBUTING.md
- **질문 및 토론**: GitHub Discussions

---

- **작성자**: ryu-qqq
- **최종 수정일**: 2025-11-13
- **버전**: 1.0.0
