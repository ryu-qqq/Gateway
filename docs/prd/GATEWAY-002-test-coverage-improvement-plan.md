# GATEWAY-002: 테스트 커버리지 95% 달성 계획

## 1. 현황 분석

### 1.1 현재 테스트 커버리지 현황 (2025-11-26)

| 모듈 | Instructions | Branches | 목표 대비 | 상태 |
|------|-------------|----------|-----------|------|
| **Domain** | 96% | 95% | 달성 | ✅ |
| **Application** | 65% | 44% | -30% / -51% | ❌ |
| **Gateway** | 88% | 84% | -7% / -11% | ⚠️ |
| **Persistence-Redis** | 44% | 60% | -51% / -35% | ❌ |
| **AuthHub-Client** | 61% | 50% | -34% / -45% | ❌ |
| **Bootstrap-Gateway** | 79% | 37% | -16% / -58% | ❌ |

**목표**: Instructions 95%, Branches 95%

---

## 2. 모듈별 상세 분석 및 작업 계획

### 2.1 Application 모듈 (65% → 95%)

**현재 상태**: 728 Instructions 커버, 387 미커버

#### 패키지별 커버리지 상세

| 패키지 | Instructions | Branches | 우선순위 |
|--------|-------------|----------|----------|
| `authentication.service.command` | 0% | 0% | 🔴 HIGH |
| `authentication.service.query` | 0% | 0% | 🔴 HIGH |
| `authentication.assembler` | 0% | 0% | 🔴 HIGH |
| `authentication.dto.command` | 0% | 0% | 🟡 MEDIUM |
| `authentication.dto.query` | 0% | 0% | 🟡 MEDIUM |
| `authentication.dto.response` | 0% | n/a | 🟡 MEDIUM |
| `common.dto.response` | 0% | 0% | 🟡 MEDIUM |
| `authentication.component` | 99% | 83% | ✅ OK |
| `authorization.service.command` | 100% | 100% | ✅ OK |
| `authorization.service.query` | 100% | 100% | ✅ OK |
| `authorization.dto.command` | 100% | 100% | ✅ OK |
| `authorization.dto.response` | 100% | n/a | ✅ OK |

#### 작업 목록

```
[ ] 2.1.1 ValidateJwtService 테스트 작성 (authentication.service.command)
    - ValidateJwtService.execute() 성공 케이스
    - ValidateJwtService.execute() 실패 케이스 (만료, 무효 서명 등)
    - 예상 커버리지 증가: +124 instructions

[ ] 2.1.2 RefreshPublicKeyService 테스트 작성 (authentication.service.query)
    - RefreshPublicKeyService.execute() 성공 케이스
    - RefreshPublicKeyService.execute() 캐시 갱신 케이스
    - 예상 커버리지 증가: +78 instructions

[ ] 2.1.3 JwtClaimsAssembler 테스트 작성 (authentication.assembler)
    - JwtClaimsAssembler.toDomain() 변환 테스트
    - null/empty 입력 처리 테스트
    - 예상 커버리지 증가: +38 instructions

[ ] 2.1.4 Authentication DTO 테스트 작성
    - ValidateJwtCommand record 테스트
    - PublicKeyQuery record 테스트
    - ValidateJwtResponse record 테스트
    - 예상 커버리지 증가: +47 instructions

[ ] 2.1.5 Common DTO 테스트 작성
    - SliceResponse 테스트
    - PageResponse 테스트
    - 예상 커버리지 증가: +99 instructions
```

---

### 2.2 Gateway 모듈 (88% → 95%)

**현재 상태**: 667 Instructions 커버, 89 미커버

#### 패키지별 커버리지 상세

| 패키지 | Instructions | Branches | 우선순위 |
|--------|-------------|----------|----------|
| `config` | 0% | n/a | 🔴 HIGH |
| `common.dto` | 59% | 50% | 🟡 MEDIUM |
| `error` | 88% | 100% | ⚠️ LOW |
| `filter` | 92% | 100% | ✅ OK |
| `controller` | 100% | n/a | ✅ OK |

#### 작업 목록

```
[ ] 2.2.1 GatewayFilterOrder 테스트 작성 (config)
    - 상수 값 검증 테스트
    - 예상 커버리지 증가: +7 instructions

[ ] 2.2.2 GatewayErrorResponse DTO 테스트 보완 (common.dto)
    - 모든 생성자/팩토리 메서드 테스트
    - equals/hashCode 테스트
    - 예상 커버리지 증가: +37 instructions

[ ] 2.2.3 GatewayErrorHandler 테스트 보완 (error)
    - 추가 에러 시나리오 테스트
    - 예상 커버리지 증가: +11 instructions
```

---

### 2.3 Persistence-Redis 모듈 (44% → 95%)

**현재 상태**: 419 Instructions 커버, 530 미커버

#### 패키지별 커버리지 상세

| 패키지 | Instructions | Branches | 우선순위 |
|--------|-------------|----------|----------|
| `config` | 0% | n/a | 🔴 HIGH |
| `repository` | 3% | n/a | 🔴 HIGH |
| `adapter` | 69% | n/a | 🟡 MEDIUM |
| `entity` | 71% | 100% | ⚠️ LOW |
| `mapper` | 74% | 0% | 🟡 MEDIUM |

#### 작업 목록

