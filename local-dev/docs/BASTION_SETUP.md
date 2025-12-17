# Bastion을 통한 AWS 리소스 접근 가이드

## 개요

로컬 개발 환경에서 AWS 리소스(RDS, ElastiCache)에 안전하게 접근하기 위해 Bastion Host를 통한 SSM Session Manager 포트 포워딩을 사용합니다.

## 아키텍처

```
Local Machine
    ↓ (SSM Session Manager)
Bastion Host (prod-bastion: i-0a8162214660b8814)
    ↓ (VPC 내부)
├─ RDS: prod-shared-mysql.cfacertspqbw.ap-northeast-2.rds.amazonaws.com
└─ Redis: fileflow-redis-prod.j9czrc.0001.apn2.cache.amazonaws.com
```

## 필수 도구 설치

### 1. AWS CLI
```bash
# macOS
brew install awscli

# 버전 확인
aws --version
```

### 2. AWS Session Manager Plugin
```bash
# macOS
brew install --cask session-manager-plugin

# 버전 확인
session-manager-plugin --version
```

## AWS 자격 증명 설정

### 방법 1: AWS Configure (권장)
```bash
aws configure
# AWS Access Key ID: [입력]
# AWS Secret Access Key: [입력]
# Default region name: ap-northeast-2
# Default output format: json
```

### 방법 2: AWS SSO (조직에서 SSO 사용 시)
```bash
aws sso login
```

### 자격 증명 확인
```bash
aws sts get-caller-identity
```

## Bastion 정보 확인

### SSM Parameter Store에서 Bastion 정보 가져오기
```bash
# Bastion Instance ID
aws ssm get-parameter --name /shared/bastion/instance-id --region ap-northeast-2 --query 'Parameter.Value' --output text

# Bastion Security Group ID
aws ssm get-parameter --name /shared/bastion/security-group-id --region ap-northeast-2 --query 'Parameter.Value' --output text

# Bastion Private IP
aws ssm get-parameter --name /shared/bastion/private-ip --region ap-northeast-2 --query 'Parameter.Value' --output text
```

### 현재 Bastion 정보
- **Instance ID**: `i-0a8162214660b8814`
- **Name**: `prod-bastion`
- **Private IP**: `10.0.10.141`
- **Instance Type**: `t3.nano`
- **OS**: Amazon Linux 2023

## 포트 포워딩 설정

### 자동 스크립트 사용 (권장)
```bash
cd /Users/sangwon-ryu/fileflow/local-dev

# 터미널 1: 포트 포워딩 시작
./scripts/aws-port-forward.sh

# 출력 예시:
# ✅ Bastion Host 발견: i-0a8162214660b8814
# ✅ RDS 엔드포인트: prod-shared-mysql.cfacertspqbw.ap-northeast-2.rds.amazonaws.com
# ✅ Redis 엔드포인트: fileflow-redis-prod.j9czrc.0001.apn2.cache.amazonaws.com
# ✅ RDS 포트 포워딩 시작
# ✅ Redis 포트 포워딩 시작
```

### 수동 포트 포워딩
```bash
# RDS 포트 포워딩 (localhost:13307 -> RDS:3306)
aws ssm start-session \
  --target i-0a8162214660b8814 \
  --region ap-northeast-2 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["prod-shared-mysql.cfacertspqbw.ap-northeast-2.rds.amazonaws.com"],"portNumber":["3306"],"localPortNumber":["13307"]}'

# Redis 포트 포워딩 (localhost:16380 -> Redis:6379)
aws ssm start-session \
  --target i-0a8162214660b8814 \
  --region ap-northeast-2 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["fileflow-redis-prod.j9czrc.0001.apn2.cache.amazonaws.com"],"portNumber":["6379"],"localPortNumber":["16380"]}'
```

## Docker Compose 실행

### 1. 환경 변수 확인
```bash
cat .env.aws
```

