#!/bin/bash

# --- 모의 투자 환경 실행 스크립트 ---

echo "🚀 모의 투자 환경으로 Collector를 시작합니다..."

# 1. Spring Profile을 'mock-kis'로 설정
export SPRING_PROFILES_ACTIVE=mock-kis

echo "  -> Active Profile: ${SPRING_PROFILES_ACTIVE}"

# 2. collector 디렉터리로 이동 (스크립트 위치에 따라 경로 조절)
cd ./collector || exit 1

# 3. Gradle을 사용하여 애플리케이션 실행
./gradlew bootRun

echo "✅ Collector 실행 완료."
