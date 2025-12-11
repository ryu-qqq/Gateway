# Terraform Infrastructure Guide

> **Version**: 2.0.0
> **Last Updated**: 2025-01-15
> **Spring Boot**: 3.5.x | **Java**: 21 | **Terraform**: >= 1.5.0

---

## 개요

이 문서는 Spring Hexagonal Template 프로젝트의 AWS 인프라를 Terraform으로 관리하는 방법을 설명합니다.

**핵심 원칙**:
- 모든 인프라는 **100% Terraform으로 관리** (AWS Console 수동 작업 금지)
- **Infrastructure 모듈 재사용** + **Wrapper Module로 컨벤션 강제**
- **SSM Parameter Store**로 Cross-Stack 참조

---

## 목차

1. [아키텍처 개요](#1-아키텍처-개요)
2. [디렉토리 구조](#2-디렉토리-구조)
3. [Wrapper Module 패턴](#3-wrapper-module-패턴)
4. [프로젝트 시작하기](#4-프로젝트-시작하기)
5. [각 모듈 사용법](#5-각-모듈-사용법)
6. [SSM Parameter 참조](#6-ssm-parameter-참조)
7. [GitHub Actions 연동](#7-github-actions-연동)
8. [Best Practices](#8-best-practices)

---

## 1. 아키텍처 개요

### 1.1 Wrapper Module 패턴

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Infrastructure Repository                            │
│            git::https://github.com/ryu-qqq/Infrastructure.git           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐           │
│  │   ECR   │  │ ECS Service │  │ ElastiCache │  │ S3 Bucket │  ...      │
│  │ Module  │  │   Module    │  │   Module    │  │  Module   │           │
│  └────┬────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘           │
│       │              │                │               │                  │
│       │    (재사용 모듈 - 유연한 설정 가능)              │                  │
│       │              │                │               │                  │
└───────┼──────────────┼────────────────┼───────────────┼──────────────────┘
        │              │                │               │
        ↓              ↓                ↓               ↓
┌───────┼──────────────┼────────────────┼───────────────┼──────────────────┐
│       │              │                │               │                  │
│  ┌────┴────┐  ┌──────┴──────┐  ┌──────┴──────┐  ┌─────┴─────┐           │
│  │   ECR   │  │ ECS Web API │  │ ElastiCache │  │    S3     │           │
│  │ Wrapper │  │   Wrapper   │  │   Wrapper   │  │  Wrapper  │           │
│  └────┬────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘           │
│       │              │                │               │                  │
│       │    🔒 컨벤션 강제 (보안, 네이밍, 태그)           │                  │
│       │              │                │               │                  │
│                Template Project (이 레포지토리)                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 개념

| 구성 요소 | 역할 |
|-----------|------|
| **Infrastructure Module** | 재사용 가능한 기본 모듈 (유연한 설정) |
| **Wrapper Module** | 컨벤션 강제 래퍼 (보안, 네이밍, 태그) |
| **Project Context** | 프로젝트 공통 설정 (이름, 팀, 환경) |
| **SSM Parameter** | Cross-Stack 참조 (VPC, Endpoint 등) |

### 1.3 강제되는 컨벤션

| 항목 | 컨벤션 | 강제 위치 |
|------|--------|-----------|
| **네이밍** | `{project}-{resource}-{env}` | Wrapper locals |
| **태그** | 8개 필수 태그 자동 적용 | Wrapper variables |
| **보안** | IMMUTABLE, 암호화, Private Subnet | Wrapper hardcoded |
| **백업** | 자동 스냅샷, Lifecycle Policy | Wrapper defaults |
| **모니터링** | CloudWatch 알람 기본 활성화 | Wrapper defaults |

---

## 2. 디렉토리 구조

```
terraform/
├── _shared/                        # 공통 설정 (참조용)
│   ├── project-context.tf          # 🔴 프로젝트 시작 시 수정 필요
│   ├── backend.tf                  # Backend 생성 가이드
│   └── shared-infra.tf             # SSM 참조 (VPC, ACM 등)
│
├── ecr/                            # ECR Wrapper Module
│   ├── main.tf                     # ECR 레포지토리 생성
│   ├── variables.tf                # 프로젝트 컨텍스트 변수
│   ├── outputs.tf                  # Repository URL, ARN
│   └── provider.tf                 # Backend 설정 (key 수정)
│
├── ecs-cluster/                    # ECS Cluster Wrapper Module
│   ├── main.tf                     # 클러스터 + Capacity Provider
│   ├── variables.tf
│   ├── outputs.tf
│   └── provider.tf
│
├── ecs-web-api/                    # ECS Web API Service Wrapper
│   ├── main.tf                     # Spring Boot 서비스 배포
│   ├── variables.tf
│   ├── outputs.tf
│   └── provider.tf
│
├── elasticache/                    # ElastiCache (Redis) Wrapper
│   ├── main.tf                     # Redis 클러스터
│   ├── variables.tf
│   ├── outputs.tf
│   └── provider.tf
│
├── s3/                             # S3 Bucket Wrapper
│   ├── main.tf                     # 파일 업로드 버킷
│   ├── variables.tf
│   ├── outputs.tf
│   └── provider.tf
│
└── README.md
```

---

## 3. Wrapper Module 패턴

### 3.1 Wrapper가 강제하는 것

```hcl
# terraform/ecr/main.tf

module "ecr_web_api" {
  source = "git::https://github.com/ryu-qqq/Infrastructure.git//terraform/modules/ecr?ref=${var.infrastructure_module_ref}"

  # 🔒 네이밍 컨벤션 강제 (변경 불가)
  name = local.naming.ecr_web_api

  # 🔒 보안 설정 (하드코딩 - 변경 금지)
  image_tag_mutability = "IMMUTABLE"    # 태그 변경 불가
  scan_on_push         = true            # 취약점 스캔 필수

  # 🔒 Lifecycle (컨벤션 기본값)
  enable_lifecycle_policy    = true
  max_image_count            = var.max_image_count  # 사용자 조정 가능
  untagged_image_expiry_days = 7

  # 🔒 필수 태그 (자동 주입)
  environment  = var.environment
  service_name = "${var.project_name}-web-api"
  team         = var.team
  owner        = var.owner
  cost_center  = var.cost_center
  project      = var.project_name
  data_class   = "confidential"
}
```

### 3.2 Wrapper 설계 원칙

| 구분 | 처리 방식 | 예시 |
|------|-----------|------|
| **보안 관련** | 하드코딩 (변경 불가) | `image_tag_mutability = "IMMUTABLE"` |
| **네이밍** | locals에서 계산 | `local.naming.ecr_web_api` |
| **태그** | variables에서 주입 | `var.team`, `var.owner` |
| **환경별 값** | 조건부 설정 | `var.environment == "prod" ? 14 : 7` |
| **옵션** | variables로 노출 | `var.max_image_count` |

---

## 4. 프로젝트 시작하기

### 4.1 사전 준비

1. **AWS CLI 설정**
```bash
aws configure
# AWS Access Key ID: xxx
# AWS Secret Access Key: xxx
# Default region name: ap-northeast-2
```

2. **Terraform 설치**
```bash
brew install terraform
terraform version  # >= 1.5.0 확인
```

### 4.2 Backend 생성 (최초 1회)

```bash
# S3 버킷 생성
aws s3api create-bucket \
  --bucket {your-project}-terraform-state \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2

# 버전관리 활성화
aws s3api put-bucket-versioning \
  --bucket {your-project}-terraform-state \
  --versioning-configuration Status=Enabled

# 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket {your-project}-terraform-state \
  --server-side-encryption-configuration '{
    "Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]
  }'

# DynamoDB Lock 테이블 생성
aws dynamodb create-table \
  --table-name {your-project}-terraform-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region ap-northeast-2
```

### 4.3 프로젝트 설정 수정

**1. 모든 `provider.tf` 파일의 backend 설정 수정:**

```hcl
# terraform/ecr/provider.tf (그리고 다른 모든 모듈)

terraform {
  backend "s3" {
    bucket         = "your-project-terraform-state"    # 🔴 변경
    key            = "ecr/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "your-project-terraform-lock"     # 🔴 변경
    encrypt        = true
  }
}
```

**2. tfvars 파일 생성 (각 모듈별):**

```hcl
# terraform/ecr/terraform.tfvars

project_name = "your-project"
environment  = "prod"
team         = "platform-team"
owner        = "team@example.com"
cost_center  = "engineering"
```

### 4.4 배포 순서

```bash
# 1. ECR (이미지 저장소)
cd terraform/ecr
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars

# 2. ECS Cluster
cd ../ecs-cluster
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars

# 3. ElastiCache (선택)
cd ../elasticache
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars

# 4. S3 (선택)
cd ../s3
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars

# 5. ECS Web API (이미지 빌드 후)
cd ../ecs-web-api
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

---

## 5. 각 모듈 사용법

### 5.1 ECR (Container Registry)

**용도**: Docker 이미지 저장소

**생성되는 리소스**:
- ECR Repository (web-api)
- ECR Repository (scheduler) - 선택
- Lifecycle Policy
- SSM Parameter (repository URL)

**주요 변수**:
```hcl
variable "max_image_count" {
  description = "보관할 최대 이미지 수"
  default     = 30
}

variable "enable_scheduler" {
  description = "Scheduler ECR 레포지토리 생성 여부"
  default     = false
}
```

**실행**:
```bash
cd terraform/ecr
terraform apply -var-file=terraform.tfvars
```

### 5.2 ECS Cluster

**용도**: ECS Fargate 클러스터

**생성되는 리소스**:
- ECS Cluster
- Capacity Provider (Fargate + Fargate Spot)
- SSM Parameter (cluster ARN, name)

**주요 변수**:
```hcl
variable "enable_container_insights" {
  description = "Container Insights 활성화"
  default     = true
}

variable "enable_fargate_spot" {
  description = "Fargate Spot 활성화 (비용 최적화)"
  default     = true
}
```

### 5.3 ECS Web API

**용도**: Spring Boot 서비스 배포

**강제 컨벤션**:
- Private Subnet 배치 (Public IP 금지)
- Circuit Breaker 활성화
- 자동 롤백 활성화
- ECS Exec은 dev 환경만 허용

**주요 변수**:
```hcl
variable "cpu" {
  description = "CPU 유닛"
  default     = 512
}

variable "memory" {
  description = "메모리 (MiB)"
  default     = 1024
}

variable "image_tag" {
  description = "Docker 이미지 태그"
  default     = "latest"
}

variable "container_environment" {
  description = "환경 변수 목록"
  default     = []
}

variable "container_secrets" {
  description = "Secrets Manager에서 주입할 시크릿"
  default     = []
}
```

**Secrets 사용 예시**:
```hcl
container_secrets = [
  {
    name      = "DB_PASSWORD"
    valueFrom = "arn:aws:secretsmanager:ap-northeast-2:123456789:secret:db-password"
  }
]
```

### 5.4 ElastiCache (Redis)

**용도**: Redis 캐시

**강제 컨벤션**:
- 암호화 필수 (at-rest, in-transit)
- Private Subnet 배치
- 자동 스냅샷 (dev: 7일, prod: 14일)

**주요 변수**:
```hcl
variable "redis_version" {
  description = "Redis 버전"
  default     = "7.0"
}

variable "node_type" {
  description = "노드 인스턴스 타입"
  default     = "cache.t3.micro"
}

variable "enable_replication" {
  description = "복제 그룹 활성화"
  default     = false
}

variable "auth_token" {
  description = "Redis AUTH 토큰"
  default     = null
  sensitive   = true
}
```

### 5.5 S3 (File Storage)

**용도**: 파일 업로드 저장소

**강제 컨벤션**:
- Public Access 완전 차단
- 버전관리 활성화
- Lifecycle: 90일 IA, 180일 Glacier (prod)

**주요 변수**:
```hcl
variable "lifecycle_rules" {
  description = "Lifecycle 규칙 (null이면 기본 규칙)"
  default     = null
}

variable "cors_rules" {
  description = "CORS 규칙"
  default     = []
}
```

---

## 6. SSM Parameter 참조

### 6.1 생성되는 SSM Parameters

각 모듈은 Cross-Stack 참조를 위해 SSM Parameter를 생성합니다:

| 모듈 | Parameter Path | 값 |
|------|----------------|-----|
| ECR | `/{project}/ecr/{name}/repository-url` | ECR Repository URL |
| ECS Cluster | `/{project}/ecs/cluster-arn` | Cluster ARN |
| ECS Cluster | `/{project}/ecs/cluster-name` | Cluster Name |
| ECS Web API | `/{project}/ecs/web-api/service-name` | Service Name |
| ElastiCache | `/{project}/elasticache/redis-endpoint` | Redis Endpoint |
| ElastiCache | `/{project}/elasticache/redis-port` | Redis Port |
| S3 | `/{project}/s3/uploads-bucket-name` | Bucket Name |
| S3 | `/{project}/s3/uploads-bucket-arn` | Bucket ARN |

### 6.2 Shared Infrastructure 참조

Infrastructure 레포에서 관리하는 공유 리소스는 다음 경로로 참조합니다:

```hcl
# terraform/_shared/shared-infra.tf

data "aws_ssm_parameter" "vpc_id" {
  name = "/shared/network/vpc-id"
}

data "aws_ssm_parameter" "private_subnets" {
  name = "/shared/network/private-subnets"
}

data "aws_ssm_parameter" "certificate_arn" {
  name = "/shared/network/certificate-arn"
}
```

### 6.3 애플리케이션에서 참조

Spring Boot에서 SSM Parameter 참조:

```yaml
# application.yml
spring:
  config:
    import: "aws-parameterstore:/${project-name}/"

# 또는 ECS Task Definition에서 직접 주입
container_environment = [
  {
    name  = "REDIS_HOST"
    value = data.aws_ssm_parameter.redis_endpoint.value
  }
]
```

---

## 7. GitHub Actions 연동

### 7.1 Terraform Plan (PR)

```yaml
# .github/workflows/terraform-plan.yml
name: Terraform Plan

on:
  pull_request:
    branches: [main]
    paths:
      - 'terraform/**'

permissions:
  contents: read
  pull-requests: write
  id-token: write

jobs:
  terraform-plan:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [ecr, ecs-cluster, ecs-web-api, elasticache, s3]

    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.5.0

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ap-northeast-2

      - name: Terraform Init
        working-directory: terraform/${{ matrix.module }}
        run: terraform init

      - name: Terraform Plan
        working-directory: terraform/${{ matrix.module }}
        run: terraform plan -var-file=terraform.tfvars -no-color
```

### 7.2 Terraform Apply (Main)

```yaml
# .github/workflows/terraform-apply.yml
name: Terraform Apply

on:
  push:
    branches: [main]
    paths:
      - 'terraform/**'

permissions:
  contents: read
  id-token: write

jobs:
  terraform-apply:
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ap-northeast-2

      # 순서대로 Apply (의존성 고려)
      - name: Apply ECR
        working-directory: terraform/ecr
        run: |
          terraform init
          terraform apply -var-file=terraform.tfvars -auto-approve

      - name: Apply ECS Cluster
        working-directory: terraform/ecs-cluster
        run: |
          terraform init
          terraform apply -var-file=terraform.tfvars -auto-approve
```

### 7.3 AWS OIDC 설정

```hcl
# GitHub Actions용 OIDC Provider
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# IAM Role
resource "aws_iam_role" "github_actions" {
  name = "github-actions-terraform"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:your-org/your-repo:*"
        }
      }
    }]
  })
}
```

---

## 8. Best Practices

### 8.1 필수 규칙

| 규칙 | 설명 |
|------|------|
| **State 암호화** | S3 Backend + `encrypt = true` 필수 |
| **State Lock** | DynamoDB 테이블로 동시 실행 방지 |
| **버전 고정** | `infrastructure_module_ref`로 모듈 버전 고정 |
| **OIDC 인증** | IAM User 금지, GitHub OIDC 사용 |
| **환경 분리** | dev/staging/prod 각각 tfvars 파일 관리 |

### 8.2 네이밍 컨벤션

```
{project}-{resource-type}-{environment}

예시:
- my-project-web-api-prod
- my-project-cluster-prod
- my-project-redis-prod
- my-project-uploads-prod
```

### 8.3 태그 컨벤션

모든 리소스에 자동 적용되는 필수 태그:

| 태그 | 용도 | 예시 |
|------|------|------|
| `Environment` | 환경 구분 | dev, staging, prod |
| `Team` | 담당 팀 | platform-team |
| `Owner` | 소유자 | team@example.com |
| `CostCenter` | 비용 센터 | engineering |
| `Project` | 프로젝트명 | my-project |
| `ManagedBy` | 관리 도구 | terraform |
| `ServiceName` | 서비스명 | my-project-web-api |
| `DataClass` | 데이터 분류 | confidential |

### 8.4 보안 체크리스트

- [x] State 파일 S3 암호화 (`encrypt = true`)
- [x] State 파일 버전관리 활성화
- [x] DynamoDB Lock 테이블 설정
- [x] GitHub Actions OIDC 사용
- [x] ECR IMMUTABLE 태그
- [x] ECR 취약점 스캔 활성화
- [x] ElastiCache 암호화 (at-rest, in-transit)
- [x] S3 Public Access 차단
- [x] ECS Private Subnet 배치
- [x] Secrets Manager 사용 (환경변수 하드코딩 금지)

### 8.5 문제 해결

**Q: terraform init 실패**
```bash
# Backend가 존재하지 않는 경우
# 4.2 Backend 생성 섹션 참조하여 S3/DynamoDB 생성
```

**Q: 모듈 업데이트가 반영되지 않음**
```bash
# 캐시 삭제 후 재초기화
rm -rf .terraform
terraform init -upgrade
```

**Q: State Lock 충돌**
```bash
# Lock 강제 해제 (주의!)
terraform force-unlock LOCK_ID
```

---

## 참고 자료

- [Infrastructure Repository](https://github.com/ryu-qqq/Infrastructure)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [GitHub Actions OIDC](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