**필수 환경 변수:**
- `AWS_REGION`: ap-northeast-2
- `AWS_DB_NAME`: fileflow
- `AWS_DB_USER`: admin
- `AWS_DB_PASSWORD`: E[&mUlOgA+ucv31nRmSDlbOr398VyGep
- `AWS_S3_BUCKET`: fileflow-uploads-prod

### 2. Docker Compose 시작
```bash
# 터미널 2 (포트 포워딩이 실행 중인 상태에서)
cd /Users/sangwon-ryu/fileflow/local-dev

# 서비스 시작
docker-compose --env-file .env.aws -f docker-compose.aws.yml up -d

# 로그 확인
docker-compose -f docker-compose.aws.yml logs -f

# 특정 서비스 로그만 확인
docker-compose -f docker-compose.aws.yml logs -f web-api
```

### 3. 서비스 접속 확인
```bash
# Web API
curl http://localhost:8080/actuator/health

# Scheduler
curl http://localhost:8081/actuator/health

# Download Worker
curl http://localhost:8082/actuator/health
```

### 4. 서비스 종료
```bash
# Docker Compose 종료
docker-compose -f docker-compose.aws.yml down

# 포트 포워딩 종료 (터미널 1에서 Ctrl+C)
```

## 연결 테스트

### RDS 연결 테스트
```bash
# MySQL 클라이언트로 직접 연결
mysql -h 127.0.0.1 -P 13307 -u admin -p
# Password: E[&mUlOgA+ucv31nRmSDlbOr398VyGep

# 연결 후
mysql> USE fileflow;
mysql> SHOW TABLES;
mysql> SELECT COUNT(*) FROM your_table;
```

### Redis 연결 테스트
```bash
# Redis 클라이언트로 직접 연결
redis-cli -h 127.0.0.1 -p 16380

# 연결 후
127.0.0.1:16380> PING
PONG
127.0.0.1:16380> INFO
127.0.0.1:16380> KEYS *
```

## Terraform으로 Bastion 정보 확인

### fileflow terraform에서 Bastion 참조
```bash
cd /Users/sangwon-ryu/fileflow/terraform

# Bastion 출력 확인
terraform output bastion_instance_id
terraform output bastion_connection_command
```

### Infrastructure terraform에서 Bastion 관리
```bash
cd /Users/sangwon-ryu/infrastructure/terraform/environments/prod/network

# Bastion 상태 확인
terraform output bastion_instance_id
terraform output bastion_security_group_id
terraform output bastion_private_ip

# Bastion 활성화/비활성화
# variables.tf 또는 terraform.tfvars에서
# enable_bastion = true/false 설정 후
terraform apply
```

## 트러블슈팅

### Session Manager Plugin 설치 확인
```bash
session-manager-plugin --version
# SessionManagerPlugin/1.2.xyz
```

### AWS 자격 증명 문제
```bash
# 자격 증명 확인
aws sts get-caller-identity

# 자격 증명 재설정
aws configure
```

### 포트 포워딩 실패
```bash
# Bastion 상태 확인
aws ec2 describe-instances --instance-ids i-0a8162214660b8814 --region ap-northeast-2 --query 'Reservations[0].Instances[0].State.Name'

# SSM Agent 상태 확인
aws ssm describe-instance-information --instance-ids i-0a8162214660b8814 --region ap-northeast-2

# 포트 사용 확인
lsof -i :13307  # RDS 포트
lsof -i :16380  # Redis 포트
```

### Docker Compose 연결 실패
```bash
# 포트 포워딩이 실행 중인지 확인
ps aux | grep "aws ssm start-session"

# Docker 로그 확인
docker-compose -f docker-compose.aws.yml logs web-api

# 네트워크 연결 테스트
docker-compose -f docker-compose.aws.yml exec web-api nc -zv host.docker.internal 13307
docker-compose -f docker-compose.aws.yml exec web-api nc -zv host.docker.internal 16380
```

### Bastion 재시작
```bash
# Bastion 인스턴스 재시작
aws ec2 reboot-instances --instance-ids i-0a8162214660b8814 --region ap-northeast-2

# 재시작 후 상태 확인 (1-2분 소요)
aws ssm describe-instance-information --instance-ids i-0a8162214660b8814 --region ap-northeast-2
```

## 보안 주의사항

1. **자격 증명 관리**
   - `.env.aws` 파일을 절대 Git에 커밋하지 마세요
   - AWS SSO 사용을 권장합니다 (임시 자격 증명)
   - IAM 사용자는 최소 권한 원칙을 적용하세요

2. **프로덕션 데이터**
   - 로컬 개발 시 프로덕션 데이터 수정에 주의하세요
   - 가능하면 읽기 전용 계정을 사용하세요
   - 중요한 작업은 별도 승인 후 진행하세요

3. **네트워크 보안**
   - 포트 포워딩은 로컬 개발 용도로만 사용하세요
   - 불필요한 포트 포워딩은 즉시 종료하세요
   - 공용 WiFi에서는 VPN 사용을 권장합니다

## 참고 자료

- [AWS SSM Session Manager 문서](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [AWS SSM Port Forwarding](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-start-port-forwarding)
- [Session Manager Plugin 설치](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html)
