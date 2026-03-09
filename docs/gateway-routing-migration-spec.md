# Gateway 라우팅 마이그레이션 명세서

> 작성일: 2026-02-22
> 목적: 외부 OMS(사방넷, 셀릭)가 호출하는 세토프 어드민 엔드포인트를 MarketPlace로 단계적 전환

---

## 현재 라우팅

```
admin-server.set-of.net/** → setof-commerce-legacy-admin-prod:8089
```

## 변경 요청

`admin-server.set-of.net` 호스트로 들어오는 요청 중 아래 경로만 MarketPlace로 리라우팅.
나머지는 기존 레거시 어드민으로 계속 전달.

```
admin-server.set-of.net + (아래 경로) → marketplace-web-api-prod:9090
admin-server.set-of.net + (그 외)     → setof-commerce-legacy-admin-prod:8089 (기존 유지)
```

---

## 라우팅 규칙

### 경로 리라이트 규칙

OMS는 `/api/v1/...`으로 호출하지만, MarketPlace 레거시 컨트롤러는 `/api/v1/legacy/...`로 매핑되어 있음.
**게이트웨이에서 path prefix 리라이트 필요:**

```
/api/v1/{path} → /api/v1/legacy/{path}
```

예시:
```
OMS 호출:      POST /api/v1/auth/authentication
게이트웨이 전달: POST /api/v1/legacy/auth/authentication → marketplace-web-api-prod:9090
```

---

### 1. Auth (인증)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| POST | `/api/v1/auth/authentication` | `/api/v1/legacy/auth/authentication` |

### 2. Seller (셀러 조회)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| GET | `/api/v1/seller` | `/api/v1/legacy/seller` |

### 3. Product (상품)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| POST | `/api/v1/product/group` | `/api/v1/legacy/product/group` |
| GET | `/api/v1/product/group/{id}` | `/api/v1/legacy/product/group/{id}` |
| PUT | `/api/v1/product/group/{id}` | `/api/v1/legacy/product/group/{id}` |
| PUT | `/api/v1/product/group/{id}/notice` | `/api/v1/legacy/product/group/{id}/notice` |
| PUT | `/api/v1/product/group/{id}/images` | `/api/v1/legacy/product/group/{id}/images` |
| PUT | `/api/v1/product/group/{id}/detailDescription` | `/api/v1/legacy/product/group/{id}/detailDescription` |
| PUT | `/api/v1/product/group/{id}/option` | `/api/v1/legacy/product/group/{id}/option` |
| PATCH | `/api/v1/product/group/{id}/price` | `/api/v1/legacy/product/group/{id}/price` |
| PATCH | `/api/v1/product/group/{id}/display-yn` | `/api/v1/legacy/product/group/{id}/display-yn` |
| PATCH | `/api/v1/product/group/{id}/out-stock` | `/api/v1/legacy/product/group/{id}/out-stock` |
| PATCH | `/api/v1/product/group/{id}/stock` | `/api/v1/legacy/product/group/{id}/stock` |

### 4. Order (주문)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| GET | `/api/v1/orders` | `/api/v1/legacy/orders` |
| GET | `/api/v1/order/{id}` | `/api/v1/legacy/order/{id}` |
| PUT | `/api/v1/order` | `/api/v1/legacy/order` |

### 5. QnA (문의)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| GET | `/api/v1/qnas` | `/api/v1/legacy/qnas` |

### 6. Shipment (배송)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| GET | `/api/v1/shipment/company-codes` | `/api/v1/legacy/shipment/company-codes` |

### 7. Image (이미지)

| Method | 원본 경로 (OMS 호출) | 리라이트 경로 (MarketPlace) |
|--------|---------------------|---------------------------|
| POST | `/api/v1/image/presigned` | `/api/v1/legacy/image/presigned` |

---

## 전체 요약

