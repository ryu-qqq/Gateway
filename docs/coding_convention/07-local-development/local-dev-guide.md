# 로컬 개발 환경 가이드

## 1. 개요

Spring Standards Template 프로젝트의 로컬 개발 환경 설정 및 사용 가이드입니다.

### 1.1 두 가지 환경

| 환경 | 용도 | 특징 |
|------|------|------|
| **로컬 환경** | 일반 개발, 단위 테스트 | Docker 컨테이너로 완전 독립 |
| **AWS 연결 환경** | 통합 테스트, 디버깅 | 실제 AWS 리소스 사용 |

### 1.2 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           로컬 개발 환경                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                     Docker Compose                              │  │
│   │                                                                 │  │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────────────┐   │  │
│   │  │ web-api │  │  MySQL  │  │  Redis  │  │   Admin Tools    │   │  │
│   │  │  :8080  │  │ :13306  │  │ :16379  │  │ phpMyAdmin:18080 │   │  │
│   │  └────┬────┘  └────┬────┘  └────┬────┘  │ Redis Cmd:18081  │   │  │
│   │       └────────────┴────────────┘       └──────────────────┘   │  │
│   │                 template-network                                │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 2. 사전 준비

### 2.1 필수 도구

```bash
# Docker Desktop 설치 (macOS)
brew install --cask docker

# 설치 확인
docker --version
docker-compose --version
```

### 2.2 (AWS 환경만) 추가 도구

```bash
# AWS CLI
brew install awscli

# Session Manager Plugin
brew install --cask session-manager-plugin

# 설치 확인
aws --version
session-manager-plugin
```

## 3. 로컬 환경 사용법

### 3.1 빠른 시작

```bash
cd local-dev

# 시작
./scripts/start.sh

# 종료
./scripts/stop.sh
```

### 3.2 서비스 접속 정보

| 서비스 | URL / 포트 | 인증 정보 |
|--------|-----------|-----------|
| Web API | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| phpMyAdmin | http://localhost:18080 | 자동 로그인 |
| Redis Commander | http://localhost:18081 | admin / admin |
| MySQL | localhost:13306 | root / root |
| Redis | localhost:16379 | - |

### 3.3 데이터베이스 CLI 접속

```bash
# MySQL
mysql -h localhost -P 13306 -u root -proot template

# Redis
redis-cli -h localhost -p 16379
```

### 3.4 로그 확인

```bash
# 전체 로그
docker-compose -f docker-compose.local.yml logs -f

# 특정 서비스
docker-compose -f docker-compose.local.yml logs -f web-api
```

## 4. AWS 연결 환경 사용법

### 4.1 아키텍처

```
┌────────────────────────────────────────────────────────────────────────┐
│                        로컬 머신                                        │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────┐    ┌──────────────────────────────────────────────┐ │
│  │   Docker     │    │       SSM Port Forwarding                    │ │
│  │  Container   │    │       (aws-port-forward.sh)                  │ │
│  │              │    │                                              │ │
│  │  web-api     │───▶│  localhost:13307 ──┐                         │ │
│  │  phpMyAdmin  │───▶│  localhost:16380 ─┐│                         │ │
│  │  Redis Cmd   │    └───────────────────┼┼─────────────────────────┘ │
│  └──────────────┘                        ││                           │
│                                          ││  AWS SSM Tunnel           │
└──────────────────────────────────────────┼┼───────────────────────────┘
                                           ││
┌──────────────────────────────────────────┼┼───────────────────────────┐
│                        AWS VPC           ▼▼                           │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐            │
│  │   Bastion    │    │     RDS      │    │ ElastiCache  │            │
│  │   Host       │───▶│    MySQL     │    │    Redis     │            │
│  │   (EC2)      │    │    :3306     │    │    :6379     │            │
│  └──────────────┘    └──────────────┘    └──────────────┘            │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### 4.2 초기 설정 (최초 1회)

```bash
cd local-dev

# 1. 환경 변수 파일 생성
cp .env.aws.example .env.aws

# 2. AWS 정보 입력
vim .env.aws
```

**.env.aws 필수 항목:**

```bash
# AWS 자격 증명
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...

# Bastion Host
AWS_BASTION_INSTANCE_ID=i-0123456789abcdef0

# RDS
AWS_RDS_ENDPOINT=mydb.cluster-xxx.rds.amazonaws.com
AWS_RDS_PASSWORD=your-password

