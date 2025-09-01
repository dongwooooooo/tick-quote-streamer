# 🚀 Quick Start - KIS API 연결 테스트

## 📝 빠른 시작 가이드

### 1️⃣ KIS API 키 설정
```bash
# 환경변수 파일 생성
cp docker-compose.env.example .env

# .env 파일 편집하여 KIS API 키 입력
# KIS_APP_KEY=발급받은_App_Key
# KIS_APP_SECRET=발급받은_App_Secret
```

### 2️⃣ 자동 연결 테스트
```bash
# KIS API 연결 사전 테스트 (인증, 네트워크 등)
./scripts/test-kis-connection.sh
```

### 3️⃣ 실제 연결 시작
```bash
# Docker 서비스 시작
docker-compose up -d

# Collector 서비스 시작 (실제 KIS API)
cd collector
source ../.env
KIS_WEBSOCKET_URL="$KIS_WEBSOCKET_URL" \
KIS_APP_KEY="$KIS_APP_KEY" \
KIS_APP_SECRET="$KIS_APP_SECRET" \
./gradlew bootRun > /tmp/collector-test.log 2>&1 &
```

### 4️⃣ 연결 상태 확인
```bash
# 실시간 로그 확인
tail -f /tmp/collector-test.log

# 인증 성공 확인
grep "Successfully obtained" /tmp/collector-test.log

# WebSocket 연결 확인
grep "WebSocket connection opened" /tmp/collector-test.log

# 데이터 수신 확인
grep "Received message" /tmp/collector-test.log
```

### 5️⃣ 데이터 확인
```bash
# Kafka 메시지 확인
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic quote-stream \
  --from-beginning \
  --max-messages 5

# 데이터베이스 저장 확인
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT stock_code, price, volume, created_at FROM quote_data ORDER BY created_at DESC LIMIT 5;"

# SSE 스트리밍 테스트
curl -N http://localhost:8083/api/sse/quotes/005930
```

## 🔧 문제 해결

### 인증 실패 (403 Forbidden)
```bash
# App Key/Secret 확인
echo "KIS_APP_KEY: $KIS_APP_KEY"
echo "KIS_APP_SECRET: $KIS_APP_SECRET"

# 수동 인증 테스트
curl -X POST "https://openapivts.koreainvestment.com:9443/oauth2/tokenP" \
  -H "Content-Type: application/json" \
  -d "{\"grant_type\": \"client_credentials\", \"appkey\": \"$KIS_APP_KEY\", \"appsecret\": \"$KIS_APP_SECRET\"}"
```

### 포트 충돌
```bash
# 포트 사용 프로세스 확인
lsof -i :8081

# 프로세스 종료
kill -9 $(lsof -t -i:8081)
```

### SSL 연결 오류
```bash
# SSL 인증서 확인
openssl s_client -connect openapivts.koreainvestment.com:9443 -servername openapivts.koreainvestment.com < /dev/null
```

## 📊 모니터링 URL

- **Kafka UI**: http://localhost:8080
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Collector Health**: http://localhost:8081/actuator/health
- **Data Processor Health**: http://localhost:8082/actuator/health
- **SSE Streamer Health**: http://localhost:8083/actuator/health

## 📚 상세 문서

자세한 설명은 [`docs/kis-api-testing-guide.md`](./kis-api-testing-guide.md)를 참조하세요.

---

**성공적인 연결을 위해 단계별로 진행하세요! 🎯**


