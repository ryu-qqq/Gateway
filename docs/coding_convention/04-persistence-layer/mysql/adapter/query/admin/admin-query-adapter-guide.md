# AdminQueryAdapter 가이드

> **목적**: 관리자 조회 전용 CQRS Query Adapter 구현 가이드 (1:1 매핑)

---

## 1️⃣ AdminQueryAdapter란?

### 역할
**관리자 Query 요청 → AdminQueryDslRepository → DTO Projection → 반환**

관리자 전용 복잡 조회를 담당하며, AdminQueryDslRepository에 1:1 매핑되어 위임합니다.

### 일반 QueryAdapter와 차이점

| 항목 | QueryAdapter | AdminQueryAdapter |
|------|-------------|-------------------|
| **Repository** | QueryDslRepository | AdminQueryDslRepository |
| **Join** | ❌ 금지 | ✅ 허용 |
| **메서드 제한** | 4개 고정 | ✅ 자유 |
| **반환 타입** | Domain | DTO Projection |
| **Mapper** | 필수 (Entity→Domain) | 선택적 (DTO 변환 시) |

### 책임
- ✅ 관리자 복잡 조회만 담당
- ✅ AdminQueryDslRepository 1:1 매핑
- ✅ DTO Projection 직접 반환 (또는 Mapper로 변환)
- ❌ **비즈니스 로직 금지**
- ❌ **저장/수정/삭제 금지** (CommandAdapter로)
- ❌ **일반 조회 금지** (QueryAdapter로)

### 핵심 원칙
```
Application Layer (UseCase/Facade)
  └─ Admin Query Port 호출
      └─ AdminQueryAdapter (1:1 매핑)
            └─ AdminQueryDslRepository
                  └─ DTO Projection (Join 허용)
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: 1:1 매핑
- **AdminQueryAdapter ↔ AdminQueryDslRepository**: 정확히 1:1 매핑
- **필드 1~2개**: AdminQueryDslRepository + (선택적) ResponseMapper

### 원칙 2: DTO Projection 직접 반환
```java
// ✅ DTO Projection 직접 반환 (Mapper 불필요)
@Component
public class OrderAdminQueryAdapter implements OrderAdminQueryPort {

    private final OrderAdminQueryDslRepository adminQueryDslRepository;

    @Override
    public List<AdminOrderListResponse> findOrderListWithMember(AdminOrderSearchCriteria criteria) {
        // DTO Projection 직접 반환
        return adminQueryDslRepository.findOrderListWithMember(criteria);
    }
}
```

### 원칙 3: 필요 시 Response 변환
```java
// ✅ Response 변환이 필요한 경우 Mapper 사용
@Component
public class OrderAdminQueryAdapter implements OrderAdminQueryPort {

    private final OrderAdminQueryDslRepository adminQueryDslRepository;
    private final AdminOrderResponseMapper responseMapper;  // 선택적