# ElastiCache
AWS_REDIS_ENDPOINT=mycache.xxx.cache.amazonaws.com
```

### 4.3 실행 방법

```bash
# 터미널 1: 포트 포워딩 시작
./scripts/aws-port-forward.sh

# 터미널 2: 애플리케이션 시작
./scripts/aws-start.sh
```

### 4.4 서비스 접속 정보

| 서비스 | URL / 포트 | 인증 정보 |
|--------|-----------|-----------|
| Web API | http://localhost:8080 | - |
| phpMyAdmin | http://localhost:18080 | .env.aws 참조 |
| Redis Commander | http://localhost:18081 | admin / admin |
| MySQL (RDS) | localhost:13307 | .env.aws 참조 |
| Redis (Cache) | localhost:16380 | - |

### 4.5 종료

```bash
# 터미널 2: 애플리케이션 종료
./scripts/aws-stop.sh

# 터미널 1: Ctrl+C로 포트 포워딩 종료
```

## 5. Admin Tools 활용

### 5.1 phpMyAdmin

**URL:** http://localhost:18080

**주요 기능:**
- 테이블 구조 확인 및 DDL 관리
- SQL 쿼리 직접 실행
- 데이터 조회/수정/삭제
- 데이터 Import/Export (최대 100MB)

**활용 시나리오:**
```
1. Flyway 마이그레이션 결과 확인
2. 테스트 데이터 수동 삽입
3. 쿼리 성능 테스트 (EXPLAIN 실행)
4. 스키마 변경 전 백업
```

### 5.2 Redis Commander

**URL:** http://localhost:18081
**인증:** admin / admin

**주요 기능:**
- 키 목록 조회 및 패턴 검색
- 키 값 조회/수정/삭제
- TTL 확인 및 변경
- 실시간 명령 모니터링

**활용 시나리오:**
```
1. 캐시 키 구조 확인
2. 세션 데이터 디버깅
3. 분산 락 상태 확인
4. TTL 정책 검증
```

## 6. 트러블슈팅

### 6.1 포트 충돌

```bash
# 사용 중인 포트 확인
lsof -i :8080
lsof -i :18080

# 프로세스 종료
kill -9 <PID>
```

### 6.2 Docker 빌드 실패

```bash
# 캐시 없이 재빌드
docker-compose -f docker-compose.local.yml build --no-cache

# 볼륨 초기화 후 재시작
docker-compose -f docker-compose.local.yml down -v
docker-compose -f docker-compose.local.yml up -d --build
```

### 6.3 MySQL 연결 대기

```bash
# MySQL 상태 확인
docker exec template-mysql mysqladmin ping -u root -proot

# 컨테이너 로그 확인
docker logs template-mysql
```

### 6.4 AWS SSM 연결 실패

```bash
# AWS 자격 증명 확인
aws sts get-caller-identity

# Bastion Host 상태 확인
aws ec2 describe-instances --instance-ids i-xxxx

# SSM Agent 상태 확인
aws ssm describe-instance-information
```

## 7. 보안 주의사항

### 7.1 로컬 환경

| 항목 | 주의사항 |
|------|---------|
| `.env.local` | Git에 커밋 금지 |
| 비밀번호 | 개발용 단순 비밀번호 (root/root, admin/admin) |
| 데이터 | 로컬 볼륨에 저장, `down -v`로 삭제 가능 |

### 7.2 AWS 환경

| 항목 | 주의사항 |
|------|---------|
| `.env.aws` | **절대** Git에 커밋 금지 |
| AWS 자격 증명 | 최소 권한 원칙, AWS SSO 권장 |
| 프로덕션 데이터 | 수정/삭제 시 각별히 주의 |
| Admin UI | 프로덕션 데이터 직접 접근 가능 |

## 8. 환경별 프로파일 매핑

| 환경 | Spring Profile | 용도 |
|------|---------------|------|
| 로컬 환경 | `local` | 개발, 단위 테스트 |
| AWS 연결 | `prod` | 통합 테스트, 디버깅 |
| CI/CD | `test` | 자동화 테스트 |

```yaml
# application-local.yml 예시
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:template}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
```

## 9. 참고

- [local-dev/README.md](../../../local-dev/README.md) - 상세 사용법
- [local-dev/docs/LOCAL_SETUP.md](../../../local-dev/docs/LOCAL_SETUP.md) - 로컬 환경 상세
- [local-dev/docs/AWS_SETUP.md](../../../local-dev/docs/AWS_SETUP.md) - AWS 연결 상세
