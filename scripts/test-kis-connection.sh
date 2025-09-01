#!/bin/bash

# KIS API 연결 테스트 스크립트 (macOS 호환)
# 사용법: ./scripts/test-kis-connection.sh

set -e

echo "🔍 KIS API 연결 테스트 스크립트"
echo "=============================="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 환경변수 로드
if [ -f ".env" ]; then
    source .env
    echo -e "${GREEN}✅ .env 파일 로드 완료${NC}"
else
    echo -e "${RED}❌ .env 파일이 없습니다. 먼저 setup-kis-api.sh를 실행하세요.${NC}"
    exit 1
fi

# 필수 환경변수 확인
if [ -z "$KIS_APP_KEY" ] || [ -z "$KIS_APP_SECRET" ]; then
    echo -e "${RED}❌ KIS API 키가 설정되지 않았습니다.${NC}"
    echo "KIS_APP_KEY: $KIS_APP_KEY"
    echo "KIS_APP_SECRET: $KIS_APP_SECRET"
    exit 1
fi

echo -e "${BLUE}📋 설정 정보:${NC}"
echo "  KIS_APP_KEY: ${KIS_APP_KEY:0:10}..."
echo "  KIS_APP_SECRET: ${KIS_APP_SECRET:0:10}..."
echo "  KIS_WEBSOCKET_URL: $KIS_WEBSOCKET_URL"
echo ""

# Step 1: 네트워크 연결 테스트 (macOS 호환)
echo -e "${BLUE}1️⃣ 네트워크 연결 테스트${NC}"
echo "================================"

# WebSocket URL에서 호스트와 포트 추출 (ws:// 및 wss:// 모두 지원)
HOST=$(echo $KIS_WEBSOCKET_URL | sed -n 's|.*://\([^:]*\):.*|\1|p')
PORT=$(echo $KIS_WEBSOCKET_URL | sed -n 's|.*:\([0-9]*\)|\1|p')

echo "호스트: $HOST"
echo "포트: $PORT"

# macOS 호환 네트워크 연결 테스트
if command -v nc > /dev/null 2>&1; then
    if nc -z -w5 $HOST $PORT 2>/dev/null; then
        echo -e "${GREEN}✅ 네트워크 연결 성공${NC}"
    else
        echo -e "${RED}❌ 네트워크 연결 실패${NC}"
        exit 1
    fi
else
    # nc가 없으면 curl로 테스트
    if curl -s --connect-timeout 5 "https://$HOST:$PORT" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 네트워크 연결 성공 (curl 사용)${NC}"
    else
        echo -e "${YELLOW}⚠️  네트워크 연결 테스트 건너뜀 (curl 연결 확인 불가)${NC}"
    fi
fi
echo ""

# Step 2: SSL 인증서 확인
echo -e "${BLUE}2️⃣ SSL 인증서 확인${NC}"
echo "======================"

