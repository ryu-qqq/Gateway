# Docker Compose 환경 비교

## 개요

FileFlow는 두 가지 로컬 개발 환경을 제공합니다:
- **로컬 환경** (`docker-compose.local.yml`): 완전 독립 환경
- **AWS 연결 환경** (`docker-compose.aws.yml`): 프로덕션 리소스 연결

## 상세 비교표

### Infrastructure

| 구분 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **MySQL** | Docker 컨테이너 (mysql:8.0) | AWS RDS (SSM 포워딩) |
| **Redis** | Docker 컨테이너 (redis:7-alpine) | AWS ElastiCache (SSM 포워딩) |
| **볼륨** | `mysql_data`, `redis_data` | 없음 (AWS 관리) |
| **네트워크** | `fileflow-network` (bridge) | `extra_hosts` (host 접근) |

### Database 연결

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **Host** | `mysql` (컨테이너명) | `host.docker.internal` |
| **Port** | `3306` (컨테이너 내부)<br>`13306` (로컬 접속) | `13307` (포워딩) |
| **User** | `root` | `${AWS_DB_USER}` (admin) |
| **Password** | `root` (하드코딩) | `${AWS_DB_PASSWORD}` (환경변수) |
| **Database** | `fileflow` | `${AWS_DB_NAME}` (fileflow) |

### Redis 연결

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **Host** | `redis` (컨테이너명) | `host.docker.internal` |
| **Port** | `6379` (컨테이너 내부)<br>`16379` (로컬 접속) | `16380` (포워딩) |
| **Password** | 없음 | `${AWS_REDIS_PASSWORD}` |

### AWS 서비스

| 서비스 | 로컬 환경 | AWS 연결 환경 |
|--------|----------|--------------|
| **S3** | 비활성화/Mock | 실제 AWS S3 |
| **SQS** | 비활성화 (`false`) | 실제 AWS SQS (`true`) |
| **Credentials** | `test` / `test` | 실제 AWS 자격 증명 |
| **Session Token** | 없음 | SSO 사용 시 필요 |

### Application 설정

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **Container Name** | `fileflow-web-api` | `fileflow-web-api-aws` |
| **Worker Enabled** | `false` | `true` |
| **SQS Enabled** | `false` | `true` |
| **Log Level** | `DEBUG` (하드코딩) | `${LOG_LEVEL}` (환경변수) |

## 연결 흐름 비교

### 로컬 환경

```
┌─────────────────────────────────────┐
│  Docker Network (fileflow-network)  │
│                                     │
│  ┌──────────┐    ┌──────────┐      │
│  │ Web API  │───▶│  MySQL   │      │
│  │          │    │  :3306   │      │
│  └──────────┘    └──────────┘      │
│       │                             │
│       │          ┌──────────┐      │
│       └─────────▶│  Redis   │      │
│                  │  :6379   │      │
│                  └──────────┘      │
└─────────────────────────────────────┘

모든 리소스가 Docker 내부에서 완결
```

### AWS 연결 환경

```
┌─────────────────────────────────────┐
│  Docker Container                   │
│  ┌──────────┐                       │
│  │ Web API  │                       │
│  │          │                       │
│  └────┬─────┘                       │
└───────┼─────────────────────────────┘
        │ host.docker.internal
        ▼
┌─────────────────────────────────────┐
│  로컬 머신 (Mac)                     │
│  localhost:3307 ◄──┐                │
│  localhost:6380 ◄──┤ SSM Forwarding │
└────────────────────┼─────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │  AWS (Bastion Host)   │
         └───────┬───────────────┘
                 │
         ┌───────┴────────┐
         ▼                ▼
    ┌────────┐      ┌──────────┐
    │  RDS   │      │ElastiCache│
    │ :3306  │      │  :6379   │
    └────────┘      └──────────┘

    ┌────────┐      ┌──────────┐
    │   S3   │      │   SQS    │
    │(직접)  │      │ (직접)   │
    └────────┘      └──────────┘

SSM 포워딩 + 직접 연결 혼합
```

## 환경 변수 비교

### 로컬 환경 (.env.local)

```bash
# 간단한 설정
LOG_LEVEL=DEBUG
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=fileflow
MYSQL_USER=fileflow
MYSQL_PASSWORD=fileflow
```