    @Override
    public AdminOrderDetailResponse findOrderDetail(OrderId orderId) {
        AdminOrderProjection projection = adminQueryDslRepository.findOrderDetail(orderId.getValue());
        return responseMapper.toResponse(projection);  // DTO → Response 변환
    }
}
```

### 원칙 4: 비즈니스 로직 금지
```java
// ❌ 비즈니스 로직 금지
public List<AdminOrderListResponse> findOrderList(AdminOrderSearchCriteria criteria) {
    List<AdminOrderListResponse> list = adminQueryDslRepository.findOrderList(criteria);

    // 금지! 비즈니스 로직
    if (criteria.getStatus() == OrderStatus.PENDING) {
        list = list.stream()
            .filter(o -> o.getAmount().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }
    return list;
}

// ✅ 단순 위임만
public List<AdminOrderListResponse> findOrderList(AdminOrderSearchCriteria criteria) {
    return adminQueryDslRepository.findOrderList(criteria);
}
```

---

## 3️⃣ 템플릿 코드

### Port 인터페이스
```java
public interface {Bc}AdminQueryPort {

    // 관리자 목록 조회 (Join 허용)
    List<Admin{Bc}ListResponse> findList(Admin{Bc}SearchCriteria criteria);

    // 관리자 상세 조회 (Join 허용)
    Optional<Admin{Bc}DetailResponse> findDetail({Bc}Id id);

    // 관리자 통계 조회
    Admin{Bc}StatsResponse getStats(Admin{Bc}StatsQuery query);

    // 관리자 페이징 조회
    Page<Admin{Bc}ListResponse> findPage(Admin{Bc}SearchCriteria criteria, Pageable pageable);
}
```

### Adapter 구현 (DTO 직접 반환)
```java
@Component
public class {Bc}AdminQueryAdapter implements {Bc}AdminQueryPort {

    private final {Bc}AdminQueryDslRepository adminQueryDslRepository;

    public {Bc}AdminQueryAdapter({Bc}AdminQueryDslRepository adminQueryDslRepository) {
        this.adminQueryDslRepository = adminQueryDslRepository;
    }

    /**
     * 관리자 목록 조회
     *
     * @param criteria 검색 조건
     * @return 관리자 목록 Response (DTO Projection)
     */
    @Override
    public List<Admin{Bc}ListResponse> findList(Admin{Bc}SearchCriteria criteria) {
        return adminQueryDslRepository.findList(criteria);
    }

    /**
     * 관리자 상세 조회
     *
     * @param id Bc ID
     * @return 관리자 상세 Response (DTO Projection)
     */
    @Override
    public Optional<Admin{Bc}DetailResponse> findDetail({Bc}Id id) {
        return adminQueryDslRepository.findDetail(id.getValue());
    }

    /**
     * 관리자 통계 조회
     *
     * @param query 통계 쿼리
     * @return 통계 Response
     */
    @Override
    public Admin{Bc}StatsResponse getStats(Admin{Bc}StatsQuery query) {
        return adminQueryDslRepository.getStats(query);
    }

    /**
     * 관리자 페이징 조회
     *
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 목록
     */
    @Override
    public Page<Admin{Bc}ListResponse> findPage(Admin{Bc}SearchCriteria criteria, Pageable pageable) {
        return adminQueryDslRepository.findPage(criteria, pageable);
    }
}
```

### Adapter 구현 (Response 변환 필요 시)
```java
@Component
public class {Bc}AdminQueryAdapter implements {Bc}AdminQueryPort {

    private final {Bc}AdminQueryDslRepository adminQueryDslRepository;
    private final Admin{Bc}ResponseMapper responseMapper;

    public {Bc}AdminQueryAdapter(
        {Bc}AdminQueryDslRepository adminQueryDslRepository,
        Admin{Bc}ResponseMapper responseMapper
    ) {
        this.adminQueryDslRepository = adminQueryDslRepository;
        this.responseMapper = responseMapper;
    }

    @Override
    public List<Admin{Bc}ListResponse> findList(Admin{Bc}SearchCriteria criteria) {
        List<Admin{Bc}Projection> projections = adminQueryDslRepository.findList(criteria);
        return projections.stream()
            .map(responseMapper::toResponse)
            .toList();
    }
}
```

---

## 4️⃣ 체크리스트

AdminQueryAdapter 구현 시:

### 구조
- [ ] `@Component` 어노테이션 추가
- [ ] AdminQueryPort 인터페이스 구현
- [ ] AdminQueryDslRepository 의존성 주입 (1:1 매핑)
- [ ] **필드 1~2개만** (AdminQueryDslRepository + 선택적 Mapper)

### 반환 타입
- [ ] DTO Projection 직접 반환 (Domain 아님!)
- [ ] 필요 시 ResponseMapper로 변환

### 금지 사항
- [ ] 비즈니스 로직 없음 (단순 위임만)
- [ ] Command 메서드 없음 (저장/수정/삭제 금지)
- [ ] 일반 조회 메서드 없음 (QueryAdapter 사용)
- [ ] 다른 Repository 주입 금지

---

## 5️⃣ 참고 문서

- [admin-querydsl-repository-archunit.md](../../../repository/admin/admin-querydsl-repository-archunit.md) - AdminQueryDslRepository ArchUnit
- [query-adapter-guide.md](../general/query-adapter-guide.md) - 일반 QueryAdapter 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
