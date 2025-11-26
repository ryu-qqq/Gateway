# GATEWAY-002 테스트 커버리지 95% 달성 계획

## 1. 현황 분석

### 1.1 현재 테스트 커버리지 (2024-11-26 기준)

| 모듈 | Instructions | Branches | 테스트 현황 | 목표 Gap |
|------|-------------|----------|-------------|----------|
| **domain** | 69% | 68% | 218 passed | +26% |
| **authhub-client** | 61% | 50% | passed | +34% |
| **application** | 미측정 | 미측정 | 17 failed / 160 total | 테스트 수정 필요 |
| **adapter-in/gateway** | 미측정 | 미측정 | failed | 테스트 수정 필요 |
| **adapter-out/persistence-redis** | 미측정 | 미측정 | 6 failed / 125 total | 테스트 수정 필요 |
| **bootstrap-gateway** | 미측정 | 미측정 | 3 failed / 15 total | 테스트 수정 필요 |

### 1.2 Domain 모듈 상세 분석

| 패키지 | 현재 커버리지 | 목표 | 상태 |
|--------|-------------|------|------|
| `authorization.vo` | 100% | 95% | ✅ 달성 |
| `authorization.exception` | 100% | 95% | ✅ 달성 |
| `common.exception` | 93% | 95% | 🔶 +2% 필요 |
| `authentication.vo` | 44% | 95% | 🔴 +51% 필요 |
| `authentication.exception` | 0% | 95% | 🔴 +95% 필요 |

---

## 2. 테스트 실패 원인 분석

### 2.1 Application 모듈 (17개 실패)

#### JwtValidator 테스트 (2개 실패)
- **실패 테스트**:
  - `유효한 JWT에서 Claims 추출 성공`
  - `만료된 JWT에서도 Claims 추출 가능`
- **원인 분석**:
  - `Instant`와 `Date` 간 정밀도 차이 (나노초 vs 밀리초)
  - JWT 생성 시 `Date.from(Instant)`로 변환하면서 나노초 손실
  - 테스트에서 `isEqualTo(expiresAt)` 비교 시 실패
- **해결 방안**:
  ```java
  // 변경 전
  assertThat(claims.expiresAt()).isEqualTo(expiresAt);

  // 변경 후 - 밀리초 단위로 truncate
  assertThat(claims.expiresAt()).isEqualTo(expiresAt.truncatedTo(ChronoUnit.MILLIS));
  ```

#### GetPermissionHashService 테스트 (9개 실패)
- **실패 테스트**: Cache Hit, Hash 매칭, 권한 데이터 검증 관련
- **원인 분석**: Mock 설정 및 Reactive 스트림 검증 문제
- **해결 방안**: Fixture 및 Mock 설정 재검토

#### GetPermissionSpecService 테스트 (6개 실패)
- **실패 테스트**: Cache-aside 패턴, 엔드포인트 검증 관련
- **원인 분석**: Mock 설정 및 Reactive 스트림 검증 문제
- **해결 방안**: Fixture 및 Mock 설정 재검토

### 2.2 Persistence-Redis 모듈 (6개 실패)

#### Entity JSON 직렬화 테스트 (4개 실패)
- **실패 테스트**:
  - `PermissionHashEntity를 JSON으로 직렬화`
  - `빈 Set이 있는 PermissionHashEntity를 JSON으로 직렬화`
  - `PermissionSpecEntity를 JSON으로 직렬화`
  - `빈 권한 리스트가 있는 PermissionSpecEntity를 JSON으로 직렬화`
- **원인 분석**: Jackson ObjectMapper 설정 또는 Entity 구조 문제
- **해결 방안**: ObjectMapper 설정 확인 및 Entity 필드 매핑 검토

#### CommandAdapter 테스트 (2개 실패)
- **실패 테스트**: `매퍼에서 에러 발생 시 예외 전파`
- **원인 분석**: Mock 설정 문제
- **해결 방안**: Mock 설정 재검토

### 2.3 Gateway 모듈

- **원인 분석**: 의존성 문제 또는 Spring Context 로딩 실패
- **해결 방안**: 테스트 리포트 상세 확인 필요

### 2.4 Bootstrap 모듈 (3개 실패)

#### JwtAuthenticationIntegrationTest (3개 실패)
- **실패 테스트**:
  - `유효한 JWT로 인증 시 요청이 성공해야 한다`
  - `커스텀 subject로 JWT 인증이 성공해야 한다`
  - `POST /actuator/refresh-public-keys 호출 시 성공해야 한다`
- **원인 분석**: 통합 테스트 환경 설정 문제 (WireMock, TestContainers 등)
- **해결 방안**: 통합 테스트 환경 설정 검토

---

## 3. 테스트 보완 계획

### 3.1 Phase 1: 테스트 실패 수정 (우선순위: 높음)

