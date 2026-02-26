#!/bin/bash

# ===============================================
# Stage Redis 차단 IP 정리 스크립트
# ===============================================
# Stage Redis에서 gateway:blocked_ip:* 키를 조회하고 삭제합니다.
# 사전 조건: aws-port-forward-stage.sh로 Redis 포트 포워딩이 활성화되어 있어야 합니다.
# ===============================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Stage Redis 포트 (Prod: 16381, Stage: 16382)
REDIS_HOST="127.0.0.1"
REDIS_PORT="16382"
BLOCKED_IP_PATTERN="gateway:blocked_ip:*"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Stage Redis 차단 IP 정리${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# redis-cli 설치 확인
if ! command -v redis-cli &> /dev/null; then
    echo -e "${RED}redis-cli가 설치되어 있지 않습니다.${NC}"
    echo "설치 방법:"
    echo "  macOS: brew install redis"
    echo "  Ubuntu: sudo apt-get install redis-tools"
    exit 1
fi

echo -e "${GREEN}redis-cli 확인 완료${NC}"

# Stage Redis 포트 포워딩 연결 확인
echo "Stage Redis 포트 포워딩 확인 중 (${REDIS_HOST}:${REDIS_PORT})..."
if ! redis-cli -h $REDIS_HOST -p $REDIS_PORT PING &> /dev/null; then
    echo -e "${RED}Stage Redis에 연결할 수 없습니다 (${REDIS_HOST}:${REDIS_PORT}).${NC}"
    echo ""
    echo -e "${YELLOW}포트 포워딩이 활성화되어 있는지 확인하세요:${NC}"
    echo "  ./local-dev/scripts/aws-port-forward-stage.sh"
    exit 1
fi

echo -e "${GREEN}Stage Redis 연결 확인 완료${NC}"
echo ""

# 차단된 IP 키 조회
echo -e "${YELLOW}차단된 IP 목록 조회 중...${NC}"
echo ""

BLOCKED_KEYS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT KEYS "$BLOCKED_IP_PATTERN" 2>/dev/null)

if [ -z "$BLOCKED_KEYS" ]; then
    echo -e "${GREEN}차단된 IP가 없습니다.${NC}"
    exit 0
fi

# 키 목록을 배열로 변환
KEY_ARRAY=()
while IFS= read -r key; do
    if [ -n "$key" ]; then
        KEY_ARRAY+=("$key")
    fi
done <<< "$BLOCKED_KEYS"

KEY_COUNT=${#KEY_ARRAY[@]}

echo -e "${YELLOW}차단된 IP ${KEY_COUNT}개 발견:${NC}"
echo -e "${BLUE}────────────────────────────────────────${NC}"

for key in "${KEY_ARRAY[@]}"; do
    # gateway:blocked_ip:xxx.xxx.xxx.xxx 에서 IP 부분 추출
    IP="${key#gateway:blocked_ip:}"
    TTL=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT TTL "$key" 2>/dev/null)
    echo -e "   ${RED}${IP}${NC}  (TTL: ${TTL}s)"
done

echo -e "${BLUE}────────────────────────────────────────${NC}"
echo ""

# 삭제 확인
echo -e "${YELLOW}위의 차단된 IP ${KEY_COUNT}개를 모두 삭제하시겠습니까? (y/N)${NC}"
read -r CONFIRM

if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo -e "${YELLOW}작업이 취소되었습니다.${NC}"
    exit 0
fi

echo ""
echo "차단된 IP 키 삭제 중..."

# 모든 blocked_ip 키 삭제
DELETED_COUNT=0
for key in "${KEY_ARRAY[@]}"; do
    redis-cli -h $REDIS_HOST -p $REDIS_PORT DEL "$key" > /dev/null 2>&1
    IP="${key#gateway:blocked_ip:}"
    echo -e "   ${GREEN}삭제 완료: ${IP}${NC}"
    DELETED_COUNT=$((DELETED_COUNT + 1))
done

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}총 ${DELETED_COUNT}개의 차단 IP가 삭제되었습니다.${NC}"
echo -e "${BLUE}========================================${NC}"