```
[ ] 2.3.1 RedisConfig 테스트 작성 (config)
    - RedisConnectionFactory 빈 생성 테스트
    - RedisTemplate 빈 생성 테스트
    - 예상 커버리지 증가: +167 instructions

[ ] 2.3.2 Redis Repository 테스트 작성 (repository)
    - PublicKeyRedisRepository CRUD 테스트
    - PermissionSpecRedisRepository CRUD 테스트
    - PermissionHashRedisRepository CRUD 테스트
    - 예상 커버리지 증가: +192 instructions

[ ] 2.3.3 Redis Adapter 테스트 보완 (adapter)
    - 미커버 메서드 테스트 추가
    - 에러 케이스 테스트
    - 예상 커버리지 증가: +67 instructions

[ ] 2.3.4 Redis Mapper 브랜치 커버리지 (mapper)
    - null 입력 처리 테스트
    - 빈 컬렉션 처리 테스트
    - 예상 커버리지 증가: +43 instructions (4 branches)

[ ] 2.3.5 Redis Entity 테스트 보완 (entity)
    - 미커버 생성자/메서드 테스트
    - 예상 커버리지 증가: +53 instructions
```

---

### 2.4 AuthHub-Client 모듈 (61% → 95%)

**현재 상태**: 349 Instructions 커버, 220 미커버

#### 패키지별 커버리지 상세

| 패키지 | Instructions | Branches | 우선순위 |
|--------|-------------|----------|----------|
| `client` | 61% | 50% | 🔴 HIGH |

#### 작업 목록

```
[ ] 2.4.1 AuthHubPublicKeyAdapter 테스트 보완
    - fetchPublicKeys() 성공 케이스
    - fetchPublicKeys() 에러 케이스 (네트워크 오류, 타임아웃)
    - 예상 커버리지 증가: +80 instructions

[ ] 2.4.2 AuthHubPermissionAdapter 테스트 보완
    - fetchPermissionSpec() 성공 케이스
    - fetchPermissionSpec() 에러 케이스
    - 예상 커버리지 증가: +70 instructions

[ ] 2.4.3 AuthHubClient WebClient 테스트
    - HTTP 요청/응답 처리 테스트
    - 재시도 로직 테스트
    - 예상 커버리지 증가: +70 instructions

[ ] 2.4.4 브랜치 커버리지 보완
    - 조건문 분기 테스트 추가
    - 예상 커버리지 증가: +8 branches
```

---

### 2.5 Bootstrap-Gateway 모듈 (79% → 95%)

**현재 상태**: Integration 테스트 위주, 단위 테스트 부족

#### 작업 목록

```
[ ] 2.5.1 Configuration 클래스 테스트
    - GatewayApplication 컨텍스트 로드 테스트
    - Bean 설정 테스트
    - 예상 커버리지 증가: +16%

[ ] 2.5.2 브랜치 커버리지 보완
    - 조건부 설정 테스트
    - 프로파일별 설정 테스트
    - 예상 커버리지 증가: +58% branches
```

---

## 3. 작업 우선순위

### Phase 1: 핵심 비즈니스 로직 (예상 소요: 2시간)
1. Application - authentication.service.command 테스트
2. Application - authentication.service.query 테스트
3. Application - authentication.assembler 테스트

### Phase 2: 인프라 계층 (예상 소요: 2시간)
4. Persistence-Redis - repository 테스트
5. Persistence-Redis - config 테스트
6. AuthHub-Client - adapter 테스트 보완

### Phase 3: 어댑터 계층 (예상 소요: 1시간)
7. Gateway - config 테스트
8. Gateway - common.dto 테스트 보완
9. Application - DTO 테스트

### Phase 4: 마무리 (예상 소요: 30분)
10. Persistence-Redis - mapper 브랜치 커버리지
11. Bootstrap-Gateway - 설정 테스트
12. 전체 커버리지 검증

---

## 4. 테스트 작성 가이드라인

### 4.1 테스트 네이밍 규칙
```
{테스트대상}Test.java          // 단위 테스트
{테스트대상}IntegrationTest.java  // 통합 테스트
```

### 4.2 테스트 구조
```java
@Test
@DisplayName("한글로 테스트 설명")
void shouldDoSomethingWhenCondition() {
    // given
    // when
    // then
}
```

### 4.3 Reactive 테스트
```java
StepVerifier.create(mono)
    .expectNext(expectedValue)
    .verifyComplete();
```

### 4.4 Mock 사용 원칙
- 외부 의존성만 Mock
- 도메인 로직은 실제 객체 사용
- Testcontainers로 Redis 통합 테스트

---

## 5. 완료 기준

- [ ] 모든 모듈 Instructions 커버리지 95% 이상
- [ ] 모든 모듈 Branches 커버리지 95% 이상
- [ ] 모든 테스트 통과 (0 failures)
- [ ] `./gradlew test jacocoTestReport` 성공

---

## 6. 참고 사항

### 6.1 JaCoCo 커버리지 제외 항목
- `*Config.java` (Spring Configuration 클래스)
- `*Application.java` (메인 클래스)
- Record의 자동 생성 메서드 (equals, hashCode, toString)

### 6.2 커버리지 측정 명령어
```bash
# 전체 테스트 및 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 특정 모듈만 테스트
./gradlew :application:test :application:jacocoTestReport

# 커버리지 검증 (임계값 체크)
./gradlew jacocoTestCoverageVerification
```

---

**작성일**: 2025-11-26
**작성자**: Claude Code Assistant
**버전**: 1.0
