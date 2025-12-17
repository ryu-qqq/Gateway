#!/bin/bash

# ===============================================
# 로컬 개발 환경 시작 스크립트
# ===============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}FileFlow 로컬 개발 환경 시작${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 스크립트 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

# .env.local 파일 확인
if [ ! -f ".env.local" ]; then
    echo -e "${YELLOW}⚠️  .env.local 파일이 없습니다. 기본값으로 시작합니다.${NC}"
    echo -e "${YELLOW}   커스터마이징하려면: cp .env.local.example .env.local${NC}"
    echo ""
    ENV_FILE=""
else
    echo -e "${GREEN}✅ .env.local 파일 발견${NC}"
    ENV_FILE="--env-file .env.local"
fi

# Docker Compose 실행
echo "Docker Compose 시작 중..."
docker-compose $ENV_FILE -f docker-compose.local.yml up -d

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}로컬 개발 환경 시작 완료${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "서비스 상태 확인:"
echo "  - Web API:        http://localhost:8080/actuator/health"
echo "  - Scheduler:      http://localhost:8081/actuator/health"
echo "  - Download Worker: http://localhost:8082/actuator/health"
echo ""
echo "데이터베이스 접속:"
echo "  - MySQL:  mysql -h 127.0.0.1 -P 13306 -u root -p"
echo "  - Redis:  redis-cli -h 127.0.0.1 -p 16379"
echo ""
echo "로그 확인:"
echo "  docker-compose -f docker-compose.local.yml logs -f"
echo ""
echo "종료:"
echo "  ./scripts/local-stop.sh"
echo ""
