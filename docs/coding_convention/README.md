# Spring 헥사고날 아키텍처 코딩 컨벤션

> **Spring Boot 3.5.x + Java 21** 기반 헥사고날 아키텍처 엔터프라이즈 표준 가이드

---

## 목적 (Purpose)

이 컨벤션은 **유지보수 가능하고 확장 가능한 엔터프라이즈 애플리케이션**을 개발하기 위한 표준 규칙을 제공합니다.

### 핵심 목표

1. **일관성 (Consistency)**: 모든 팀원이 동일한 코딩 스타일과 패턴을 사용
2. **유지보수성 (Maintainability)**: 명시적이고 읽기 쉬운 코드로 장기적인 유지보수 비용 절감
3. **확장성 (Scalability)**: 헥사고날 아키텍처로 비즈니스 요구사항 변화에 유연하게 대응
4. **품질 (Quality)**: Zero-Tolerance 규칙으로 코드 품질 보장

---

## 개요 (Overview)

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

## Zero-Tolerance 규칙

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

#### 1. Lombok 전면 금지
```java
// 금지
@Data
@Builder
public class Order { ... }

// 권장
public class Order {
    private Long id;

    public Long getId() {
        return id;
    }
}
```

#### 2. Law of Demeter (Getter 체이닝 금지)
```java
// 금지
String zipCode = order.getCustomer().getAddress().getZipCode();

// 권장
String zipCode = order.getCustomerZipCode();
```

#### 3. Long FK 전략 (JPA 관계 금지)
```java
// 금지
@Entity
public class OrderJpaEntity {
    @ManyToOne
    private CustomerJpaEntity customer;
}

// 권장
@Entity
public class OrderJpaEntity {
    private Long customerId; // Long FK
}
```

#### 4. Transaction 경계
```java
// 금지
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    paymentClient.processPayment(order); // 외부 API 호출
}

// 권장
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
}

public void processPayment(Order order) {
    paymentClient.processPayment(order); // 트랜잭션 밖
}
```

---

## 문서 통계

| 레이어 | 파일 수 | 주요 내용 |
|--------|---------|-----------|
| 00-project-setup | 5 | 멀티모듈, Gradle, GitHub Workflows, Terraform |
| 01-adapter-in-layer | 27 | REST API, Controller, DTO, Error, OpenAPI, Security |
| 02-domain-layer | 14 | Aggregate, VO, Event, Exception |
| 03-application-layer | 51 | Assembler, DTO, Event, Facade, Factory, Manager, Port, Service, Scheduler |
| 04-persistence-layer | 41 | MySQL (Adapter, Entity, Mapper, Repository), Redis (Cache, Lock) |
| 05-testing | 3 | Integration Testing, Test Fixtures |
| 06-observability | 4 | Logging, ADOT, CloudWatch |
| 07-local-development | 1 | 로컬 개발 환경 |
| **총계** | **146** | README 제외 |

---

## 레이어별 가이드 구조

### 00. 프로젝트 설정

| 문서 | 설명 |
|------|------|
| [멀티모듈 구조](./00-project-setup/multi-module-structure.md) | 헥사고날 멀티모듈 구조 및 의존성 규칙 |
| [Gradle 설정](./00-project-setup/gradle-configuration.md) | Gradle 빌드 설정 가이드 |
| [GitHub Workflows](./00-project-setup/github-workflows-guide.md) | CI/CD 워크플로우 설정 |
| [버전 관리](./00-project-setup/version-management.md) | gradle.properties 버전 관리 |
| [Terraform](./00-project-setup/terraform-guide.md) | AWS 인프라 Wrapper Module 패턴 |

---

### 01. Adapter-In Layer (REST API)

> **역할**: HTTP 요청/응답 처리 → UseCase 위임

**핵심 원칙**:
- Thin Controller (비즈니스 로직 금지)
- Pure Java (Lombok 금지, Record 사용)
- Bean Validation 필수
- RESTful 설계 (리소스 기반 URI)

#### 디렉토리 구조
```
01-adapter-in-layer/rest-api/
├── rest-api-guide.md           # 요약 가이드
├── controller/                 # HTTP 요청 진입점
├── dto/
│   ├── command/               # 상태 변경 요청 (POST, PUT, PATCH, DELETE)
│   ├── query/                 # 조회 조건 요청 (GET)
│   └── response/              # HTTP 응답 DTO
├── error/                     # 예외 처리
├── mapper/                    # API DTO ↔ UseCase DTO 변환
├── openapi/                   # OpenAPI/Swagger 설정
└── security/                  # 인증/인가 설정
```

