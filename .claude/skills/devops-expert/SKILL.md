---
name: devops-expert
version: 3.0.0
description: |
  GitHub Actions CI/CD, Terraform 인프라, Docker Compose 로컬 환경, AWS SSM 포트포워딩.
  시크릿 하드코딩 금지, 환경 분리 필수. /kb-devops 명령 시 자동 활성화.
author: claude-spring-standards
created: 2024-11-01
updated: 2025-12-05
tags: [project, devops, ci-cd, github-actions, terraform, docker, aws]
---

# DevOps Expert (DevOps 전문가)

## 목적 (Purpose)

**CI/CD 파이프라인**, **인프라 코드(IaC)**, **로컬 개발 환경**을 프로젝트 컨벤션에 맞게 구성합니다.
GitHub Actions, Terraform, Docker Compose를 활용한 자동화 시스템을 설계하고 구현합니다.

## 활성화 조건

- `/kb-devops` 명령 실행 시
- CI/CD 파이프라인 설정 시
- 인프라 코드(Terraform) 작업 시
- github actions, terraform, docker compose, ci/cd, 로컬 환경 키워드 언급 시

## 산출물 (Output)

| 컴포넌트 | 파일명 패턴 | 위치 |
|----------|-------------|------|
| CI 워크플로우 | `ci.yml` | `.github/workflows/` |
| CD 워크플로우 | `build-and-deploy.yml` | `.github/workflows/` |
| Terraform Plan | `terraform-plan.yml` | `.github/workflows/` |
| Terraform Apply | `terraform-apply.yml` | `.github/workflows/` |
| Dockerfile | `Dockerfile` | `bootstrap/bootstrap-web-api/` |
| Docker Compose | `docker-compose.local.yml` | `local-dev/` |
| Terraform Module | `main.tf`, `variables.tf`, `outputs.tf` | `terraform/{module}/` |

## 완료 기준 (Acceptance Criteria)

- [ ] GitHub Actions CI 워크플로우 구성 완료
- [ ] QA 도구 검증 포함 (Checkstyle, SpotBugs, PMD, Spotless)
- [ ] JaCoCo 커버리지 검증 포함
- [ ] 시크릿 하드코딩 금지 준수
- [ ] 환경 분리 (local/test/prod) 적용
- [ ] Docker Multi-stage 빌드 적용
- [ ] Terraform Wrapper Module 패턴 적용
- [ ] AWS OIDC 인증 사용 (IAM User 금지)

---

## CI/CD 성숙도 모델

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CI/CD Maturity Model                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Phase 1          Phase 2          Phase 3           Phase 4            │
│  ─────────        ─────────        ─────────         ─────────          │
│  기본 CI          CD 추가          IaC 통합          엔터프라이즈        │
│                                                                          │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐      ┌─────────┐          │
│  │ Test    │     │ Build   │     │Terraform│      │Reusable │          │
│  │ Build   │ ──▶ │ Push    │ ──▶ │ Plan    │ ──▶  │Workflows│          │
│  │ Quality │     │ Deploy  │     │ Apply   │      │Multi-Env│          │
│  └─────────┘     └─────────┘     └─────────┘      └─────────┘          │
│                                                                          │
│  현재 템플릿     단일 서비스      인프라 자동화     대규모 서비스        │
│  (시작점)        배포            (AWS 통합)        (마이크로서비스)      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 코드 템플릿

### 1. CI 워크플로우 (Phase 1)

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

permissions:
  contents: read
  checks: write
  pull-requests: write

