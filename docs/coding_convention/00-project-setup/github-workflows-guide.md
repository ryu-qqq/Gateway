# GitHub Workflows Guide — **CI/CD 파이프라인 구성 가이드**

> **목적**: GitHub Actions 기반 CI/CD 파이프라인 구성 및 단계별 확장 전략

---

## 1️⃣ 개요

### CI/CD 성숙도 모델

이 문서는 템플릿 프로젝트의 CI/CD를 **4단계**로 나누어 점진적으로 구축하는 방법을 설명합니다.

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

### 현재 템플릿 구조

```
spring-hexagonal-template/
├── .github/
│   └── workflows/
│       ├── ci.yml                    ← Phase 1 (현재)
│       ├── build-and-deploy.yml      ← Phase 2 (예시)
│       ├── terraform-plan.yml        ← Phase 3 (예시)
│       └── terraform-apply.yml       ← Phase 3 (예시)
│
├── bootstrap/
│   └── bootstrap-web-api/
│       └── Dockerfile                ← 컨테이너 빌드용
│
└── terraform/                        ← Phase 3에서 추가
    ├── ecr/
    ├── ecs-web-api/
    ├── s3/
    └── ...
```

---

## 2️⃣ Phase 1: 기본 CI (현재 템플릿)

### 목표
- PR/Push 시 자동 테스트
- 코드 품질 검증 (QA 도구)
- ArchUnit 아키텍처 검증

### `.github/workflows/ci.yml`