#### 상세 가이드

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **REST API 요약** | [가이드](./01-adapter-in-layer/rest-api/rest-api-guide.md) | - | - |
| **Controller** | [가이드](./01-adapter-in-layer/rest-api/controller/controller-guide.md) | [테스트](./01-adapter-in-layer/rest-api/controller/controller-test-guide.md) / [RestDocs](./01-adapter-in-layer/rest-api/controller/controller-test-restdocs-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/controller/controller-archunit.md) |
| **DTO - Command** | [가이드](./01-adapter-in-layer/rest-api/dto/command/command-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/command/command-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/command/command-dto-archunit.md) |
| **DTO - Query** | [가이드](./01-adapter-in-layer/rest-api/dto/query/query-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/query/query-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/query/query-dto-archunit.md) |
| **DTO - Response** | [가이드](./01-adapter-in-layer/rest-api/dto/response/response-dto-guide.md) | [테스트](./01-adapter-in-layer/rest-api/dto/response/response-dto-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/dto/response/response-dto-archunit.md) |
| **DTO 업계표준** | [가이드](./01-adapter-in-layer/rest-api/dto/dto-industry-standards.md) | - | - |
| **Error Handling** | [가이드](./01-adapter-in-layer/rest-api/error/error-guide.md) | [테스트](./01-adapter-in-layer/rest-api/error/error-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/error/error-archunit.md) |
| **Mapper** | [가이드](./01-adapter-in-layer/rest-api/mapper/mapper-guide.md) | [테스트](./01-adapter-in-layer/rest-api/mapper/mapper-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/mapper/mapper-archunit.md) |
| **OpenAPI** | [가이드](./01-adapter-in-layer/rest-api/openapi/openapi-guide.md) | - | [ArchUnit](./01-adapter-in-layer/rest-api/openapi/openapi-archunit.md) |
| **Security** | [가이드](./01-adapter-in-layer/rest-api/security/security-guide.md) | [테스트](./01-adapter-in-layer/rest-api/security/security-test-guide.md) | [ArchUnit](./01-adapter-in-layer/rest-api/security/security-archunit.md) |
| **API Paths** | [가이드](./01-adapter-in-layer/rest-api/security/api-paths-guide.md) | - | - |

---

### 02. Domain Layer

> **역할**: 순수 비즈니스 로직 (기술 독립적)