### AWS 연결 환경 (.env.aws)

```bash
# 실제 AWS 자격 증명 필요
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_SESSION_TOKEN=...  # SSO 사용 시

# RDS
AWS_DB_NAME=fileflow
AWS_DB_USER=admin
AWS_DB_PASSWORD=actual-production-password

# ElastiCache
AWS_REDIS_PASSWORD=

# S3
AWS_S3_BUCKET=fileflow-bucket-prod

# Logging
LOG_LEVEL=DEBUG
```

## 사용 시나리오

### 로컬 환경 사용 시기

✅ **다음 경우에 사용:**
- 일반적인 기능 개발
- 단위 테스트 작성
- 로컬에서 빠른 반복 개발
- 인터넷 연결이 불안정한 환경
- AWS 계정이 없는 개발자
- 데이터베이스 스키마 변경 테스트

❌ **다음 경우에는 부적합:**
- S3 파일 업로드/다운로드 테스트
- SQS 메시지 처리 테스트
- 프로덕션 데이터 디버깅
- 실제 환경과 동일한 조건 필요

### AWS 연결 환경 사용 시기

✅ **다음 경우에 사용:**
- 프로덕션 데이터 디버깅
- S3/SQS 통합 테스트
- 실제 환경 재현
- 데이터 마이그레이션 테스트
- 성능 프로파일링 (실제 데이터)

❌ **다음 경우에는 부적합:**
- 일반적인 기능 개발
- 빠른 반복 개발
- 데이터베이스 스키마 변경 (위험)
- 인터넷 연결이 불안정한 환경

## 성능 비교

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **시작 시간** | ~30초 | ~2분 (포워딩 포함) |
| **DB 응답 속도** | 매우 빠름 (<1ms) | 느림 (~50-100ms) |
| **Redis 응답 속도** | 매우 빠름 (<1ms) | 느림 (~50-100ms) |
| **S3 업로드** | 불가능 | 실제 속도 |
| **SQS 처리** | 불가능 | 실제 속도 |

## 비용 비교

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **Infrastructure** | 무료 (로컬 리소스) | 무료 (기존 AWS 사용) |
| **Bastion Host** | 불필요 | ~$3/월 (t3.nano) |
| **데이터 전송** | 무료 | 소량 (거의 무료) |
| **총 비용** | $0 | ~$3/월 |

## 보안 비교

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **자격 증명 관리** | 간단 (test/test) | 엄격 (실제 AWS) |
| **데이터 노출 위험** | 낮음 (로컬 데이터) | 높음 (프로덕션 데이터) |
| **접근 로깅** | 없음 | CloudTrail 자동 |
| **네트워크 격리** | 완전 격리 | VPC 내부 |

## 추천 워크플로우

### 일반 개발자

1. **기본**: 로컬 환경 사용
2. **S3/SQS 테스트 필요 시**: AWS 연결 환경으로 전환
3. **테스트 완료 후**: 다시 로컬 환경으로 복귀

### 시니어/DevOps

1. **개발**: 로컬 환경
2. **통합 테스트**: AWS 연결 환경
3. **프로덕션 이슈 디버깅**: AWS 연결 환경
4. **성능 프로파일링**: AWS 연결 환경

## 전환 방법

### 로컬 → AWS 연결

```bash
# 1. 로컬 환경 종료
cd local-dev
./scripts/local-stop.sh

# 2. AWS 환경 설정
cp .env.aws .env.aws
vim .env.aws

# 3. 포트 포워딩 시작 (터미널 1)
./scripts/aws-port-forward.sh

# 4. AWS 환경 시작 (터미널 2)
./scripts/aws-start.sh
```

### AWS 연결 → 로컬

```bash
# 1. AWS 환경 종료
cd local-dev
./scripts/aws-stop.sh
# 포트 포워딩 터미널에서 Ctrl+C

# 2. 로컬 환경 시작
./scripts/local-start.sh
```

## 결론

- **일반 개발**: `docker-compose.local.yml` 사용
- **프로덕션 테스트**: `docker-compose.aws.yml` 사용
- **둘 다 유지**: 상황에 따라 빠르게 전환