# WebSocket이 ws:// (HTTP) 프로토콜을 사용하는 경우 SSL 확인 건너뛰기
if [[ $KIS_WEBSOCKET_URL == ws://* ]]; then
    echo -e "${YELLOW}⚠️  WebSocket이 HTTP 프로토콜을 사용하므로 SSL 확인을 건너뜁니다${NC}"
else
    # HTTPS WebSocket의 경우에만 SSL 확인
    SSL_INFO=$(timeout 5 openssl s_client -connect $HOST:$PORT -servername $HOST < /dev/null 2>/dev/null | openssl x509 -noout -text 2>/dev/null | grep -E "(Subject:|DNS:)" 2>/dev/null || true)
    
    if [ -n "$SSL_INFO" ]; then
        echo -e "${GREEN}✅ SSL 인증서 확인 성공${NC}"
        echo "$SSL_INFO"
    else
        echo -e "${YELLOW}⚠️  SSL 인증서 확인 실패 (하지만 계속 진행)${NC}"
    fi
fi
echo ""

# Step 3: KIS API 인증 토큰 발급 테스트
echo -e "${BLUE}3️⃣ KIS API 인증 토큰 발급 테스트${NC}"
echo "==============================="

# WebSocket과 REST API는 다른 도메인/포트를 사용
# WebSocket: ws://ops.koreainvestment.com:31000
# REST API: https://openapivts.koreainvestment.com:29443
if [[ $KIS_WEBSOCKET_URL == *"ops.koreainvestment.com"* ]]; then
    # 모의투자 환경
    BASE_URL="https://openapivts.koreainvestment.com:29443"
else
    # 실전투자 환경
    BASE_URL="https://openapi.koreainvestment.com:9443"
fi
TOKEN_URL="$BASE_URL/oauth2/tokenP"

echo "토큰 발급 URL: $TOKEN_URL"

# 마스킹된 정보로 명령어 표시
echo "실행 중인 curl 명령어:"
echo "curl -X POST \"$TOKEN_URL\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"grant_type\": \"client_credentials\", \"appkey\": \"${KIS_APP_KEY:0:10}...\", \"appsecret\": \"[MASKED]\"}'"
echo ""

# HTTP 상태 코드와 응답 바디만 분리해서 가져오기
# 모의투자 환경의 올바른 REST API 포트 사용 (29443)
HTTP_CODE=$(curl -w "%{http_code}" -s -k -X POST "$TOKEN_URL" \
    -H "Content-Type: application/json" \
    -d "{
        \"grant_type\": \"client_credentials\",
        \"appkey\": \"$KIS_APP_KEY\",
        \"appsecret\": \"$KIS_APP_SECRET\"
    }" -o /tmp/token_response.json 2>/dev/null)

TOKEN_RESPONSE=$(cat /tmp/token_response.json 2>/dev/null || echo "{}")

echo "HTTP 상태 코드: $HTTP_CODE"
echo "응답 내용: $TOKEN_RESPONSE"
echo ""

if echo "$TOKEN_RESPONSE" | grep -q "access_token"; then
    echo -e "${GREEN}✅ 인증 토큰 발급 성공${NC}"
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    echo "토큰: ${ACCESS_TOKEN:0:20}..."
else
    echo -e "${RED}❌ 인증 토큰 발급 실패${NC}"
    
    # HTTP 상태 코드별 안내
    case $HTTP_CODE in
        403)
            echo -e "${YELLOW}💡 403 Forbidden: App Key 또는 Secret이 유효하지 않습니다.${NC}"
            echo -e "${YELLOW}   KIS Developers 사이트에서 키 정보를 확인하세요.${NC}"
            ;;
        401)
            echo -e "${YELLOW}💡 401 Unauthorized: 인증 정보가 잘못되었습니다.${NC}"
            ;;
        500)
            echo -e "${YELLOW}💡 500 Internal Server Error: KIS 서버 내부 오류입니다.${NC}"
            ;;
        000)
            echo -e "${YELLOW}💡 연결 실패: 네트워크 또는 SSL 문제일 수 있습니다.${NC}"
            ;;
        *)
            echo -e "${YELLOW}💡 HTTP $HTTP_CODE: 예상치 못한 응답입니다.${NC}"
            ;;
    esac
    
    if [[ -n "$TOKEN_RESPONSE" && "$TOKEN_RESPONSE" != "{}" ]]; then
        echo "서버 응답: $TOKEN_RESPONSE"
    fi
    exit 1
fi
echo ""

# Step 4: WebSocket 승인키 발급 테스트
echo -e "${BLUE}4️⃣ WebSocket 승인키 발급 테스트${NC}"
echo "=============================="

APPROVAL_URL="$BASE_URL/oauth2/Approval"
echo "승인키 발급 URL: $APPROVAL_URL"

echo "실행 중인 curl 명령어:"
echo "curl -X POST \"$APPROVAL_URL\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -H \"authorization: Bearer [TOKEN]\" \\"
echo "  -d '{\"grant_type\": \"client_credentials\", \"appkey\": \"${KIS_APP_KEY:0:10}...\", \"secretkey\": \"[MASKED]\"}'"
echo ""

# HTTP 상태 코드와 응답 바디만 분리해서 가져오기
# 모의투자 환경의 올바른 REST API 포트 사용 (29443)
APPROVAL_HTTP_CODE=$(curl -w "%{http_code}" -s -k -X POST "$APPROVAL_URL" \
    -H "Content-Type: application/json" \
    -H "authorization: Bearer $ACCESS_TOKEN" \
    -d "{
        \"grant_type\": \"client_credentials\",
        \"appkey\": \"$KIS_APP_KEY\",
        \"secretkey\": \"$KIS_APP_SECRET\"
    }" -o /tmp/approval_response.json 2>/dev/null)

APPROVAL_RESPONSE=$(cat /tmp/approval_response.json 2>/dev/null || echo "{}")

echo "HTTP 상태 코드: $APPROVAL_HTTP_CODE"
echo "응답 내용: $APPROVAL_RESPONSE"
echo ""

if echo "$APPROVAL_RESPONSE" | grep -q "approval_key"; then
    echo -e "${GREEN}✅ WebSocket 승인키 발급 성공${NC}"
    APPROVAL_KEY=$(echo "$APPROVAL_RESPONSE" | grep -o '"approval_key":"[^"]*"' | cut -d'"' -f4)
    echo "승인키: ${APPROVAL_KEY:0:20}..."