#### Step 1: Domain 모듈 테스트 안정화 ✅ 완료
- [x] 불변성 테스트 수정 (`isNotSameAs` → mutable 컬렉션 사용)
- [x] null 값 테스트 수정 (NullPointerException 기대로 변경)

#### Step 2: Application 모듈 테스트 수정
```
우선순위: JwtValidator → GetPermissionHashService → GetPermissionSpecService
```

1. **JwtValidatorTest 수정**
   - Instant 정밀도 문제 해결
   - `truncatedTo(ChronoUnit.MILLIS)` 적용

2. **GetPermissionHashServiceTest 수정**
   - Mock 설정 재검토
   - Fixture 데이터 정합성 확인
   - Reactive 스트림 검증 로직 점검

3. **GetPermissionSpecServiceTest 수정**
   - Mock 설정 재검토
   - Cache-aside 패턴 테스트 로직 점검

#### Step 3: Persistence-Redis 모듈 테스트 수정
1. **Entity JSON 직렬화 테스트 수정**
   - ObjectMapper 설정 확인
   - Entity 필드 매핑 검토

2. **CommandAdapter 테스트 수정**
   - Mock 설정 재검토

#### Step 4: Gateway 모듈 테스트 수정
- 테스트 리포트 상세 확인 후 수정

#### Step 5: Bootstrap 통합 테스트 수정
- WireMock 설정 확인
- TestContainers 설정 확인
- Spring Context 로딩 문제 해결

### 3.2 Phase 2: 커버리지 95% 달성 (우선순위: 중간)

#### Domain 모듈 (+26% 필요)

##### authentication.exception 패키지 (현재 0%)
```
추가 필요 테스트:
- AuthenticationErrorCode 테스트
- InvalidTokenException 테스트
- TokenExpiredException 테스트
- TokenSignatureException 테스트
```

##### authentication.vo 패키지 (현재 44%)
```
추가 필요 테스트:
- AccessToken 추가 테스트 케이스
- JwtToken 추가 테스트 케이스
- JwtClaims 추가 테스트 케이스
- PublicKey 추가 테스트 케이스
```

##### common.exception 패키지 (현재 93%)
```
추가 필요 테스트:
- DomainException 엣지 케이스 테스트
```

#### Application 모듈

##### 테스트 실패 수정 후 커버리지 측정 필요
```
예상 추가 테스트:
- UseCase 엣지 케이스 테스트
- Service 엣지 케이스 테스트
- Component 엣지 케이스 테스트
```

#### Adapter 모듈들

##### authhub-client (현재 61%)
```
추가 필요 테스트:
- WebClient 에러 처리 테스트
- Retry 로직 테스트
- Circuit Breaker 테스트
```

##### persistence-redis
```
추가 필요 테스트:
- Redis 연결 실패 시나리오
- 직렬화/역직렬화 엣지 케이스
```

##### gateway
```
추가 필요 테스트:
- Filter 체이닝 테스트
- 에러 응답 처리 테스트
```

---

## 4. 작업 우선순위

### 즉시 수행 (P0)
1. Application 모듈 테스트 실패 수정
2. Persistence-Redis 모듈 테스트 실패 수정

### 단기 (P1) - 1주일 내
3. Gateway 모듈 테스트 실패 수정
4. Bootstrap 통합 테스트 수정
5. Domain authentication 패키지 커버리지 95% 달성

### 중기 (P2) - 2주일 내
6. Application 모듈 커버리지 95% 달성
7. Adapter 모듈들 커버리지 95% 달성

---

## 5. 성공 기준

### 테스트 통과율
- 모든 모듈 테스트 100% 통과

### 커버리지 목표
| 모듈 | Instructions 목표 | Branches 목표 |
|------|------------------|---------------|
| domain | ≥ 95% | ≥ 90% |
| application | ≥ 95% | ≥ 90% |
| adapter-in/gateway | ≥ 90% | ≥ 85% |
| adapter-out/persistence-redis | ≥ 90% | ≥ 85% |
| adapter-out/authhub-client | ≥ 90% | ≥ 85% |
| bootstrap-gateway | ≥ 80% | ≥ 75% |

### 품질 기준
- 모든 테스트는 독립적으로 실행 가능
- Flaky 테스트 0개
- 테스트 실행 시간 < 5분 (전체)

---

## 6. 참고 사항

### 테스트 작성 원칙 (프로젝트 컨벤션)
- TDD 사이클 준수: Red → Green → Refactor
- Tidy First: Structural 변경과 Behavioral 변경 분리
- 커밋 규칙: `test:`, `feat:`, `struct:` prefix 사용

### 테스트 유형별 가이드
- **Domain 테스트**: 순수 단위 테스트, Mock 최소화
- **Application 테스트**: UseCase 단위 테스트, Port Mock 사용
- **Adapter 테스트**: 통합 테스트, TestContainers/WireMock 활용
- **Bootstrap 테스트**: E2E 통합 테스트

---

**문서 작성일**: 2024-11-26
**작성자**: Claude Code
**상태**: 진행 중