```yaml
# ============================================================================
# Spring Hexagonal Template - CI Pipeline
# ============================================================================
# Phase 1: 기본 CI (테스트 + 품질 검증)
# ============================================================================

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

### Phase 1 체크리스트

- [ ] `.github/workflows/ci.yml` 생성
- [ ] PR 시 자동 테스트 확인
- [ ] QA 도구 검증 통과 (Checkstyle, SpotBugs, PMD, Spotless)
- [ ] JaCoCo 커버리지 기준 충족
- [ ] ArchUnit 테스트 통과
- [ ] Lombok 금지 검증 통과

---

## 3️⃣ Phase 2: CD 추가 (단일 서비스 배포)

### 목표
- Docker 이미지 빌드 & ECR Push
- ECS/EKS 배포 자동화
- 환경별 배포 (dev/staging/prod)

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 2: CD Pipeline                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  main push                                                       │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────┐     ┌─────────────┐     ┌─────────────┐           │
│  │  Test   │ ──▶ │ Build Docker│ ──▶ │ Deploy ECS  │           │
│  │         │     │ Push ECR    │     │             │           │
│  └─────────┘     └─────────────┘     └─────────────┘           │
│                                              │                   │
│                                              ▼                   │
│                                       ┌─────────────┐           │
│                                       │ Slack 알림  │           │
│                                       └─────────────┘           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### `.github/workflows/build-and-deploy.yml` (단일 서비스)

```yaml
# ============================================================================
# Spring Hexagonal Template - Build & Deploy Pipeline
# ============================================================================
# Phase 2: 단일 서비스 CD (Docker Build → ECR Push → ECS Deploy)
# ============================================================================

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
  # ==========================================================================
  # 테스트
  # ==========================================================================
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

  # ==========================================================================
  # Docker 빌드 & ECR Push
  # ==========================================================================
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
          # Docker 빌드
          docker build \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            -f bootstrap/bootstrap-web-api/Dockerfile \
            .

          # ECR Push
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

          # Output 설정
          echo "image-uri=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT
          echo "image-tag=$IMAGE_TAG" >> $GITHUB_OUTPUT

  # ==========================================================================
  # ECS 배포
  # ==========================================================================
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
          # ECS 서비스 업데이트 (새 이미지로)
          aws ecs update-service \
            --cluster ${{ env.ECS_CLUSTER }} \
            --service ${{ env.ECS_SERVICE }} \
            --force-new-deployment

          # 배포 완료 대기
          aws ecs wait services-stable \
            --cluster ${{ env.ECS_CLUSTER }} \
            --services ${{ env.ECS_SERVICE }}

          echo "✅ Deployment completed successfully!"

  # ==========================================================================
  # 알림
  # ==========================================================================
  notify:
    name: Notify Deployment
    needs: [build, deploy]
    runs-on: ubuntu-latest
    if: always()

    steps:
      - name: Send Slack Notification
        if: ${{ secrets.SLACK_WEBHOOK_URL != '' }}
        uses: slackapi/slack-github-action@v1.25.0
        with:
          payload: |
            {
              "text": "${{ needs.deploy.result == 'success' && '✅' || '❌' }} Deployment ${{ needs.deploy.result }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Spring Hexagonal Template*\n환경: Production\n상태: ${{ needs.deploy.result }}\n이미지: `${{ needs.build.outputs.image-tag }}`\n배포자: @${{ github.actor }}"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK

      - name: Create GitHub Summary
        run: |
          echo "## 🚀 Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Item | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| **Status** | ${{ needs.deploy.result }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Image** | \`${{ needs.build.outputs.image-tag }}\` |" >> $GITHUB_STEP_SUMMARY
          echo "| **Cluster** | ${{ env.ECS_CLUSTER }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Service** | ${{ env.ECS_SERVICE }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Actor** | @${{ github.actor }} |" >> $GITHUB_STEP_SUMMARY
```

### Phase 2 체크리스트

- [ ] AWS OIDC Provider 설정 (GitHub Actions용)
- [ ] ECR Repository 생성
- [ ] ECS Cluster/Service 생성
- [ ] `AWS_ROLE_ARN` Secret 등록
- [ ] (선택) `SLACK_WEBHOOK_URL` Secret 등록
- [ ] Dockerfile 최적화 (Multi-stage build)

---

## 4️⃣ Phase 3: IaC 통합 (Terraform)

### 목표
- 인프라를 코드로 관리
- PR 기반 인프라 변경 검토
- 배포와 인프라 변경 연동

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Phase 3: IaC Integration                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  PR (terraform/**)              main push (코드 변경)                   │
│      │                               │                                   │
│      ▼                               ▼                                   │
│  ┌───────────────┐             ┌─────────┐                              │
│  │ Terraform     │             │  Test   │                              │
│  │ Plan          │             │  Build  │                              │
│  └───────┬───────┘             └────┬────┘                              │
│          │                          │                                    │
│          ▼                          ▼                                    │
│  ┌───────────────┐             ┌─────────────┐                          │
│  │ PR Comment    │             │ Docker Push │                          │
│  │ (Plan 결과)   │             │ ECR         │                          │
│  └───────────────┘             └──────┬──────┘                          │
│                                       │                                  │
│  merge 후                             │                                  │
│      │                                ▼                                  │
│      ▼                          ┌─────────────┐                          │
│  ┌───────────────┐              │ Terraform   │◀─── 이미지 태그 전달    │
│  │ Terraform     │              │ Apply       │                          │
│  │ Apply         │              │ (ECS 업데이트)                         │
│  └───────────────┘              └─────────────┘                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### `.github/workflows/terraform-plan.yml`

```yaml
# ============================================================================
# Terraform Plan
# ============================================================================
# Phase 3: IaC 변경사항 미리보기
# ============================================================================

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
          # 프로젝트 확장 시 모듈 추가
          # - name: elasticache
          #   dir: terraform/elasticache
          # - name: rds
          #   dir: terraform/rds

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

  # PR에 Plan 결과 코멘트
  comment-plan:
    name: Comment Plan Results
    needs: terraform-plan
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'

    steps:
      - name: Download All Plan Artifacts
        uses: actions/download-artifact@v4
        with:
          path: plans

      - name: Comment Plan on PR
        uses: actions/github-script@v7
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const fs = require('fs');
            const path = require('path');

            const modules = ['ecr', 'ecs-web-api'];
            let planSummary = `## 📋 Terraform Plan Results\n\n`;

            for (const module of modules) {
              try {
                const planPath = path.join('plans', `plan-${module}`, `plan-${module}.txt`);
                const plan = fs.readFileSync(planPath, 'utf8');

                planSummary += `<details><summary>📦 ${module}</summary>\n\n`;
                planSummary += `\`\`\`terraform\n${plan.slice(0, 10000)}\n\`\`\`\n\n`;
                planSummary += `</details>\n\n`;
              } catch (error) {
                planSummary += `<details><summary>⚠️ ${module} - Error</summary>\n\n`;
                planSummary += `Unable to read plan: ${error.message}\n\n`;
                planSummary += `</details>\n\n`;
              }
            }

            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: planSummary
            });