| 도메인 | 엔드포인트 수 | Path Prefix 패턴 |
|--------|-------------|-----------------|
| Auth | 1 | `/api/v1/auth/**` |
| Seller | 1 | `/api/v1/seller` |
| Product | 11 | `/api/v1/product/group/**` |
| Order | 3 | `/api/v1/order/**`, `/api/v1/orders` |
| QnA | 1 | `/api/v1/qnas` |
| Shipment | 1 | `/api/v1/shipment/**` |
| Image | 1 | `/api/v1/image/presigned` |
| **합계** | **19** | |

---

## 구현 옵션

### 옵션 A: 경로별 개별 매칭

각 경로를 개별 라우트로 등록. 정밀하지만 라우트 수가 많음(19개).

### 옵션 B: Path Prefix 그룹 매칭 + 리라이트

```yaml
# 호스트: admin-server.set-of.net
# 대상: marketplace-web-api-prod:9090
# 리라이트: /api/v1/{path} → /api/v1/legacy/{path}

routes:
  - host: admin-server.set-of.net
    paths:
      - /api/v1/auth/authentication    # POST
      - /api/v1/seller                 # GET
      - /api/v1/product/group/**       # POST, GET, PUT, PATCH
      - /api/v1/order/**               # GET, PUT
      - /api/v1/orders                 # GET
      - /api/v1/qnas                   # GET
      - /api/v1/shipment/**            # GET
      - /api/v1/image/presigned         # POST
    uri: http://marketplace-web-api-prod:9090
    rewrite-path-pattern: /api/v1/(?<remaining>.*)
    rewrite-path-replacement: /api/v1/legacy/${remaining}
```

7개의 path prefix로 19개 엔드포인트를 커버. 단, `/api/v1/product/group/**` 패턴은 OMS가 호출하지 않는 경로도 매칭될 수 있으나 MarketPlace에 해당 엔드포인트가 없으면 404 반환되므로 안전.

### 옵션 C: 전체 전환 (최종 목표)

모든 `admin-server.set-of.net` 트래픽을 MarketPlace로 전환. 레거시 어드민 서비스 완전 폐기 시점에 적용.

---

## 전환 순서 (권장)

| 단계 | 대상 | 사유 |
|------|------|------|
| 1단계 | Product (11개) | 구현 완료, 가장 트래픽 많음 |
| 2단계 | Auth + Seller (2개) | 인증 흐름, 전환 필수 선행 |
| 3단계 | Order + QnA + Shipment + Image (6개) | 나머지 OMS 호출 엔드포인트 |

> **주의**: Auth + Seller를 먼저 전환하지 않으면 Product만 MarketPlace로 가도 인증 실패할 수 있음.
> OMS 호출 흐름: `POST /auth/authentication` → `GET /seller` → 실제 API 호출.
> 따라서 **Auth/Seller와 Product를 동시에 전환**하거나, **Auth/Seller를 먼저** 전환해야 함.

---

## 영향 받는 클라이언트

| 클라이언트 | IP | 호출 엔드포인트 |
|-----------|-----|---------------|
| 사방넷 OMS | `27.102.150.130` | Auth, Seller, Product 전체, Order 전체, QnA, Shipment |
| 셀릭 OMS | `112.175.152.87`, `119.205.195.140`, `121.140.112.177`, `121.140.112.202` | Auth, Seller, Product 일부, Order 일부 |

---

## 롤백 계획

게이트웨이 라우팅 규칙은 `GatewayRoutingConfig`에서 부팅 시점에 고정 등록되며, 런타임 토글이 없습니다.
따라서 롤백 시 **YAML 설정 변경 후 게이트웨이 재배포가 필요**합니다.

**롤백 절차:**
1. `application-{env}.yml`에서 marketplace-oms-legacy 라우트를 제거 또는 비활성화
2. 게이트웨이 재배포 (ECS 롤링 업데이트)
3. 레거시 어드민으로 트래픽 복구 확인

MarketPlace 레거시 컨트롤러는 세토프 어드민과 동일한 요청/응답 스펙을 유지하므로 클라이언트 변경 불필요.
