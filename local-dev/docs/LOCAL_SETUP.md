# FileFlow 로컬 개발 환경 가이드

## 개요

이 가이드는 FileFlow 프로젝트를 로컬 환경에서 전체 스택으로 실행하는 방법을 설명합니다.

## 사전 요구사항

- Docker Desktop (Mac/Windows) 또는 Docker Engine (Linux)
- Docker Compose
- 최소 8GB RAM (권장: 16GB)
- 최소 10GB 디스크 공간

## 빠른 시작

### 1. 전체 환경 시작

```bash
# 실행 권한 부여 (최초 1회)
chmod +x scripts/local-start.sh scripts/local-stop.sh

# 전체 환경 시작
./scripts/local-start.sh
```

### 2. 서비스 확인

시작 후 다음 URL에서 서비스 상태를 확인할 수 있습니다:

- **Web API**: http://localhost:8080/actuator/health
- **Scheduler**: http://localhost:8081/actuator/health
- **Download Worker**: http://localhost:8082/actuator/health

### 3. 환경 종료

```bash
./scripts/local-stop.sh
```

## 서비스 구성

### Infrastructure Services

| 서비스 | 포트 | 설명 |
|--------|------|------|
| MySQL | 13306 | 메인 데이터베이스 |
| Redis | 16379 | 캐시 및 분산 락 |

**MySQL 접속 정보:**
- Host: localhost
- Port: 13306
- Database: fileflow
- Username: root
- Password: root

**Redis 접속 정보:**
- Host: localhost
- Port: 16379
- Password: (없음)

### Application Services

| 서비스 | 포트 | 설명 |
|--------|------|------|
| Web API | 8080 | REST API 서버 |
| Scheduler | 8081 | 스케줄링 작업 서버 |
| Download Worker | 8082 | 다운로드 워커 서버 |

## 수동 Docker Compose 명령어

### 전체 서비스 시작

```bash
docker-compose -f docker-compose.local.yml up -d
```

### 특정 서비스만 시작

```bash
# Infrastructure만 시작
docker-compose -f docker-compose.local.yml up -d mysql redis

# Web API만 시작
docker-compose -f docker-compose.local.yml up -d web-api
```

### 로그 확인

```bash
# 전체 로그
docker-compose -f docker-compose.local.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose.local.yml logs -f web-api
docker-compose -f docker-compose.local.yml logs -f scheduler
docker-compose -f docker-compose.local.yml logs -f download-worker
```

### 서비스 재시작

```bash
# 전체 재시작
docker-compose -f docker-compose.local.yml restart

# 특정 서비스 재시작
docker-compose -f docker-compose.local.yml restart web-api
```

### 서비스 중지 및 제거

```bash
# 서비스 중지 (데이터 유지)
docker-compose -f docker-compose.local.yml down

# 서비스 중지 및 볼륨 삭제 (데이터 삭제)
docker-compose -f docker-compose.local.yml down -v
```

## 개발 워크플로우

### 코드 변경 후 재배포

1. **특정 서비스 재빌드 및 재시작:**

```bash
# Web API 재빌드
docker-compose -f docker-compose.local.yml build web-api
docker-compose -f docker-compose.local.yml up -d web-api

# Scheduler 재빌드
docker-compose -f docker-compose.local.yml build scheduler
docker-compose -f docker-compose.local.yml up -d scheduler

# Download Worker 재빌드
docker-compose -f docker-compose.local.yml build download-worker
docker-compose -f docker-compose.local.yml up -d download-worker
```

2. **전체 재빌드:**

```bash
docker-compose -f docker-compose.local.yml build
docker-compose -f docker-compose.local.yml up -d
```

### 데이터베이스 초기화

```bash
# MySQL 데이터 삭제 및 재시작
docker-compose -f docker-compose.local.yml down -v
docker-compose -f docker-compose.local.yml up -d mysql redis

# Flyway 마이그레이션은 애플리케이션 시작 시 자동 실행됨
docker-compose -f docker-compose.local.yml up -d web-api scheduler download-worker
```

## 트러블슈팅

### 포트 충돌

다른 애플리케이션이 포트를 사용 중인 경우:

```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :13306
lsof -i :16379

# 프로세스 종료
kill -9 <PID>
```

### 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker-compose -f docker-compose.local.yml ps

# 컨테이너 상세 정보
docker inspect fileflow-web-api
docker inspect fileflow-mysql
docker inspect fileflow-redis
```

### 빌드 캐시 문제

```bash
# 빌드 캐시 없이 재빌드
docker-compose -f docker-compose.local.yml build --no-cache

# Docker 시스템 정리
docker system prune -a
```

### MySQL 연결 실패

```bash
# MySQL 컨테이너 로그 확인
docker-compose -f docker-compose.local.yml logs mysql

# MySQL 컨테이너 내부 접속 (컨테이너 내부에서는 3306)
docker exec -it fileflow-mysql mysql -u root -proot fileflow

# 또는 로컬에서 직접 접속 (포트 13306)
mysql -h 127.0.0.1 -P 13306 -u root -proot fileflow
```

### Redis 연결 실패

```bash
# Redis 컨테이너 로그 확인
docker-compose -f docker-compose.local.yml logs redis

# Redis 컨테이너 내부 접속 (컨테이너 내부에서는 6379)
docker exec -it fileflow-redis redis-cli

# 또는 로컬에서 직접 접속 (포트 16379)
redis-cli -h 127.0.0.1 -p 16379
```

## 환경 변수 커스터마이징

`docker-compose.local.yml` 파일에서 각 서비스의 환경 변수를 수정할 수 있습니다:

```yaml
services:
  web-api:
    environment:
      LOG_LEVEL: INFO  # DEBUG에서 INFO로 변경
      DB_NAME: custom_db  # 데이터베이스 이름 변경
```

## 성능 최적화

### Docker Desktop 리소스 설정

Docker Desktop > Preferences > Resources에서:
- **CPUs**: 최소 4개 (권장: 8개)
- **Memory**: 최소 8GB (권장: 16GB)
- **Swap**: 2GB
- **Disk**: 최소 10GB

### 빌드 속도 향상

```bash
# Gradle 데몬 활성화 (이미 기본값)
# 멀티 모듈 병렬 빌드
./gradlew :bootstrap:bootstrap-web-api:bootJar --parallel
```

## 추가 도구

### MySQL 클라이언트

- [MySQL Workbench](https://www.mysql.com/products/workbench/)
- [DBeaver](https://dbeaver.io/)
- [TablePlus](https://tableplus.com/)

### Redis 클라이언트

- [RedisInsight](https://redis.com/redis-enterprise/redis-insight/)
- [Medis](https://getmedis.com/)
- [Another Redis Desktop Manager](https://github.com/qishibo/AnotherRedisDesktopManager)

## 참고 사항

- 로컬 환경은 `prod` 프로파일을 사용하지만 AWS 서비스는 LocalStack 또는 Mock으로 대체됩니다
- Flyway 마이그레이션은 애플리케이션 시작 시 자동으로 실행됩니다
- 모든 서비스는 `fileflow-network` 네트워크를 통해 통신합니다
- 데이터는 Docker 볼륨에 저장되어 컨테이너 재시작 시에도 유지됩니다