```

### `.github/workflows/terraform-apply.yml`

```yaml
# ============================================================================
# Terraform Apply
# ============================================================================
# Phase 3: IaC 변경 적용 (수동 또는 빌드 파이프라인에서 호출)
# ============================================================================

name: Terraform Apply

on:
  workflow_dispatch:
  workflow_call:  # 다른 워크플로우에서 호출 가능
    inputs:
      image-tag:
        description: 'Docker image tag to deploy'
        required: false
        type: string
    secrets:
      AWS_ROLE_ARN:
        required: true

permissions:
  contents: read
  id-token: write

jobs:
  terraform-apply:
    name: Terraform Apply
    runs-on: ubuntu-latest
    timeout-minutes: 30
    strategy:
      max-parallel: 1  # 순차 실행 (의존성 순서)
      matrix:
        module:
          - name: ecr
            dir: terraform/ecr
            order: 1
          - name: ecs-web-api
            dir: terraform/ecs-web-api
            order: 2

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

      - name: Terraform Init
        working-directory: ${{ matrix.module.dir }}
        run: terraform init -reconfigure

      - name: Terraform Apply
        working-directory: ${{ matrix.module.dir }}
        run: |
          EXTRA_VARS=""

          # ECS 모듈에 이미지 태그 전달
          if [ "${{ matrix.module.name }}" == "ecs-web-api" ] && [ -n "${{ inputs.image-tag }}" ]; then
            echo "📦 Using image tag: ${{ inputs.image-tag }}"
            EXTRA_VARS="-var=image_tag=${{ inputs.image-tag }}"
          fi

          terraform plan $EXTRA_VARS -out=tfplan
          terraform apply -auto-approve tfplan

          echo "✅ Apply completed for ${{ matrix.module.name }}"

      - name: Terraform Output
        working-directory: ${{ matrix.module.dir }}
        run: terraform output
```

### Phase 3 체크리스트

- [ ] `terraform/` 디렉토리 구조 생성
- [ ] Terraform Backend 설정 (S3 + DynamoDB)
- [ ] 모듈별 Terraform 코드 작성
- [ ] GitHub Actions OIDC 권한 확장
- [ ] PR 기반 Plan 검토 프로세스 확립

---

## 5️⃣ Phase 4: 엔터프라이즈 (다중 서비스)

### 목표
- Reusable Workflows로 코드 재사용
- 다중 컴포넌트 병렬 빌드/배포
- 환경별 배포 전략 (dev → staging → prod)

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Phase 4: Enterprise Pipeline                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────┐                                                                │
│  │  Test   │                                                                │
│  └────┬────┘                                                                │
│       │                                                                      │
│       ▼                                                                      │
│  ┌──────────────┬──────────────┬──────────────┐                            │
│  │ build-       │ build-       │ build-       │  ← 병렬 빌드               │
│  │ web-api      │ scheduler    │ worker       │                            │
│  └──────┬───────┴──────┬───────┴──────┬───────┘                            │
│         │              │              │                                      │
│         ▼              ▼              ▼                                      │
│  ┌──────────────┬──────────────┬──────────────┐                            │
│  │ deploy-      │ deploy-      │ deploy-      │  ← 병렬 배포               │
│  │ web-api      │ scheduler    │ worker       │                            │
│  └──────┬───────┴──────┬───────┴──────┬───────┘                            │
│         │              │              │                                      │
│         └──────────────┼──────────────┘                                      │
│                        ▼                                                     │
│              ┌──────────────────┐                                           │
│              │ update-terraform │  ← 이미지 태그 동기화                     │
│              └────────┬─────────┘                                           │
│                       ▼                                                      │
│              ┌──────────────────┐                                           │
│              │     notify       │  ← Slack 통합 알림                        │
│              └──────────────────┘                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Reusable Workflows 구조

```
your-org/Infrastructure/
└── .github/
    └── workflows/
        ├── reusable-build-docker.yml     ← Docker 빌드 + ECR Push
        ├── reusable-deploy-ecs.yml       ← ECS 배포
        └── reusable-notify-slack.yml     ← Slack 알림
