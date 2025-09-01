#!/bin/bash

# KIS API 설정 스크립트
# 실제 KIS API 연동을 위한 환경 변수 설정을 도와주는 스크립트

set -e

echo "🔑 KIS API 설정 스크립트"
echo "=========================="

# 현재 .env 파일 확인
if [ ! -f ".env" ]; then
    echo "📝 .env 파일이 없습니다. docker-compose.env.example을 복사합니다..."
    cp docker-compose.env.example .env
    echo "✅ .env 파일이 생성되었습니다."
fi

echo ""
echo "현재 KIS API 설정:"
echo "==================="
grep -E "^KIS_" .env || echo "KIS 설정이 없습니다."

echo ""
echo "🔧 KIS API 설정을 업데이트하시겠습니까? (y/n)"
read -r update_choice

if [ "$update_choice" = "y" ] || [ "$update_choice" = "Y" ]; then
    echo ""
    echo "📋 KIS API 정보를 입력해주세요:"
    echo "(한국투자증권에서 발급받은 정보만 입력하면 됩니다)"
    
    # App Key 입력
    echo -n "App Key: "
    read -r app_key
    
    # App Secret 입력 (보안을 위해 숨김)
    echo -n "App Secret: "
    read -s app_secret
    echo ""
    
    # 환경 선택
    echo ""
    echo "🌐 사용할 환경을 선택하세요:"
    echo "1) 모의투자 환경 (추천)"
    echo "2) 실제 투자 환경 (주의!)"
    echo "3) 개발 테스트 환경 (Mock 서버)"
    echo -n "선택 (1-3): "
    read -r env_choice
    
    case $env_choice in
        1)
            websocket_url="wss://openapivts.koreainvestment.com:9443"
            env_name="모의투자"
            ;;
        2)
            websocket_url="wss://openapi.koreainvestment.com:9443"
            env_name="실제 투자"
            echo "⚠️  경고: 실제 투자 환경을 선택했습니다. 실제 거래가 발생할 수 있습니다!"
            ;;
        3)
            websocket_url="ws://localhost:8090/kis-mock"
            env_name="개발 테스트"
            ;;
        *)
            echo "❌ 잘못된 선택입니다. 모의투자 환경으로 설정됩니다."
            websocket_url="wss://openapivts.koreainvestment.com:9443"
            env_name="모의투자"
            ;;
    esac
    
    # .env 파일 업데이트
    echo ""
    echo "📝 .env 파일을 업데이트합니다..."
    
    # KIS 관련 설정 업데이트
    sed -i.bak \
        -e "s|^KIS_APP_KEY=.*|KIS_APP_KEY=$app_key|" \
        -e "s|^KIS_APP_SECRET=.*|KIS_APP_SECRET=$app_secret|" \
        -e "s|^KIS_WEBSOCKET_URL=.*|KIS_WEBSOCKET_URL=$websocket_url|" \
        .env
    
    echo "✅ 설정이 완료되었습니다!"
    echo ""
    echo "🔧 설정된 환경: $env_name"
    echo "📡 WebSocket URL: $websocket_url"
    echo ""
fi

echo "🚀 다음 단계:"
echo "============="

if grep -q "ws://localhost:8090" .env; then
    echo "1. Mock WebSocket 서버 시작:"
    echo "   cd mock-websocket && ./gradlew bootRun &"
    echo ""
fi

echo "2. Collector 서비스 재시작:"
echo "   # 기존 프로세스 종료"
echo "   pkill -f 'gradlew bootRun'"
echo "   # Collector 시작"
echo "   cd collector && ./gradlew bootRun &"
echo ""

echo "3. 연결 상태 확인:"
echo "   tail -f /tmp/collector.log"
echo "   curl http://localhost:8081/actuator/health"
echo ""

echo "4. Kafka 메시지 확인:"
echo "   # Kafka UI: http://localhost:8080"
echo "   # 또는 CLI:"
echo "   docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic quote-stream --from-beginning"
echo ""

echo "📚 자세한 설정 가이드: docs/kis-api-setup.md"
echo ""
echo "✨ 설정 완료!"