jobs:
  # ==========================================================================
  # 테스트 및 품질 검증
  # ==========================================================================
  test:
    name: Test & Quality Check
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      # ----------------------------------------------------------------------
      # 코드 품질 검증 (QA Tools)
      # ----------------------------------------------------------------------
      - name: Run Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest --no-daemon
        continue-on-error: false

      - name: Run SpotBugs
        run: ./gradlew spotbugsMain --no-daemon
        continue-on-error: false

      - name: Run PMD (Law of Demeter)
        run: ./gradlew pmdMain --no-daemon
        continue-on-error: false

      - name: Check Code Formatting (Spotless)
        run: ./gradlew spotlessCheck --no-daemon
        continue-on-error: false

      # ----------------------------------------------------------------------
      # Lombok 금지 검증
      # ----------------------------------------------------------------------
      - name: Verify No Lombok
        run: ./gradlew checkNoLombok --no-daemon

      # ----------------------------------------------------------------------
      # Version Catalog 일관성 검증
      # ----------------------------------------------------------------------
      - name: Verify Version Catalog
        run: ./gradlew verifyVersionCatalog --no-daemon

      # ----------------------------------------------------------------------
      # 테스트 실행 (단위 + ArchUnit)
      # ----------------------------------------------------------------------
      - name: Run Tests
        run: ./gradlew test --no-daemon

      # ----------------------------------------------------------------------
      # JaCoCo 커버리지 검증
      # ----------------------------------------------------------------------
      - name: Verify Code Coverage
        run: ./gradlew jacocoTestCoverageVerification --no-daemon

      # ----------------------------------------------------------------------
      # 테스트 리포트 업로드
      # ----------------------------------------------------------------------
      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: |
            **/build/reports/tests/
            **/build/reports/jacoco/
            **/build/reports/checkstyle/
            **/build/reports/spotbugs/
            **/build/reports/pmd/
          retention-days: 7

      # ----------------------------------------------------------------------
      # PR에 커버리지 코멘트
      # ----------------------------------------------------------------------
      - name: Add Coverage Comment to PR
        uses: madrapps/jacoco-report@v1.6.1
        if: github.event_name == 'pull_request'
        with:
          paths: |
            ${{ github.workspace }}/**/build/reports/jacoco/test/jacocoTestReport.xml
          token: ${{ secrets.GITHUB_TOKEN }}
          min-coverage-overall: 70
          min-coverage-changed-files: 80
          title: "📊 Code Coverage Report"

  # ==========================================================================
  # 빌드 검증 (JAR 생성 가능 여부)
  # ==========================================================================
  build:
    name: Build Verification
    needs: test
    runs-on: ubuntu-latest
    timeout-minutes: 10

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build JAR
        run: |
          chmod +x gradlew
          ./gradlew :bootstrap:bootstrap-web-api:bootJar --no-daemon -x test

      - name: Upload JAR Artifact
        uses: actions/upload-artifact@v4
        with:
          name: application-jar
          path: bootstrap/bootstrap-web-api/build/libs/*.jar
          retention-days: 1
```

### 2. CD 워크플로우 (Phase 2)

```yaml
# .github/workflows/build-and-deploy.yml
name: Build and Deploy

on:
  workflow_dispatch:  # 수동 트리거
  push:
    branches: [main]
    paths-ignore:
      - 'terraform/**'
      - '**.md'
      - 'docs/**'

permissions:
  contents: read
  id-token: write  # OIDC 인증용

env:
  AWS_REGION: ap-northeast-2
  ECR_REPOSITORY: spring-hexagonal-template-prod
  ECS_CLUSTER: my-cluster-prod
  ECS_SERVICE: my-service-prod

jobs:
  test:
    name: Run Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Run tests
        run: |
          chmod +x gradlew
          ./gradlew clean test --no-daemon

  build:
    name: Build & Push Docker Image
    needs: test
    runs-on: ubuntu-latest
    timeout-minutes: 20
    outputs:
      image-uri: ${{ steps.build-image.outputs.image-uri }}
      image-tag: ${{ steps.build-image.outputs.image-tag }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      # 🔒 OIDC 인증 (IAM User 금지)
      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}
          role-duration-seconds: 3600

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag, and push image to ECR
        id: build-image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: web-api-${{ github.run_number }}-${{ github.sha }}
        run: |
          docker build \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            -f bootstrap/bootstrap-web-api/Dockerfile \
            .

          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

          echo "image-uri=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT
          echo "image-tag=$IMAGE_TAG" >> $GITHUB_OUTPUT

  deploy:
    name: Deploy to ECS
    needs: build
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}
          role-duration-seconds: 3600

      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster ${{ env.ECS_CLUSTER }} \
            --service ${{ env.ECS_SERVICE }} \
            --force-new-deployment

          aws ecs wait services-stable \
            --cluster ${{ env.ECS_CLUSTER }} \
            --services ${{ env.ECS_SERVICE }}

          echo "✅ Deployment completed successfully!"
```

### 3. Dockerfile (Multi-stage)

```dockerfile
# bootstrap/bootstrap-web-api/Dockerfile
# ============================================================================
# Multi-stage build for optimized image size
# ============================================================================

# ----------------------------------------
# Stage 1: Build
# ----------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Gradle 파일 복사 (캐시 레이어)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY gradle/libs.versions.toml gradle/

# 모듈별 build.gradle 복사
COPY domain/build.gradle domain/
COPY application/build.gradle application/
COPY adapter-in/rest-api/build.gradle adapter-in/rest-api/
COPY adapter-out/persistence-mysql/build.gradle adapter-out/persistence-mysql/
COPY adapter-out/persistence-redis/build.gradle adapter-out/persistence-redis/
COPY bootstrap/bootstrap-web-api/build.gradle bootstrap/bootstrap-web-api/

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY domain/src domain/src
COPY application/src application/src
COPY adapter-in/rest-api/src adapter-in/rest-api/src
COPY adapter-out/persistence-mysql/src adapter-out/persistence-mysql/src
COPY adapter-out/persistence-redis/src adapter-out/persistence-redis/src
COPY bootstrap/bootstrap-web-api/src bootstrap/bootstrap-web-api/src

# 빌드
RUN ./gradlew :bootstrap:bootstrap-web-api:bootJar --no-daemon -x test

# ----------------------------------------
# Stage 2: Runtime
# ----------------------------------------
FROM eclipse-temurin:21-jre-alpine

# 헬스체크용 curl 설치
RUN apk add --no-cache curl

# 🔒 보안: non-root 사용자 생성
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# JAR 복사
COPY --from=builder /workspace/bootstrap/bootstrap-web-api/build/libs/*.jar app.jar

# 소유권 설정
RUN chown -R app:app /app

# non-root 사용자로 전환
USER app

# JVM 컨테이너 최적화 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 4. Docker Compose (로컬 환경)

```yaml
# local-dev/docker-compose.local.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: template-mysql
    ports:
      - "13306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: template
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - template-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: template-redis
    ports:
      - "16379:6379"
    networks:
      - template-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  phpmyadmin:
    image: phpmyadmin:latest
    container_name: template-phpmyadmin
    ports:
      - "18080:80"
    environment:
      PMA_HOST: mysql
      PMA_USER: root
      PMA_PASSWORD: root
    networks:
      - template-network
    depends_on:
      mysql:
        condition: service_healthy

  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: template-redis-commander
    ports:
      - "18081:8081"
    environment:
      REDIS_HOSTS: local:redis:6379
      HTTP_USER: admin
      HTTP_PASSWORD: admin
    networks:
      - template-network
    depends_on:
      redis:
        condition: service_healthy

volumes:
  mysql-data:

networks:
  template-network:
    driver: bridge
```

### 5. Terraform Wrapper Module (ECR 예시)

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
  max_image_count            = var.max_image_count
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

# SSM Parameter로 Cross-Stack 참조
resource "aws_ssm_parameter" "ecr_web_api_url" {
  name  = "/${var.project_name}/ecr/web-api/repository-url"
  type  = "String"
  value = module.ecr_web_api.repository_url
}

locals {
  naming = {
    ecr_web_api = "${var.project_name}-web-api-${var.environment}"
  }
}
```

### 6. Terraform Variables

```hcl
# terraform/ecr/variables.tf

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "environment" {
  description = "환경 (dev/staging/prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod"
  }
}

variable "team" {
  description = "담당 팀"
  type        = string
}

variable "owner" {
  description = "소유자 이메일"
  type        = string
}

variable "cost_center" {
  description = "비용 센터"
  type        = string
}

variable "infrastructure_module_ref" {
  description = "Infrastructure 모듈 버전"
  type        = string
  default     = "v1.0.0"
}

variable "max_image_count" {
  description = "보관할 최대 이미지 수"
  type        = number
  default     = 30
}
```

### 7. Terraform Provider (Backend)

```hcl
# terraform/ecr/provider.tf

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "your-project-terraform-state"  # 🔴 프로젝트별 수정
    key            = "ecr/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "your-project-terraform-lock"   # 🔴 프로젝트별 수정
    encrypt        = true
  }
}

provider "aws" {
  region = "ap-northeast-2"
}
```

### 8. Terraform Plan 워크플로우 (Phase 3)

```yaml
# .github/workflows/terraform-plan.yml
name: Terraform Plan

on:
  pull_request:
    branches: [main]
    paths:
      - 'terraform/**'
  workflow_dispatch:

permissions:
  contents: read
  pull-requests: write
  id-token: write

jobs:
  terraform-plan:
    name: Terraform Plan
    runs-on: ubuntu-latest
    timeout-minutes: 15
    strategy:
      matrix:
        module:
          - name: ecr
            dir: terraform/ecr
          - name: ecs-web-api
            dir: terraform/ecs-web-api

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.0

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ap-northeast-2
          role-duration-seconds: 3600

      - name: Terraform Format Check
        working-directory: ${{ matrix.module.dir }}
        run: terraform fmt -check -recursive

      - name: Terraform Init
        working-directory: ${{ matrix.module.dir }}
        run: terraform init

      - name: Terraform Validate
        working-directory: ${{ matrix.module.dir }}
        run: terraform validate

      - name: Terraform Plan
        id: plan
        working-directory: ${{ matrix.module.dir }}
        run: |
          terraform plan -no-color -out=tfplan
          terraform show -no-color tfplan > plan-${{ matrix.module.name }}.txt
        continue-on-error: true

      - name: Upload Plan Artifact
        uses: actions/upload-artifact@v4
        with:
          name: plan-${{ matrix.module.name }}
          path: ${{ matrix.module.dir }}/plan-${{ matrix.module.name }}.txt
          retention-days: 5
```

### 9. AWS SSM 포트 포워딩 스크립트

```bash
#!/bin/bash
# local-dev/scripts/aws-port-forward.sh

# 환경 변수 로드
source .env.aws

echo "🔧 Starting AWS SSM Port Forwarding..."

# RDS 포트 포워딩 (13307 → RDS:3306)
aws ssm start-session \
  --target $AWS_BASTION_INSTANCE_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{
    \"host\":[\"$AWS_RDS_ENDPOINT\"],
    \"portNumber\":[\"3306\"],
    \"localPortNumber\":[\"13307\"]
  }" &

# ElastiCache 포트 포워딩 (16380 → Redis:6379)
aws ssm start-session \
  --target $AWS_BASTION_INSTANCE_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{
    \"host\":[\"$AWS_REDIS_ENDPOINT\"],
    \"portNumber\":[\"6379\"],
    \"localPortNumber\":[\"16380\"]
  }" &

echo "✅ Port forwarding started!"
echo "   MySQL: localhost:13307"
echo "   Redis: localhost:16380"
echo ""
echo "Press Ctrl+C to stop..."

wait
```

---

## 디렉토리 구조

```
project/
├── .github/
│   └── workflows/
│       ├── ci.yml                    ← Phase 1: 기본 CI
│       ├── build-and-deploy.yml      ← Phase 2: CD
│       ├── terraform-plan.yml        ← Phase 3: IaC Plan
│       └── terraform-apply.yml       ← Phase 3: IaC Apply
│
├── bootstrap/
│   └── bootstrap-web-api/
│       └── Dockerfile                ← Multi-stage 빌드
│
├── local-dev/
│   ├── docker-compose.local.yml      ← 로컬 환경
│   ├── docker-compose.aws.yml        ← AWS 연결용
│   ├── scripts/
│   │   ├── start.sh                  ← 로컬 시작
│   │   ├── stop.sh                   ← 로컬 종료
│   │   ├── aws-port-forward.sh       ← AWS SSM 터널
│   │   └── aws-start.sh              ← AWS 연결 시작
│   ├── .env.local                    ← 로컬 환경변수 (커밋 금지)
│   └── .env.aws                      ← AWS 환경변수 (커밋 금지)
│
└── terraform/
    ├── _shared/                      ← 공통 설정
    │   └── project-context.tf
    ├── ecr/                          ← ECR Wrapper
    │   ├── main.tf
    │   ├── variables.tf
    │   ├── outputs.tf
    │   └── provider.tf
    ├── ecs-cluster/                  ← ECS Cluster Wrapper
    ├── ecs-web-api/                  ← ECS Service Wrapper
    ├── elasticache/                  ← Redis Wrapper
    └── s3/                           ← S3 Wrapper
```

---

## 서비스 접속 정보

### 로컬 환경

| 서비스 | URL / 포트 | 인증 정보 |
|--------|-----------|-----------|
| Web API | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| phpMyAdmin | http://localhost:18080 | 자동 로그인 |
| Redis Commander | http://localhost:18081 | admin / admin |
| MySQL | localhost:13306 | root / root |
| Redis | localhost:16379 | - |

### AWS 연결 환경

| 서비스 | URL / 포트 | 비고 |
|--------|-----------|------|
| MySQL (RDS) | localhost:13307 | SSM 터널 |
| Redis (Cache) | localhost:16380 | SSM 터널 |

---

## Zero-Tolerance 규칙

### ✅ MANDATORY (필수)

| 규칙 | 설명 |
|------|------|
| GitHub Actions OIDC | IAM User Access Key 금지, OIDC 인증 사용 |
| 시크릿 관리 | GitHub Secrets, AWS Secrets Manager 사용 |
| Docker Multi-stage | 빌드 의존성 분리, 이미지 크기 최소화 |
| Non-root 실행 | 컨테이너는 non-root 사용자로 실행 |
| ECR IMMUTABLE | 이미지 태그 변경 불가 설정 |
| Terraform Backend | S3 + DynamoDB Lock 필수 |
| 환경 분리 | local/test/prod 프로파일 분리 |
| 네이밍 컨벤션 | `{project}-{resource}-{env}` 형식 |
| 필수 태그 | Environment, Team, Owner, CostCenter, Project, ManagedBy |

### ❌ PROHIBITED (금지)

| 항목 | 이유 |
|------|------|
| 시크릿 하드코딩 | 보안 위험, 노출 시 피해 심각 |
| IAM User Access Key | 장기 자격 증명 보안 위험 |
| `.env` 파일 커밋 | 환경 변수 노출 위험 |
| 프로덕션 직접 접근 | SSM 터널링 사용 필수 |
| root 사용자 컨테이너 | 보안 취약점 |
| ECR MUTABLE 태그 | 이미지 위변조 가능 |
| AWS Console 수동 작업 | IaC로 100% 관리 |
| Terraform Apply 수동 실행 | CI/CD 파이프라인으로 실행 |

---

## Phase별 체크리스트

### Phase 1: 기본 CI ✅

- [ ] `.github/workflows/ci.yml` 생성
- [ ] PR 시 자동 테스트 확인
- [ ] QA 도구 검증 통과 (Checkstyle, SpotBugs, PMD, Spotless)
- [ ] JaCoCo 커버리지 기준 충족 (70%/80%/90%)
- [ ] ArchUnit 테스트 통과
- [ ] Lombok 금지 검증 통과
- [ ] Version Catalog 일관성 검증 통과

### Phase 2: CD 추가

- [ ] AWS OIDC Provider 설정
- [ ] ECR Repository 생성
- [ ] ECS Cluster/Service 생성
- [ ] `AWS_ROLE_ARN` Secret 등록
- [ ] `build-and-deploy.yml` 구성
- [ ] Dockerfile Multi-stage 빌드 적용
- [ ] (선택) Slack 알림 연동

### Phase 3: IaC 통합

- [ ] Terraform Backend 설정 (S3 + DynamoDB)
- [ ] `terraform/` 디렉토리 구조 생성
- [ ] Wrapper Module 패턴 적용
- [ ] `terraform-plan.yml` 구성
- [ ] `terraform-apply.yml` 구성
- [ ] PR 기반 Plan 검토 프로세스 확립

### Phase 4: 엔터프라이즈

- [ ] 중앙 Infrastructure 레포 생성
- [ ] Reusable Workflows 구현
- [ ] 다중 bootstrap 모듈 확장
- [ ] 환경별 배포 전략 (dev → staging → prod)

---

## Terraform 강제 컨벤션

### Wrapper Module이 강제하는 것

| 구분 | 처리 방식 | 예시 |
|------|-----------|------|
| **보안 관련** | 하드코딩 (변경 불가) | `image_tag_mutability = "IMMUTABLE"` |
| **네이밍** | locals에서 계산 | `local.naming.ecr_web_api` |
| **태그** | variables에서 주입 | `var.team`, `var.owner` |
| **환경별 값** | 조건부 설정 | `var.environment == "prod" ? 14 : 7` |
| **옵션** | variables로 노출 | `var.max_image_count` |

### SSM Parameter 생성 규칙

| 모듈 | Parameter Path | 값 |
|------|----------------|-----|
| ECR | `/{project}/ecr/{name}/repository-url` | ECR Repository URL |
| ECS Cluster | `/{project}/ecs/cluster-arn` | Cluster ARN |
| ECS Web API | `/{project}/ecs/web-api/service-name` | Service Name |
| ElastiCache | `/{project}/elasticache/redis-endpoint` | Redis Endpoint |
| S3 | `/{project}/s3/uploads-bucket-name` | Bucket Name |

---

## 보안 체크리스트

### CI/CD 보안

- [ ] GitHub Actions OIDC 사용 (Access Key 금지)
- [ ] 최소 권한 IAM Role 설정
- [ ] Secrets Manager로 민감 정보 관리
- [ ] Docker 이미지 취약점 스캔

### Terraform 보안

- [ ] State 파일 S3 암호화 (`encrypt = true`)
- [ ] State 파일 버전관리 활성화
- [ ] DynamoDB Lock 테이블 설정
- [ ] ECR IMMUTABLE 태그 강제

### 인프라 보안

- [ ] ElastiCache 암호화 (at-rest, in-transit)
- [ ] S3 Public Access 차단
- [ ] ECS Private Subnet 배치
- [ ] non-root 컨테이너 실행

---

## 참조 문서

- **GitHub Workflows Guide**: `docs/coding_convention/00-project-setup/github-workflows-guide.md`
- **Terraform Guide**: `docs/coding_convention/00-project-setup/terraform-guide.md`
- **Local Dev Guide**: `docs/coding_convention/07-local-development/local-dev-guide.md`
- **Gradle Configuration**: `docs/coding_convention/00-project-setup/gradle-configuration.md`