```

### `.github/workflows/build-and-deploy.yml` (다중 서비스)

```yaml
# ============================================================================
# Phase 4: Enterprise Build & Deploy
# ============================================================================
# Reusable Workflows 사용 - 600줄 → 150줄로 단순화
# ============================================================================

name: Build and Deploy to ECS

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths-ignore:
      - 'terraform/**'
      - '**.md'
      - 'docs/**'

permissions:
  contents: read
  id-token: write

env:
  AWS_REGION: ap-northeast-2
  ECS_CLUSTER: my-cluster-prod

jobs:
  # ==========================================================================
  # 테스트 (한 번만 실행)
  # ==========================================================================
  test:
    name: Run Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20

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

  # ==========================================================================
  # 빌드 Jobs (병렬 실행) - Reusable Workflow 사용
  # ==========================================================================
  build-web-api:
    name: Build web-api
    needs: test
    uses: your-org/Infrastructure/.github/workflows/reusable-build-docker.yml@main
    with:
      ecr-repository: my-app-web-api-prod
      component: web-api
      dockerfile: bootstrap/bootstrap-web-api/Dockerfile
      gradle-task: ":bootstrap:bootstrap-web-api:bootJar"
    secrets:
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}

  # 프로젝트 확장 시 추가
  # build-scheduler:
  #   name: Build scheduler
  #   needs: test
  #   uses: your-org/Infrastructure/.github/workflows/reusable-build-docker.yml@main
  #   with:
  #     ecr-repository: my-app-scheduler-prod
  #     component: scheduler
  #     dockerfile: bootstrap/bootstrap-scheduler/Dockerfile
  #     gradle-task: ":bootstrap:bootstrap-scheduler:bootJar"
  #   secrets:
  #     AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}

  # ==========================================================================
  # 배포 Jobs (병렬 실행)
  # ==========================================================================
  deploy-web-api:
    name: Deploy web-api
    needs: build-web-api
    uses: your-org/Infrastructure/.github/workflows/reusable-deploy-ecs.yml@main
    with:
      ecs-cluster: my-cluster-prod
      ecs-service: my-app-web-api-prod
      image-uri: ${{ needs.build-web-api.outputs.image-uri }}
    secrets:
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}

  # ==========================================================================
  # Terraform Image Tag 업데이트
  # ==========================================================================
  update-terraform-tags:
    name: Update Terraform Image Tags
    needs: [build-web-api]
    uses: ./.github/workflows/terraform-apply.yml
    with:
      image-tag: ${{ needs.build-web-api.outputs.image-tag }}
    secrets:
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}

  # ==========================================================================
  # 통합 알림
  # ==========================================================================
  notify:
    name: Deployment Notification
    needs: [deploy-web-api, update-terraform-tags]
    if: always()
    uses: your-org/Infrastructure/.github/workflows/reusable-notify-slack.yml@main
    with:
      project-name: My App
      environment: prod
      status: ${{ needs.deploy-web-api.result == 'success' && needs.update-terraform-tags.result == 'success' && 'success' || 'failure' }}
      components: |
        [
          {"name": "web-api", "status": "${{ needs.deploy-web-api.result }}"},
          {"name": "terraform-tags", "status": "${{ needs.update-terraform-tags.result }}"}
        ]
    secrets:
      SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### Phase 4 체크리스트

- [ ] 중앙 Infrastructure 레포지토리 생성
- [ ] Reusable Workflows 구현 (build, deploy, notify)
- [ ] 다중 bootstrap 모듈 구조 (scheduler, worker 등)
- [ ] 환경별 배포 전략 수립 (dev → staging → prod)
- [ ] Slack/Teams 알림 통합
- [ ] GitHub Summary 활용

---

## 6️⃣ Dockerfile 가이드

### Multi-stage Build (권장)

