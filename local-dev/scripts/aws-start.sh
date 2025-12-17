#!/bin/bash

# ===============================================
# AWS 연결 개발 환경 시작 스크립트
# ===============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}FileFlow AWS 연결 환경 시작${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 스크립트 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

# .env.aws 파일 확인 (필수)
if [ ! -f ".env.aws" ]; then
    echo -e "${RED}❌ .env.aws 파일이 없습니다!${NC}"
    echo ""
    echo "다음 단계를 수행하세요:"
    echo "  1. cp .env.aws.example .env.aws"
    echo "  2. vim .env.aws  # AWS 자격 증명 입력"
    echo "  3. ./scripts/aws-start.sh"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ .env.aws 파일 발견${NC}"
echo ""

# 포트 포워딩 확인
echo "포트 포워딩 상태 확인 중..."
if ! lsof -i :13307 > /dev/null 2>&1; then
    echo -e "${RED}❌ RDS 포트 포워딩이 활성화되지 않았습니다!${NC}"
    echo ""
    echo "다른 터미널에서 먼저 실행하세요:"
    echo "  ./scripts/aws-port-forward.sh"
    echo ""
    exit 1
fi

if ! lsof -i :16380 > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Redis 포트 포워딩이 활성화되지 않았습니다.${NC}"
    echo "   ElastiCache를 사용하지 않는다면 무시하세요."
    echo ""
fi

echo -e "${GREEN}✅ 포트 포워딩 활성화 확인${NC}"
echo ""

# AWS 자격 증명 확인 (파일 존재만 체크, docker-compose가 읽음)
echo "AWS 자격 증명 확인 중..."
if ! grep -q "AWS_ACCESS_KEY_ID" .env.aws; then
    echo -e "${RED}❌ .env.aws 파일에 AWS_ACCESS_KEY_ID가 없습니다!${NC}"
    echo "   .env.aws 파일을 확인하세요."
    exit 1
fi

echo -e "${GREEN}✅ .env.aws 파일 확인 완료${NC}"
echo ""

# Docker Compose 실행
echo "Docker Compose 시작 중..."
docker-compose --env-file .env.aws -f docker-compose.aws.yml up -d

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}AWS 연결 환경 시작 완료${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "서비스 상태 확인:"
echo "  - Web API:        http://localhost:8080/actuator/health"
echo "  - Scheduler:      http://localhost:8081/actuator/health"
echo "  - Download Worker: http://localhost:8082/actuator/health"
echo ""
echo "연결된 AWS 리소스:"
echo "  - RDS:         localhost:13307 (포워딩)"
echo "  - ElastiCache: localhost:16380 (포워딩)"
echo "  - S3:          실제 AWS S3"
echo "  - SQS:         실제 AWS SQS"
echo ""
echo -e "${RED}⚠️  주의: 프로덕션 데이터에 연결되어 있습니다!${NC}"
echo ""
echo "로그 확인:"
echo "  docker-compose -f docker-compose.aws.yml logs -f"
echo ""
echo "종료:"
echo "  ./scripts/aws-stop.sh"
echo "  포트 포워딩 터미널에서 Ctrl+C"
echo ""
