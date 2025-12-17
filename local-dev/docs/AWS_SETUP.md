# AWS 리소스 연결 로컬 개발 가이드

## 개요

로컬 Docker Compose 환경에서 실제 AWS 배포 리소스(RDS, ElastiCache, S3, SQS)에 안전하게 연결하여 테스트하는 방법을 설명합니다.

## 사전 요구사항

### 1. AWS CLI 설치

```bash
# macOS
brew install awscli

# 또는 공식 설치 프로그램
# https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
```

### 2. AWS Session Manager Plugin 설치

```bash
# macOS
brew install --cask session-manager-plugin

# 설치 확인
session-manager-plugin --version
```

### 3. AWS 자격 증명 설정

```bash
# 방법 1: AWS Configure
aws configure

# 방법 2: AWS SSO (권장)
aws sso login --profile your-profile
export AWS_PROFILE=your-profile
```

### 4. Terraform으로 Bastion Host 배포

AWS 리소스에 안전하게 접근하려면 Bastion Host가 필요합니다.

```bash
cd terraform/bastion  # Bastion Host Terraform 모듈 (생성 필요)
terraform init
terraform apply
```

## 빠른 시작

### 1. 환경 변수 설정

```bash
# .env.aws.example을 복사하여 .env.aws 생성
cp .env.aws .env.aws

# .env.aws 파일 편집 (실제 값으로 변경)
# - AWS_ACCESS_KEY_ID
# - AWS_SECRET_ACCESS_KEY
# - AWS_DB_PASSWORD
# - AWS_S3_BUCKET
```

### 2. 포트 포워딩 시작

```bash
# 실행 권한 부여 (최초 1회)
chmod +x scripts/aws-port-forward.sh

# 포트 포워딩 시작 (백그라운드에서 실행)
./scripts/aws-port-forward.sh
```

이 스크립트는 다음을 자동으로 수행합니다:
- Bastion Host 인스턴스 검색
- RDS 엔드포인트 검색 및 포트 포워딩 (localhost:13307 → RDS:3306)
- ElastiCache 엔드포인트 검색 및 포트 포워딩 (localhost:16380 → Redis:6379)

### 3. Docker Compose 실행

**새 터미널 창**에서 실행:

```bash
# AWS 리소스 연결 모드로 실행
docker-compose --env-file .env.aws -f docker-compose.aws.yml up -d

# 로그 확인
docker-compose -f docker-compose.aws.yml logs -f
```

### 4. 서비스 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 5. 종료

```bash
# Docker Compose 종료
docker-compose -f docker-compose.aws.yml down

# 포트 포워딩 종료 (포트 포워딩 터미널에서 Ctrl+C)
```

## 아키텍처

```
┌─────────────────┐
│  로컬 Docker    │
│  Compose        │
│                 │
│  ┌───────────┐  │
│  │ Web API   │  │
│  │ Scheduler │  │
│  │ Worker    │  │
│  └─────┬─────┘  │
└────────┼────────┘
         │
         │ host.docker.internal
         │
┌────────▼────────┐
│  로컬 머신      │
│                 │
│  localhost:3307 ◄──┐
│  localhost:6380 ◄──┤ SSM Port Forwarding
└─────────────────┘  │
                     │
         ┌───────────┴───────────┐
         │                       │
    ┌────▼─────┐          ┌─────▼────┐
    │ Bastion  │          │ Bastion  │
    │  Host    │          │  Host    │
    └────┬─────┘          └─────┬────┘
         │                      │
    ┌────▼─────┐          ┌─────▼────┐
    │   RDS    │          │ElastiCache│
    │  :3306   │          │  :6379   │
    └──────────┘          └──────────┘

    ┌──────────┐          ┌──────────┐
    │    S3    │          │   SQS    │
    │  (직접)  │          │  (직접)  │
    └──────────┘          └──────────┘
```

## 연결 방식 비교

| 리소스 | 연결 방식 | 포트 | 설명 |
|--------|----------|------|------|
| RDS | SSM Port Forwarding | 3307 → 3306 | Bastion을 통한 안전한 터널링 |
| ElastiCache | SSM Port Forwarding | 6380 → 6379 | Bastion을 통한 안전한 터널링 |
| S3 | 직접 연결 | - | AWS SDK + IAM 자격 증명 |
| SQS | 직접 연결 | - | AWS SDK + IAM 자격 증명 |

## 수동 포트 포워딩 (스크립트 미사용)

### RDS 포트 포워딩

```bash
# Bastion 인스턴스 ID 확인
BASTION_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=fileflow-bastion-prod" \
            "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text)

# RDS 엔드포인트 확인
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier fileflow-db-prod \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# 포트 포워딩 시작
aws ssm start-session \
  --target $BASTION_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"${RDS_ENDPOINT}\"],\"portNumber\":[\"3306\"],\"localPortNumber\":[\"3307\"]}"
```