**핵심 원칙**:
- Pure Java (Lombok 절대 금지)
- Law of Demeter 엄수 (Tell, Don't Ask)
- Aggregate 중심 설계
- 불변성 우선 (Setter 금지)
- 기술 독립성 (JPA, Spring 어노테이션 금지)

#### 디렉토리 구조
```
02-domain-layer/
├── domain-guide.md            # 요약 가이드
├── aggregate/                 # Aggregate Root + Entity
├── vo/                        # Value Object (불변 객체)
├── event/                     # Domain Event
└── exception/                 # BC 전용 예외
```

#### 상세 가이드

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Domain 요약** | [가이드](./02-domain-layer/domain-guide.md) | - | - |
| **Aggregate** | [가이드](./02-domain-layer/aggregate/aggregate-guide.md) | [테스트](./02-domain-layer/aggregate/aggregate-test-guide.md) | [ArchUnit](./02-domain-layer/aggregate/aggregate-archunit.md) |
| **Value Object** | [가이드](./02-domain-layer/vo/vo-guide.md) | [테스트](./02-domain-layer/vo/vo-test-guide.md) | [ArchUnit](./02-domain-layer/vo/vo-archunit.md) |
| **Query VO** | [가이드](./02-domain-layer/vo/query-vo-guide.md) | - | - |
| **LockKey** | - | - | [ArchUnit](./02-domain-layer/vo/lockkey-archunit.md) |
| **Domain Event** | [가이드](./02-domain-layer/event/event-guide.md) | - | [ArchUnit](./02-domain-layer/event/event-archunit.md) |
| **Exception** | [가이드](./02-domain-layer/exception/exception-guide.md) | [테스트](./02-domain-layer/exception/exception-test-guide.md) | [ArchUnit](./02-domain-layer/exception/exception-archunit-guide.md) |

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

#### 디렉토리 구조
```
03-application-layer/
├── application-guide.md       # 요약 가이드
├── assembler/                 # DTO ↔ Domain 변환
├── dto/
│   ├── command/              # Command DTO
│   ├── query/                # Query DTO
│   ├── response/             # Response DTO
│   └── bundle/               # Bundle 패턴 (Persist/Query)
├── event/                     # Transaction Event Registry
├── facade/
│   ├── command/              # Command Facade (여러 Manager 조합)
│   └── query/                # Query Facade
├── factory/
│   ├── command/              # Command Factory
│   └── query/                # Query Factory
├── listener/                  # Domain Event Listener
├── manager/
│   ├── command/              # Transaction Manager
│   └── query/                # Read Manager
├── port/
│   ├── in/
│   │   ├── command/          # Command UseCase (상태 변경)
│   │   └── query/            # Query UseCase (조회)
│   └── out/
│       ├── command/          # Persistence Port (저장)
│       ├── query/            # Query Port (조회)
│       ├── cache/            # Cache Port
│       └── common/           # 공통 Port (Distributed Lock 등)
├── scheduler/                 # 배치 작업
└── service/
    ├── command/              # Command Service (UseCase 구현)
    └── query/                # Query Service (UseCase 구현)
```

#### 상세 가이드

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Application 요약** | [가이드](./03-application-layer/application-guide.md) | - | - |
| **Assembler** | [가이드](./03-application-layer/assembler/assembler-guide.md) | [테스트](./03-application-layer/assembler/assembler-test-guide.md) | [ArchUnit](./03-application-layer/assembler/assembler-archunit.md) |

**DTO**:

| 컴포넌트 | 가이드 | ArchUnit |
|----------|--------|----------|
| **Command DTO** | [가이드](./03-application-layer/dto/command/command-dto-guide.md) | [ArchUnit](./03-application-layer/dto/dto-record-archunit.md) |
| **Query DTO** | [가이드](./03-application-layer/dto/query/query-dto-guide.md) | - |
| **Response DTO** | [가이드](./03-application-layer/dto/response/response-dto-guide.md) | - |
| **Bundle** | [가이드](./03-application-layer/dto/bundle/bundle-guide.md) | - |
| **Persist Bundle** | [가이드](./03-application-layer/dto/bundle/persist-bundle-guide.md) | - |
| **Query Bundle** | [가이드](./03-application-layer/dto/bundle/query-bundle-guide.md) | - |

**Event & Listener**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Transaction Event Registry** | [가이드](./03-application-layer/event/transaction-event-registry-guide.md) | [테스트](./03-application-layer/event/transaction-event-registry-test-guide.md) | [ArchUnit](./03-application-layer/event/transaction-event-registry-archunit.md) |
| **Event Listener** | [가이드](./03-application-layer/listener/event-listener-guide.md) | [테스트](./03-application-layer/listener/event-listener-test-guide.md) | [ArchUnit](./03-application-layer/listener/event-listener-archunit.md) |

**Facade & Factory**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Command Facade** | [가이드](./03-application-layer/facade/command/facade-guide.md) | [테스트](./03-application-layer/facade/facade-test-guide.md) | [ArchUnit](./03-application-layer/facade/facade-archunit.md) |
| **Query Facade** | [가이드](./03-application-layer/facade/query/query-facade-guide.md) | - | - |
| **Command Factory** | [가이드](./03-application-layer/factory/command/command-factory-guide.md) | [테스트](./03-application-layer/factory/command/command-factory-test-guide.md) | [ArchUnit](./03-application-layer/factory/command/command-factory-archunit.md) |
| **Query Factory** | [가이드](./03-application-layer/factory/query/query-factory-guide.md) | [테스트](./03-application-layer/factory/query/query-factory-test-guide.md) | [ArchUnit](./03-application-layer/factory/query/query-factory-archunit.md) |

**Manager**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Transaction Manager 요약** | [가이드](./03-application-layer/manager/transaction-manager-guide.md) | [테스트](./03-application-layer/manager/transaction-manager-test-guide.md) | [ArchUnit](./03-application-layer/manager/transaction-manager-archunit.md) |
| **Command Manager** | [가이드](./03-application-layer/manager/command/transaction-manager-guide.md) | - | - |
| **Read Manager** | [가이드](./03-application-layer/manager/query/read-manager-guide.md) | - | - |

**Port-In**:

| 컴포넌트 | 가이드 | ArchUnit |
|----------|--------|----------|
| **UseCase** | - | [ArchUnit](./03-application-layer/port/in/usecase-archunit.md) |
| **Command UseCase** | [가이드](./03-application-layer/port/in/command/port-in-command-guide.md) | - |
| **Query UseCase** | [가이드](./03-application-layer/port/in/query/port-in-query-guide.md) | - |

**Port-Out**:

| 컴포넌트 | 가이드 | ArchUnit |
|----------|--------|----------|
| **Command Port** | [가이드](./03-application-layer/port/out/command/port-out-command-guide.md) | [ArchUnit](./03-application-layer/port/out/command/port-out-command-archunit.md) |
| **Query Port** | [가이드](./03-application-layer/port/out/query/port-out-query-guide.md) | [ArchUnit](./03-application-layer/port/out/query/port-out-query-archunit.md) |
| **Cache Port** | [가이드](./03-application-layer/port/out/cache/cache-query-port-guide.md) | - |
| **Distributed Lock Port** | [가이드](./03-application-layer/port/out/common/distributed-lock-port-guide.md) | - |

**Service (UseCase 구현)**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Service 요약** | [가이드](./03-application-layer/service/service-guide.md) | - | - |
| **Command Service** | [가이드](./03-application-layer/service/command/command-service-guide.md) | [테스트](./03-application-layer/service/command/command-service-test-guide.md) | [ArchUnit](./03-application-layer/service/command/command-service-archunit.md) |
| **Query Service** | [가이드](./03-application-layer/service/query/query-service-guide.md) | [테스트](./03-application-layer/service/query/query-service-test-guide.md) | [ArchUnit](./03-application-layer/service/query/query-service-archunit.md) |

**Scheduler (배치 작업)**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Scheduler** | [가이드](./03-application-layer/scheduler/scheduler-guide.md) | [테스트](./03-application-layer/scheduler/scheduler-test-guide.md) | [ArchUnit](./03-application-layer/scheduler/scheduler-archunit.md) |

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
├── persistence-mysql-guide.md  # 요약 가이드
├── adapter/
│   ├── adapter-guide.md       # 어댑터 요약
│   ├── command/               # Command Adapter (저장)
│   └── query/
│       ├── general/           # 일반 Query Adapter
│       ├── admin/             # Admin Query Adapter
│       └── lock/              # Lock Query Adapter
├── config/                    # Flyway, HikariCP 설정
├── entity/                    # JPA Entity (Long FK 전략)
├── mapper/                    # Entity ↔ Domain 변환
└── repository/
    ├── repository-guide.md    # 리포지토리 요약
    ├── jpa/                   # JPA Repository (Command)
    ├── querydsl/              # QueryDSL Repository (Query)
    ├── admin/                 # Admin Repository
    └── lock/                  # Lock Repository
```

**상세 가이드**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **MySQL 요약** | [가이드](./04-persistence-layer/mysql/persistence-mysql-guide.md) | - | - |
| **Adapter 요약** | [가이드](./04-persistence-layer/mysql/adapter/adapter-guide.md) | - | - |
| **Command Adapter** | [가이드](./04-persistence-layer/mysql/adapter/command/command-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/command/command-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/command/command-adapter-archunit.md) |
| **Query Adapter** | [가이드](./04-persistence-layer/mysql/adapter/query/general/query-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/query/general/query-adapter-test-guide.md) / [통합](./04-persistence-layer/mysql/adapter/query/general/query-adapter-integration-testing.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/query/general/query-adapter-archunit.md) |
| **Admin Query Adapter** | [가이드](./04-persistence-layer/mysql/adapter/query/admin/admin-query-adapter-guide.md) | - | [ArchUnit](./04-persistence-layer/mysql/adapter/query/admin/admin-query-adapter-archunit.md) |
| **Lock Query Adapter** | [가이드](./04-persistence-layer/mysql/adapter/query/lock/lock-query-adapter-guide.md) | [테스트](./04-persistence-layer/mysql/adapter/query/lock/lock-query-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/adapter/query/lock/lock-query-adapter-archunit.md) |
| **Entity** | [가이드](./04-persistence-layer/mysql/entity/entity-guide.md) | [테스트](./04-persistence-layer/mysql/entity/entity-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/entity/entity-archunit.md) |
| **Mapper** | [가이드](./04-persistence-layer/mysql/mapper/mapper-guide.md) | [테스트](./04-persistence-layer/mysql/mapper/mapper-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/mapper/mapper-archunit.md) |
| **Repository 요약** | [가이드](./04-persistence-layer/mysql/repository/repository-guide.md) | - | - |
| **JPA Repository** | [가이드](./04-persistence-layer/mysql/repository/jpa/jpa-repository-guide.md) | - | [ArchUnit](./04-persistence-layer/mysql/repository/jpa/jpa-repository-archunit.md) |
| **QueryDSL Repository** | [가이드](./04-persistence-layer/mysql/repository/querydsl/querydsl-repository-guide.md) | [테스트](./04-persistence-layer/mysql/repository/querydsl/querydsl-repository-test-guide.md) | [ArchUnit](./04-persistence-layer/mysql/repository/querydsl/querydsl-repository-archunit.md) |
| **Admin Repository** | - | - | [ArchUnit](./04-persistence-layer/mysql/repository/admin/admin-querydsl-repository-archunit.md) |
| **Lock Repository** | [가이드](./04-persistence-layer/mysql/repository/lock/lock-repository-guide.md) | - | [ArchUnit](./04-persistence-layer/mysql/repository/lock/lock-repository-archunit.md) |

**Configuration**:

| 컴포넌트 | 가이드 |
|----------|--------|
| **Flyway** | [가이드](./04-persistence-layer/mysql/config/flyway-guide.md) |
| **HikariCP** | [가이드](./04-persistence-layer/mysql/config/hikaricp-configuration.md) |

#### 04-2. Redis (Cache & Lock)

**디렉토리 구조**:
```
04-persistence-layer/redis/
├── persistence-redis-guide.md  # 요약 가이드
├── adapter/                    # Cache Adapter
├── config/                     # Lettuce 설정
└── lock/                       # Distributed Lock
```

**상세 가이드**:

| 컴포넌트 | 가이드 | 테스트 | ArchUnit |
|----------|--------|--------|----------|
| **Redis 요약** | [가이드](./04-persistence-layer/redis/persistence-redis-guide.md) | - | - |
| **Cache Adapter** | [가이드](./04-persistence-layer/redis/adapter/cache-adapter-guide.md) | [테스트](./04-persistence-layer/redis/adapter/cache-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/redis/adapter/cache-adapter-archunit.md) |
| **Distributed Lock** | [가이드](./04-persistence-layer/redis/lock/distributed-lock-guide.md) | - | - |
| **Lock Adapter** | [가이드](./04-persistence-layer/redis/lock/lock-adapter-guide.md) | [테스트](./04-persistence-layer/redis/lock/lock-adapter-test-guide.md) | [ArchUnit](./04-persistence-layer/redis/lock/lock-adapter-archunit.md) |

**Configuration**:

| 컴포넌트 | 가이드 |
|----------|--------|
| **Cache 설정** | [가이드](./04-persistence-layer/redis/config/cache-configuration.md) |
| **Lettuce 설정** | [가이드](./04-persistence-layer/redis/config/lettuce-configuration.md) |

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
├── integration-testing/       # 통합 테스트 가이드
└── test-fixtures/             # Test Fixtures 가이드
```

**상세 가이드**:

| 컴포넌트 | 가이드 | ArchUnit |
|----------|--------|----------|
| **통합 테스트** | [가이드](./05-testing/integration-testing/01_integration-testing-overview.md) | - |
| **Test Fixtures** | [가이드](./05-testing/test-fixtures/01_test-fixtures-guide.md) | [ArchUnit](./05-testing/test-fixtures/02_test-fixtures-archunit.md) |

---

### 06. Observability

> **역할**: 모니터링, 로깅, 추적

**디렉토리 구조**:
```
06-observability/
├── observability-guide.md     # 요약 가이드
├── logging-configuration.md   # 로깅 설정
├── adot-integration.md        # AWS ADOT 연동
└── cloudwatch-integration.md  # CloudWatch 연동
```

**상세 가이드**:

| 컴포넌트 | 가이드 |
|----------|--------|
| **Observability 요약** | [가이드](./06-observability/observability-guide.md) |
| **Logging 설정** | [가이드](./06-observability/logging-configuration.md) |
| **ADOT 연동** | [가이드](./06-observability/adot-integration.md) |
| **CloudWatch 연동** | [가이드](./06-observability/cloudwatch-integration.md) |

---

### 07. Local Development

> **역할**: 로컬 개발 환경 설정

**상세 가이드**:

| 컴포넌트 | 가이드 |
|----------|--------|
| **로컬 개발 환경** | [가이드](./07-local-development/local-dev-guide.md) |

---

## 가이드 활용 방법

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

## 컨벤션 검증 체계

### 3-Tier 자동 검증 시스템

| Tier | 검증 시점 | 도구 | 목적 |
|------|----------|------|------|
| 1 | 코드 생성 직후 | validation-helper.py | 실시간 규칙 검증 |
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

## 체크리스트

코드 작성 시:
- [ ] Lombok 사용 여부 확인
- [ ] Law of Demeter 준수 여부
- [ ] Transaction 경계 준수 여부
- [ ] Javadoc 작성 여부 (public 클래스/메서드)
- [ ] ArchUnit 테스트 통과 여부
- [ ] Git pre-commit hook 통과 여부

---

## 문의 및 기여

- **버그 리포트**: GitHub Issues
- **기여 가이드**: CONTRIBUTING.md
- **질문 및 토론**: GitHub Discussions

---

- **작성자**: ryu-qqq
- **최종 수정일**: 2025-12-05
- **버전**: 2.0.0
