#!/bin/bash

# ===============================================
# 로컬 개발 환경 종료 스크립트
# ===============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}FileFlow 로컬 개발 환경 종료${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# 스크립트 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

# 옵션 확인
REMOVE_VOLUMES=false
if [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    REMOVE_VOLUMES=true
    echo -e "${RED}⚠️  데이터 볼륨도 함께 삭제됩니다!${NC}"
    echo ""
fi

# Docker Compose 종료
if [ "$REMOVE_VOLUMES" = true ]; then
    echo "컨테이너 및 볼륨 삭제 중..."
    docker-compose -f docker-compose.local.yml down -v
else
    echo "컨테이너 종료 중 (데이터 유지)..."
    docker-compose -f docker-compose.local.yml down
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}로컬 개발 환경 종료 완료${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

if [ "$REMOVE_VOLUMES" = false ]; then
    echo "💡 데이터는 보존되었습니다."
    echo "   데이터까지 삭제하려면: ./scripts/local-stop.sh --clean"
    echo ""
fi
