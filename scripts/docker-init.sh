#!/bin/bash

# Docker 환경 초기화 스크립트

echo "🐳 Docker 환경 초기화를 시작합니다..."

# 1. 환경 변수 파일 설정
if [ ! -f ".env" ]; then
    echo "📋 환경 변수 파일 생성 중..."
    cp docker-compose.env.example .env
    echo "  - .env 파일이 생성되었습니다."
else
    echo "  - .env 파일이 이미 존재합니다."
fi

# 2. Docker 네트워크 생성
echo "🌐 Docker 네트워크 생성 중..."
docker network create stock-network 2>/dev/null || echo "  - stock-network 네트워크가 이미 존재합니다."

# 3. 기존 컨테이너 정리 (선택사항)
echo "🧹 기존 컨테이너 정리 (필요시)..."
read -p "기존 컨테이너를 정리하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker-compose down -v
    echo "  - 기존 컨테이너 및 볼륨이 정리되었습니다."
fi

# 4. 인프라 서비스 시작 (Kafka, MySQL)
echo "🚀 인프라 서비스 시작 중..."
docker-compose up -d zookeeper kafka mysql

# 5. 서비스 상태 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "📊 서비스 상태 확인:"
docker-compose ps

echo ""
echo "✅ Docker 환경 초기화가 완료되었습니다!"
echo ""
echo "📚 다음 단계:"
echo "  1. 인프라 서비스 상태 확인: docker-compose logs kafka mysql"
echo "  2. Kafka UI 접속: http://localhost:8080 (monitoring 프로필 사용시)"
echo "  3. MySQL 접속: mysql -h localhost -u stock_user -p (비밀번호: stock_pass)"
echo "  4. 애플리케이션 서비스 시작: docker-compose up quote-stream-collector data-processor"
echo ""
echo "🔍 유용한 명령어:"
echo "  - 전체 서비스 시작: docker-compose up"
echo "  - 특정 서비스 시작: docker-compose up [service-name]"
echo "  - 로그 확인: docker-compose logs -f [service-name]"
echo "  - 서비스 중지: docker-compose stop"
echo "  - 전체 정리: docker-compose down -v"