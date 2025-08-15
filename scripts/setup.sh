#!/bin/bash

# 실시간 주식 시세 스트리밍 시스템 개발 환경 설정 스크립트

echo "🚀 실시간 주식 시세 스트리밍 시스템 개발 환경 설정을 시작합니다..."

# 1. 필수 도구 확인
echo "📋 필수 도구 확인 중..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다. Docker를 설치해주세요."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다. Docker Compose를 설치해주세요."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java가 설치되어 있지 않습니다. Java 21 이상을 설치해주세요."
    exit 1
fi

echo "✅ 필수 도구 확인 완료"

# 2. Gradle Wrapper 설정
echo "📦 Gradle Wrapper 설정 중..."
for service in quote-stream-collector data-processor notification-service sse-streamer; do
    if [ -d "$service" ]; then
        echo "  - $service Gradle Wrapper 설정..."
        cd "$service"
        if [ ! -f "gradlew" ]; then
            gradle wrapper --gradle-version 8.5
        fi
        cd ..
    fi
done

# 3. Docker 네트워크 생성
echo "🌐 Docker 네트워크 생성 중..."
docker network create stock-network 2>/dev/null || echo "  - stock-network 네트워크가 이미 존재합니다."

# 4. 권한 설정
echo "🔐 스크립트 권한 설정 중..."
chmod +x scripts/*.sh 2>/dev/null || true

# 5. 환경 변수 템플릿 생성
echo "⚙️ 환경 변수 템플릿 생성 중..."
if [ ! -f ".env" ]; then
    cp docker-compose.env.example .env
    echo "  - .env 파일이 생성되었습니다."
else
    echo "  - .env 파일이 이미 존재합니다."
fi

echo ""
echo "🎉 개발 환경 설정이 완료되었습니다!"
echo ""
echo "📚 다음 단계:"
echo "  1. .env 파일을 확인하고 필요시 수정하세요"
echo "  2. './scripts/docker-init.sh' 명령으로 Docker 환경을 초기화하세요"
echo "  3. 각 서비스를 개별적으로 빌드하고 실행하세요"
echo ""
echo "🔍 유용한 명령어:"
echo "  - Docker 환경 초기화: ./scripts/docker-init.sh"
echo "  - 전체 서비스 시작: docker-compose up"
echo "  - 인프라만 시작: docker-compose up -d kafka mysql"
echo "  - 로그 확인: docker-compose logs -f [service-name]"
echo "  - 전체 중지: docker-compose down"