```dockerfile
# ============================================================================
# Spring Hexagonal Template - Web API Dockerfile
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

# 보안: non-root 사용자 생성
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

### Dockerfile 최적화 포인트

| 항목 | 설명 |
|------|------|
| **Multi-stage** | 빌드 의존성 제외, 이미지 크기 최소화 |
| **Alpine** | 경량 베이스 이미지 (~5MB vs ~200MB) |
| **캐시 레이어** | 의존성 먼저 다운로드 → 소스 복사 (변경 시 캐시 활용) |
| **non-root** | 보안: root 권한 없이 실행 |
| **JVM 옵션** | 컨테이너 환경 최적화 (메모리, CPU) |
| **헬스체크** | Actuator 엔드포인트로 상태 확인 |

---

## 7️⃣ AWS OIDC 설정

### GitHub Actions OIDC 연동 (권장)

Secrets에 AWS 키를 저장하는 대신 OIDC를 사용하여 더 안전하게 인증합니다.

### IAM Identity Provider 생성

```bash
# AWS CLI로 OIDC Provider 생성
aws iam create-open-id-connect-provider \
  --url "https://token.actions.githubusercontent.com" \
  --client-id-list "sts.amazonaws.com" \
  --thumbprint-list "6938fd4d98bab03faadb97b34396831e3780aea1"
```

### IAM Role 생성 (Trust Policy)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:your-org/your-repo:*"
        }
      }
    }
  ]
}
```

### GitHub Secrets 등록

```
AWS_ROLE_ARN = arn:aws:iam::123456789012:role/GitHubActionsRole
```

---

## 8️⃣ 디렉토리 구조 (최종)

### Phase 4 완성 시 구조

```
spring-hexagonal-template/
├── .github/
│   └── workflows/
│       ├── ci.yml                    ← Phase 1: 기본 CI
│       ├── build-and-deploy.yml      ← Phase 2-4: CD
│       ├── terraform-plan.yml        ← Phase 3: IaC Plan
│       └── terraform-apply.yml       ← Phase 3: IaC Apply
│
├── bootstrap/
│   ├── bootstrap-web-api/
│   │   ├── Dockerfile
│   │   └── ...
│   ├── bootstrap-scheduler/          ← Phase 4 확장
│   │   └── Dockerfile
│   └── bootstrap-worker/             ← Phase 4 확장
│       └── Dockerfile
│
├── terraform/
│   ├── ecr/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── ecs-web-api/
│   ├── ecs-scheduler/                ← Phase 4 확장
│   ├── ecs-worker/                   ← Phase 4 확장
│   ├── elasticache/
│   ├── rds/
│   └── s3/
│
└── docs/
    └── coding_convention/
        └── 00-project-setup/
            └── github-workflows-guide.md  ← 이 문서
```

---

## 9️⃣ 체크리스트 종합

### Phase 1 (기본 CI) ✅ 현재 템플릿

- [ ] `.github/workflows/ci.yml` 생성
- [ ] QA 도구 검증 (Checkstyle, SpotBugs, PMD, Spotless)
- [ ] JaCoCo 커버리지 검증
- [ ] ArchUnit 아키텍처 테스트

### Phase 2 (CD 추가)

- [ ] AWS OIDC Provider 설정
- [ ] ECR Repository 생성
- [ ] ECS Cluster/Service 생성
- [ ] `build-and-deploy.yml` 구성
- [ ] Slack 알림 연동 (선택)

### Phase 3 (IaC 통합)

- [ ] Terraform Backend 설정 (S3 + DynamoDB)
- [ ] `terraform/` 모듈 구조 생성
- [ ] `terraform-plan.yml` 구성
- [ ] `terraform-apply.yml` 구성
- [ ] PR 기반 Plan 검토 프로세스

### Phase 4 (엔터프라이즈)

- [ ] 중앙 Infrastructure 레포 생성
- [ ] Reusable Workflows 구현
- [ ] 다중 bootstrap 모듈 확장
- [ ] 환경별 배포 전략 (dev/staging/prod)
- [ ] 통합 모니터링 및 알림

---

## 📖 관련 문서

- **[Gradle Configuration](./gradle-configuration.md)** - 빌드 설정 및 QA 도구
- **[Multi-Module Structure](./multi-module-structure.md)** - 멀티모듈 구조
- **[Version Management](./version-management.md)** - 버전 관리 전략

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