### ElastiCache 포트 포워딩

```bash
# Redis 엔드포인트 확인
REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
  --cache-cluster-id fileflow-redis-prod \
  --show-cache-node-info \
  --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
  --output text)

# 포트 포워딩 시작
aws ssm start-session \
  --target $BASTION_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"${REDIS_ENDPOINT}\"],\"portNumber\":[\"6379\"],\"localPortNumber\":[\"6380\"]}"
```

## 직접 DB/Redis 접속 (디버깅용)

### MySQL 접속

```bash
# 로컬에서 RDS 직접 접속
mysql -h 127.0.0.1 -P 3307 -u admin -p

# 또는 Docker 컨테이너에서 접속
docker exec -it fileflow-web-api-aws sh
mysql -h host.docker.internal -P 3307 -u admin -p
```

### Redis 접속

```bash
# 로컬에서 ElastiCache 직접 접속
redis-cli -h 127.0.0.1 -p 6380

# 또는 Docker 컨테이너에서 접속
docker exec -it fileflow-web-api-aws sh
redis-cli -h host.docker.internal -p 6380
```

## 트러블슈팅

### 1. Session Manager Plugin 오류

```bash
# 설치 확인
session-manager-plugin --version

# 재설치
brew reinstall --cask session-manager-plugin
```

### 2. Bastion Host 찾을 수 없음

```bash
# Bastion Host 상태 확인
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=fileflow-bastion-prod" \
  --query 'Reservations[*].Instances[*].[InstanceId,State.Name,Tags[?Key==`Name`].Value|[0]]' \
  --output table

# Bastion Host가 없으면 Terraform으로 배포
cd terraform/bastion
terraform apply
```

### 3. 포트 충돌

```bash
# 포트 사용 확인
lsof -i :3307
lsof -i :6380

# 프로세스 종료
kill -9 <PID>
```

### 4. AWS 자격 증명 만료 (SSO)

```bash
# SSO 재로그인
aws sso login --profile your-profile

# 자격 증명 확인
aws sts get-caller-identity
```

### 5. Docker 컨테이너에서 host.docker.internal 접근 불가

```bash
# Docker Desktop 설정 확인
# Preferences > Resources > Network
# "Enable host.docker.internal" 체크

# Linux의 경우 docker-compose.aws.yml에서 extra_hosts 설정 확인
```

## 보안 고려사항

### 1. 자격 증명 관리

- `.env.aws` 파일은 **절대 Git에 커밋하지 마세요**
- `.gitignore`에 `.env.aws` 추가 확인
- AWS SSO 사용 권장 (임시 자격 증명)

### 2. 네트워크 보안

- Bastion Host는 SSM Session Manager만 허용 (SSH 포트 22 불필요)
- RDS/ElastiCache는 Private Subnet에 배치
- 보안 그룹은 Bastion에서만 접근 허용

### 3. IAM 권한

필요한 최소 권한:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:StartSession"
      ],
      "Resource": [
        "arn:aws:ec2:*:*:instance/*",
        "arn:aws:ssm:*:*:document/AWS-StartPortForwardingSessionToRemoteHost"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "rds:DescribeDBInstances",
        "elasticache:DescribeCacheClusters"
      ],
      "Resource": "*"
    }
  ]
}
```

## 대안: VPN 연결

SSM Session Manager 대신 VPN을 사용할 수도 있습니다:

### AWS Client VPN

1. Terraform으로 Client VPN 엔드포인트 생성
2. VPN 클라이언트 설정 다운로드
3. AWS VPN Client 설치 및 연결
4. VPC 내부 IP로 직접 접속

**장점:**
- 더 빠른 연결 속도
- 여러 리소스 동시 접근 용이

**단점:**
- 추가 비용 발생 (시간당 과금)
- 설정 복잡도 증가
- 보안 감사 로그 부족

## 비용 최적화

### 1. Bastion Host

```bash
# 사용하지 않을 때 중지
aws ec2 stop-instances --instance-ids $BASTION_ID

# 사용할 때만 시작
aws ec2 start-instances --instance-ids $BASTION_ID
```

### 2. 개발 환경 RDS

- 개발용 RDS는 `db.t3.micro` 또는 `db.t4g.micro` 사용
- Multi-AZ 비활성화
- 자동 백업 최소화 (1일)

### 3. ElastiCache

- `cache.t3.micro` 또는 `cache.t4g.micro` 사용
- 클러스터 모드 비활성화

## 참고 자료

- [AWS Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [Port Forwarding Using Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-remote-port-forwarding)
- [Docker host.docker.internal](https://docs.docker.com/desktop/networking/#i-want-to-connect-from-a-container-to-a-service-on-the-host)
