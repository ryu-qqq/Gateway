# Connectly Gateway 로컬 개발 환경

로컬에서 Connectly Gateway를 개발하고 테스트하기 위한 Docker Compose 환경입니다.

## 📦 구성

```
local-dev/
├── README.md                          # 이 파일
├── docker-compose.local.yml          # 완전 독립 로컬 환경
├── docker-compose.aws.yml            # AWS 리소스 연결 환경
├── Dockerfile.local                  # 로컬 빌드용 Dockerfile
├── .env.local                        # 로컬 환경 변수
└── .env.aws                          # AWS 환경 변수 템플릿
```

## 🏗️ Gateway 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    Connectly Gateway                         │
│                  (Spring Cloud Gateway)                      │
├─────────────────────────────────────────────────────────────┤
│  • JWT 인증/인가 (JWKS via AuthHub)                          │
│  • 라우팅 (Microservices 프록시)                             │
│  • Rate Limiting (Redis 기반)                                │
│  • Request/Response Logging                                  │
└─────────────────────────────────────────────────────────────┘
         │                              │
         ▼                              ▼
   ┌──────────┐                  ┌──────────┐
   │  Redis   │                  │ AuthHub  │
   │ (Cache)  │                  │ (JWKS)   │
   └──────────┘                  └──────────┘
```

## 🚀 빠른 시작

### 방법 1: 완전 독립 로컬 환경 (권장)

로컬 Redis를 Docker로 실행하여 독립된 환경에서 개발합니다.

```bash
cd local-dev

# 시작
docker-compose -f docker-compose.local.yml up -d

# 로그 확인
docker-compose -f docker-compose.local.yml logs -f gateway

# 종료
docker-compose -f docker-compose.local.yml down
```

**특징:**
- ✅ 인터넷 연결 불필요 (빌드 후)
- ✅ AWS 계정 불필요
- ✅ 빠른 시작/종료
- ⚠️ AuthHub 서버가 로컬에서 실행 중이어야 함 (localhost:9090)

### 방법 2: AWS 리소스 연결 환경 (프로덕션 테스트용)

실제 AWS ElastiCache에 연결하여 프로덕션과 동일한 환경에서 테스트합니다.

```bash
cd local-dev

# 1. 환경 변수 설정 (필수)
vim .env.aws  # AWS 자격 증명 입력

# 2. AWS SSM 포트 포워딩 시작 (터미널 1)
aws ssm start-session --target <bastion-instance-id> \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["<redis-endpoint>"],"portNumber":["6379"],"localPortNumber":["16379"]}'

# 3. Docker Compose 시작 (터미널 2)
docker-compose --env-file .env.aws -f docker-compose.aws.yml up -d

# 4. 로그 확인
docker-compose -f docker-compose.aws.yml logs -f gateway

# 5. 종료
docker-compose -f docker-compose.aws.yml down
# 포트 포워딩 터미널에서 Ctrl+C
```

**특징:**
- ✅ 실제 프로덕션 Redis 캐시 접근
- ✅ 프로덕션 환경 디버깅
- ❌ AWS 계정 및 권한 필요
- ❌ 인터넷 연결 필수

## 📊 환경 비교

| 항목 | 로컬 환경 | AWS 연결 환경 |
|------|----------|--------------|
| **Redis** | 로컬 Docker 컨테이너 | AWS ElastiCache (SSM 포워딩) |
| **AuthHub** | localhost:9090 | localhost:9090 또는 실제 AuthHub |
| **데이터** | 로컬 테스트 데이터 | 프로덕션 캐시 데이터 |
| **AWS 계정** | 불필요 | 필요 |
| **인터넷** | 불필요 | 필요 |
| **시작 속도** | 빠름 (~30초) | 느림 (~1분) |
| **용도** | 일반 개발, 단위 테스트 | 통합 테스트, 디버깅 |

## 🔧 서비스 포트

| 서비스 | 로컬 환경 | AWS 환경 |
|--------|----------|----------|
| Gateway | http://localhost:8080 | http://localhost:8080 |
| Redis | localhost:16379 | localhost:16379 (포워딩) |
| AuthHub (외부) | localhost:9090 | localhost:9090 |

## 🔍 헬스체크

```bash
# Gateway 헬스체크
curl http://localhost:8080/actuator/health

# Gateway 메트릭스
curl http://localhost:8080/actuator/metrics

# Prometheus 메트릭스
curl http://localhost:8080/actuator/prometheus
```

## 🛠️ 트러블슈팅

### 포트 충돌

```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :16379

# 프로세스 종료
kill -9 <PID>
```

### Docker 빌드 실패

```bash
# 캐시 없이 재빌드
docker-compose -f docker-compose.local.yml build --no-cache
```

### AuthHub 연결 실패

```bash
# AuthHub 서버 실행 확인
curl http://localhost:9090/actuator/health

# AuthHub JWKS 엔드포인트 확인
curl http://localhost:9090/api/v1/auth/jwks
```

### AWS 연결 실패

```bash
# AWS 자격 증명 확인
aws sts get-caller-identity

# SSM 연결 확인 후 포트 포워딩 재시작
```

### Redis 연결 실패

```bash
# Redis 컨테이너 상태 확인
docker-compose -f docker-compose.local.yml ps redis

# Redis 직접 연결 테스트
redis-cli -h localhost -p 16379 ping
```

## 🔒 보안 주의사항

### 로컬 환경
- `.env.local` 파일은 Git에 커밋하지 마세요

### AWS 환경
- `.env.aws` 파일은 **절대** Git에 커밋하지 마세요
- AWS 자격 증명은 최소 권한 원칙 적용
- AWS SSO 사용 권장 (임시 자격 증명)

## 📚 추가 리소스

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [AWS SSM Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [Spring Cloud Gateway 문서](https://spring.io/projects/spring-cloud-gateway)
