#!/bin/bash

# ===============================================
# AWS 연결 개발 환경 종료 스크립트
# ===============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}FileFlow AWS 연결 환경 종료${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# 스크립트 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

# Docker Compose 종료
echo "컨테이너 종료 중..."
docker-compose -f docker-compose.aws.yml down

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}AWS 연결 환경 종료 완료${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "💡 포트 포워딩도 종료하려면:"
echo "   포트 포워딩 터미널에서 Ctrl+C를 누르세요."
echo ""
