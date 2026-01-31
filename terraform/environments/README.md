# Gateway Terraform 환경별 구성

## 디렉토리 구조

```
environments/
├── prod/                    # 프로덕션 환경
│   ├── ecr/                 # ECR Repository
│   ├── ecs-cluster/         # ECS Cluster
│   ├── ecs-gateway/         # ECS Service (gateway)
│   ├── elasticache/         # Redis (전용)
│   └── cloudfront/          # CloudFront (prod + admin)
│
└── stage/                   # 스테이징 환경
    ├── ecr/                 # ECR Repository
    ├── ecs-cluster/         # ECS Cluster
    ├── ecs-gateway/         # ECS Service (gateway)
    └── cloudfront/          # CloudFront (stage only)
    # Note: elasticache 없음 - 공유 Redis 사용
```

## 환경별 차이점

| 항목 | Prod | Stage |
|------|------|-------|
| ECS CPU | 512 | 256 |
| ECS Memory | 1024 | 512 |
| Desired Count | 2 | 1 |
| AutoScaling Min | 2 | 1 |
| AutoScaling Max | 5 | 3 |
| Log Retention | 30일 | 14일 |
| Snapshot Retention | 1일 | 0 (비활성화) |
| Capacity Provider | FARGATE 100% | FARGATE_SPOT 70% |
| Redis | 전용 (gateway-redis-prod) | 공유 (/shared/stage/elasticache/*) |
| AuthHub URL | https://auth.set-of.com | https://stage-auth.set-of.com |
| Legacy API | *-prod.connectly.local | *-stage.connectly.local |

## CloudFront 구성

### Prod CloudFront
- **set-of.com, www.set-of.com**: Frontend + Gateway ALB (Prod)
- **admin.set-of.com**: Gateway ALB (Prod) - Admin API only

### Stage CloudFront
- **stage.set-of.com**: Frontend (Stage) + Gateway ALB (Stage)

## 배포 순서

### Prod 환경 (기존 state 호환)

```bash
# Prod는 기존 state를 그대로 사용
# environments/prod로 이동하여 terraform init 실행

cd environments/prod/ecr
terraform init
terraform plan

cd ../ecs-cluster
terraform init
terraform plan

cd ../elasticache
terraform init
terraform plan

cd ../ecs-gateway
terraform init
terraform plan

cd ../cloudfront
terraform init
terraform plan
```

### Stage 환경 최초 배포

```bash
# 1. ECR Repository 생성
cd environments/stage/ecr
terraform init
terraform apply

# 2. ECS Cluster 생성
cd ../ecs-cluster
terraform init
terraform apply

# 3. ECS Service 생성 (공유 Redis 사용)
cd ../ecs-gateway
terraform init
terraform apply

# 4. CloudFront 생성
cd ../cloudfront
terraform init
terraform apply
```

## 사전 요구사항 (Stage)

Stage 환경 배포 전 다음 AWS 리소스가 필요합니다:

### SSM Parameters
```bash
# 공유 Redis (이미 존재해야 함)
/shared/stage/elasticache/redis-endpoint
/shared/stage/elasticache/redis-port

# Sentry DSN (수동 생성 필요)
/gateway/stage/sentry/dsn
```

### Stage AuthHub
```bash
# Stage AuthHub가 배포되어 있어야 함
https://stage-auth.set-of.com
```

### Stage Legacy API (Cloud Map)
```bash
# Stage Legacy API가 Cloud Map에 등록되어 있어야 함
setof-commerce-legacy-api-stage.connectly.local:8088
setof-commerce-legacy-admin-stage.connectly.local:8089
```

## Backend State 경로

| 환경 | 모듈 | State Key |
|------|------|-----------|
| prod | ecr | `gateway/ecr/terraform.tfstate` |
| prod | ecs-cluster | `gateway/ecs-cluster/terraform.tfstate` |
| prod | ecs-gateway | `gateway/ecs-gateway/terraform.tfstate` |
| prod | elasticache | `gateway/elasticache/terraform.tfstate` |
| prod | cloudfront | `gateway/cloudfront/terraform.tfstate` |
| stage | ecr | `gateway/stage/ecr/terraform.tfstate` |
| stage | ecs-cluster | `gateway/stage/ecs-cluster/terraform.tfstate` |
| stage | ecs-gateway | `gateway/stage/ecs-gateway/terraform.tfstate` |
| stage | cloudfront | `gateway/stage/cloudfront/terraform.tfstate` |

## 주의사항

1. **Prod Backend Key**: Prod 환경은 기존 state 호환성을 위해 레거시 경로 유지
2. **Shared Resources**: VPC, Service Discovery Namespace는 `/shared/` 경로의 SSM 파라미터 공유
3. **Stage Redis**: 공유 Redis 사용 (`/shared/stage/elasticache/*`)
4. **CloudFront State 분리**: stage.set-of.com Route53 레코드 import 주의
5. **Stage Gateway ALB**: `gateway-alb-stage` 이름으로 생성됨

## 도메인 구성

| 도메인 | 환경 | 용도 |
|--------|------|------|
| set-of.com | prod | Frontend + API |
| www.set-of.com | prod | Frontend + API |
| admin.set-of.com | prod | Admin API |
| api.set-of.com | prod | Gateway ALB 직접 접근 |
| stage.set-of.com | stage | Frontend + API |
| stage-api.set-of.com | stage | Gateway ALB 직접 접근 |