else
    echo -e "${RED}❌ WebSocket 승인키 발급 실패${NC}"
    
    # HTTP 상태 코드별 안내
    case $APPROVAL_HTTP_CODE in
        403)
            echo -e "${YELLOW}💡 403 Forbidden: Access Token이 유효하지 않거나 권한이 없습니다.${NC}"
            ;;
        401)
            echo -e "${YELLOW}💡 401 Unauthorized: Access Token이 만료되었거나 잘못되었습니다.${NC}"
            ;;
        500)
            echo -e "${YELLOW}💡 500 Internal Server Error: KIS 서버 내부 오류입니다.${NC}"
            ;;
        000)
            echo -e "${YELLOW}💡 연결 실패: 네트워크 또는 SSL 문제일 수 있습니다.${NC}"
            ;;
        *)
            echo -e "${YELLOW}💡 HTTP $APPROVAL_HTTP_CODE: 예상치 못한 응답입니다.${NC}"
            ;;
    esac
    
    if [[ -n "$APPROVAL_RESPONSE" && "$APPROVAL_RESPONSE" != "{}" ]]; then
        echo "서버 응답: $APPROVAL_RESPONSE"
    fi
    exit 1
fi
echo ""

# Step 5: 포트 사용 확인
echo -e "${BLUE}5️⃣ 포트 사용 확인${NC}"
echo "=================="

PORTS=(8081 8082 8083 8084 9092 3306)
for port in "${PORTS[@]}"; do
    if lsof -i :$port > /dev/null 2>&1; then
        echo -e "포트 $port: ${GREEN}✅ 사용 중${NC}"
    else
        echo -e "포트 $port: ${YELLOW}⚠️  사용 안함${NC}"
    fi
done
echo ""

# Step 6: Docker 서비스 확인
echo -e "${BLUE}6️⃣ Docker 서비스 확인${NC}"
echo "===================="

if command -v docker-compose > /dev/null 2>&1; then
    DOCKER_STATUS=$(docker-compose ps --format table 2>/dev/null || echo "ERROR")
    if [ "$DOCKER_STATUS" != "ERROR" ]; then
        echo -e "${GREEN}✅ Docker Compose 서비스 상태:${NC}"
        docker-compose ps
    else
        echo -e "${YELLOW}⚠️  Docker Compose 서비스가 실행되지 않음${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Docker Compose가 설치되지 않음${NC}"
fi
echo ""

# Step 7: Collector 서비스 테스트 시작
echo -e "${BLUE}7️⃣ Collector 서비스 테스트 준비${NC}"
echo "=============================="

# 기존 프로세스 정리
if pgrep -f "collector.*gradlew" > /dev/null; then
    echo "기존 Collector 프로세스 종료 중..."
    pkill -f "collector.*gradlew" || true
    sleep 3
fi

# 포트 8081 사용 확인
if lsof -i :8081 > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  포트 8081이 사용 중입니다. 프로세스를 종료하세요:${NC}"
    lsof -i :8081
    echo ""
    echo "종료 명령어: kill -9 \$(lsof -t -i:8081)"
    exit 1
fi

echo -e "${GREEN}✅ 모든 사전 검사 완료${NC}"
echo ""

# 실행 명령어 안내
echo -e "${BLUE}🚀 Collector 서비스 시작 명령어:${NC}"
echo "================================"
echo "cd collector"
echo "KIS_WEBSOCKET_URL=\"$KIS_WEBSOCKET_URL\" \\"
echo "KIS_APP_KEY=\"$KIS_APP_KEY\" \\"
echo "KIS_APP_SECRET=\"$KIS_APP_SECRET\" \\"
echo "./gradlew bootRun > /tmp/collector-kis-test.log 2>&1 &"
echo ""

echo -e "${BLUE}📊 연결 상태 확인 명령어:${NC}"
echo "========================"
echo "# 실시간 로그 확인"
echo "tail -f /tmp/collector-kis-test.log"
echo ""
echo "# 인증 성공 확인"
echo "grep -E \"(Successfully obtained|access token)\" /tmp/collector-kis-test.log"
echo ""
echo "# WebSocket 연결 확인"
echo "grep -E \"(WebSocket connection opened|connection opened to KIS)\" /tmp/collector-kis-test.log"
echo ""
echo "# 데이터 수신 확인"
echo "grep -E \"(Received message|Successfully processed)\" /tmp/collector-kis-test.log"
echo ""

# 임시 파일 정리
rm -f /tmp/token_response.json /tmp/approval_response.json 2>/dev/null

echo -e "${GREEN}✨ KIS API 연결 사전 테스트 완료!${NC}"
echo "위의 명령어를 사용하여 실제 연결을 시작하세요."
