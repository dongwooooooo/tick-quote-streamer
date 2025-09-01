# KIS API 직접 연결 테스트 가이드

## 📋 개요

이 문서는 한국투자증권 KIS Open API에 직접 연결하여 실시간 주식 데이터를 수집하는 방법을 안내합니다.

## 🔑 1. KIS API 키 발급

### 1.1 한국투자증권 계좌 개설
- [한국투자증권 홈페이지](https://securities.koreainvestment.com/)에서 계좌 개설
- 모의투자 계좌도 가능 (실제 거래 없이 API 테스트 가능)

### 1.2 KIS Developers 가입
1. [KIS Developers](https://apiportal.koreainvestment.com/) 접속
2. 회원가입 및 로그인
3. **API 신청** 메뉴에서 서비스 신청

### 1.3 App Key/Secret 발급
1. **API 신청** → **앱 등록** 
2. 앱 정보 입력:
   - **앱 이름**: `tick-quote-streamer` (또는 원하는 이름)
   - **서비스 구분**: `웹서비스`
   - **사용목적**: `개인투자용`
3. 발급된 **App Key**와 **App Secret** 저장

## 🛠️ 2. 환경 설정

### 2.1 환경변수 설정
```bash
# .env 파일 생성 (프로젝트 루트)
cp docker-compose.env.example .env

# .env 파일 편집
vim .env
```

### 2.2 KIS API 정보 입력
```bash
# KIS API 설정 (발급받은 정보로 교체)
KIS_APP_KEY=발급받은_App_Key
KIS_APP_SECRET=발급받은_App_Secret

# WebSocket URL 설정
# 모의투자환경 (추천)
KIS_WEBSOCKET_URL=wss://openapivts.koreainvestment.com:9443

# 실제투자환경 (주의!)
# KIS_WEBSOCKET_URL=wss://openapi.koreainvestment.com:9443
```

### 2.3 환경변수 자동 설정 스크립트
```bash
# 대화식 설정 스크립트 실행
./scripts/setup-kis-api.sh
```

## 🧪 3. 연결 테스트

### 3.1 단계별 테스트

#### Step 1: 기본 환경 확인
```bash
# Docker 서비스 시작
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 포트 사용 확인
lsof -i :8081 :8082 :8083 :8084 :9092 :3306
```

#### Step 2: KIS API 인증 테스트
```bash
# 환경변수 로드
source .env

# 인증 토큰 발급 테스트 (cURL)
curl -X POST "https://openapivts.koreainvestment.com:9443/oauth2/tokenP" \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "client_credentials",
    "appkey": "'$KIS_APP_KEY'",
    "appsecret": "'$KIS_APP_SECRET'"
  }'
```

**성공 응답 예시:**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

**실패 응답 예시:**
```json
{
  "error_description": "유효하지 않은 AppKey입니다.",
  "error_code": "EGW00103"
}
```

#### Step 3: WebSocket 승인키 발급 테스트
```bash
# 먼저 access_token 획득 (위 결과에서 복사)
ACCESS_TOKEN="발급받은_토큰"

# WebSocket 승인키 발급
curl -X POST "https://openapivts.koreainvestment.com:9443/oauth2/Approval" \
  -H "Content-Type: application/json" \
  -H "authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "grant_type": "client_credentials",
    "appkey": "'$KIS_APP_KEY'",
    "secretkey": "'$KIS_APP_SECRET'"
  }'
```

**성공 응답 예시:**
```json
{
  "approval_key": "f7a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5"
}
```

### 3.2 Collector 서비스 실행 및 테스트

#### Step 1: Collector 서비스 시작
```bash
# 기존 프로세스 종료
pkill -f "collector.*gradlew"

# 포트 정리 확인
lsof -i :8081 || echo "포트 8081 사용 가능"

# Collector 시작 (실제 KIS API)
cd collector
KIS_WEBSOCKET_URL="wss://openapivts.koreainvestment.com:9443" \
KIS_APP_KEY="$KIS_APP_KEY" \
KIS_APP_SECRET="$KIS_APP_SECRET" \
./gradlew bootRun > /tmp/collector-kis-test.log 2>&1 &

# 로그 실시간 확인
tail -f /tmp/collector-kis-test.log
```

#### Step 2: 연결 상태 확인
```bash
# 서비스 헬스 체크
curl http://localhost:8081/actuator/health

# 인증 성공 확인
grep -E "(Successfully obtained|access token)" /tmp/collector-kis-test.log

# WebSocket 연결 확인
grep -E "(WebSocket connection opened|connection opened to KIS)" /tmp/collector-kis-test.log

# 데이터 수신 확인
grep -E "(Received message|Successfully processed)" /tmp/collector-kis-test.log
```

#### Step 3: Kafka 메시지 확인
```bash
# Kafka 토픽에 메시지 도착 확인
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic quote-stream \
  --from-beginning \
  --max-messages 5

# 또는 Kafka UI 사용
open http://localhost:8080
```

## 🔧 4. 문제 해결 (Troubleshooting)

### 4.1 일반적인 오류와 해결책

#### 인증 오류 (403 Forbidden)
```bash
# 오류: "유효하지 않은 AppKey입니다"
# 해결: App Key/Secret 재확인
echo "현재 설정:"
echo "KIS_APP_KEY: $KIS_APP_KEY"
echo "KIS_APP_SECRET: $KIS_APP_SECRET"

# 공백이나 특수문자 확인
echo -n "$KIS_APP_KEY" | wc -c
echo -n "$KIS_APP_SECRET" | wc -c
```

#### SSL 연결 오류
```bash
# 오류: SSL handshake failed
# 해결: 호스트명 검증 우회 확인
grep "SSL hostname verification" /tmp/collector-kis-test.log

# Java 버전 확인 (Java 11+ 필요)
java -version
```

#### WebSocket 연결 실패
```bash
# 연결 시도 확인
grep -E "(Connecting to KIS|WebSocket)" /tmp/collector-kis-test.log

# 재연결 로직 확인
grep -E "(reconnect|Attempting to)" /tmp/collector-kis-test.log
```

#### 포트 충돌 오류
```bash
# 사용 중인 포트 확인
lsof -i :8081

# 프로세스 종료
kill -9 $(lsof -t -i:8081)
```

### 4.2 로그 레벨 조정
```bash
# 디버그 로그 활성화
cd collector
KIS_WEBSOCKET_URL="wss://openapivts.koreainvestment.com:9443" \
./gradlew bootRun --args="--logging.level.org.example.collector=DEBUG" > /tmp/collector-debug.log 2>&1 &
```

### 4.3 네트워크 연결 테스트
```bash
# KIS API 서버 연결 테스트
telnet openapivts.koreainvestment.com 9443

# SSL 인증서 확인
openssl s_client -connect openapivts.koreainvestment.com:9443 \
  -servername openapivts.koreainvestment.com < /dev/null

# DNS 확인
nslookup openapivts.koreainvestment.com
```

## 📊 5. 데이터 확인

### 5.1 실시간 데이터 모니터링
```bash
# Collector → Kafka 전송 확인
grep "Quote message sent successfully" /tmp/collector-kis-test.log | tail -5

# Data Processor 소비 확인
docker logs data-processor --tail 10

# 데이터베이스 저장 확인
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT stock_code, price, volume, created_at FROM quote_data ORDER BY created_at DESC LIMIT 5;"
```

### 5.2 성능 메트릭 확인
```bash
# Prometheus 메트릭
curl http://localhost:8081/actuator/prometheus | grep kafka

# JVM 메모리 사용량
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

## 🚀 6. 전체 시스템 테스트

### 6.1 완전한 데이터 플로우 테스트
```bash
# 1단계: 모든 서비스 시작
docker-compose up -d
sleep 10

# 2단계: Collector 시작 (실제 KIS API)
cd collector
source ../.env
KIS_WEBSOCKET_URL="$KIS_WEBSOCKET_URL" \
KIS_APP_KEY="$KIS_APP_KEY" \
KIS_APP_SECRET="$KIS_APP_SECRET" \
./gradlew bootRun > /tmp/collector-full-test.log 2>&1 &

# 3단계: 30초 대기 후 전체 확인
sleep 30

# 4단계: 데이터 플로우 확인
echo "=== 1. KIS API 연결 ==="
grep "WebSocket connection opened" /tmp/collector-full-test.log

echo "=== 2. Kafka 전송 ==="
grep "Quote message sent successfully" /tmp/collector-full-test.log | wc -l

echo "=== 3. 데이터 저장 ==="
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT COUNT(*) as total_records FROM quote_data;"

echo "=== 4. SSE 스트리밍 ==="
curl -N http://localhost:8083/api/sse/quotes/005930 &
SSE_PID=$!
sleep 5
kill $SSE_PID
```

### 6.2 부하 테스트
```bash
# 동시 SSE 연결 테스트
for i in {1..10}; do
  curl -N http://localhost:8083/api/sse/quotes/005930 > /tmp/sse_test_$i.log 2>&1 &
done

# 10초 후 종료
sleep 10
pkill -f "curl.*sse"

# 결과 확인
ls -la /tmp/sse_test_*.log
```

## 📚 7. 유용한 명령어 모음

### 7.1 서비스 관리
```bash
# 모든 서비스 상태 확인
docker-compose ps
curl http://localhost:8081/actuator/health  # Collector
curl http://localhost:8082/actuator/health  # Data Processor
curl http://localhost:8083/actuator/health  # SSE Streamer
curl http://localhost:8084/actuator/health  # Notification Service

# 서비스 재시작
docker-compose restart data-processor
docker-compose restart sse-streamer

# 로그 확인
docker logs data-processor --tail 50
docker logs sse-streamer --tail 50
```

### 7.2 데이터 조회
```bash
# 최신 주식 데이터 조회
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT * FROM quote_data WHERE stock_code='005930' ORDER BY created_at DESC LIMIT 10;"

# 호가 데이터 조회
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT o.stock_code, o.quote_time, ol.order_type, ol.price_level, ol.price, ol.volume 
   FROM orderbooks o JOIN orderbook_levels ol ON o.id = ol.orderbook_id 
   WHERE o.stock_code='005930' ORDER BY o.quote_time DESC, ol.order_type, ol.price_level LIMIT 20;"

# 알림 조건 조회
docker exec -i mysql mysql -u root -proot123 stock_streaming -e \
  "SELECT * FROM notification_conditions WHERE is_active=1;"
```

### 7.3 모니터링
```bash
# Kafka UI 열기
open http://localhost:8080

# Grafana 대시보드 열기
open http://localhost:3000
# 로그인: admin / admin123

# Prometheus 메트릭 확인
open http://localhost:9090
```

## ⚠️ 8. 주의사항

### 8.1 보안
- **API 키를 절대 공개 저장소에 커밋하지 마세요**
- `.env` 파일을 `.gitignore`에 추가하세요
- 운영 환경에서는 환경변수나 시크릿 관리 도구 사용

### 8.2 API 사용량 제한
- KIS API는 일일/분당 호출 제한이 있습니다
- 모의투자 환경을 우선 사용하세요
- 불필요한 API 호출을 피하세요

### 8.3 데이터 정확성
- 모의투자 환경의 데이터는 실제와 다를 수 있습니다
- 투자 의사결정에 사용하기 전 실제 환경에서 검증하세요

## 🆘 9. 지원 및 문서

- **KIS Developers**: https://apiportal.koreainvestment.com/
- **API 문서**: KIS Developers → API 문서
- **FAQ**: KIS Developers → FAQ
- **이슈 신고**: GitHub Issues (프로젝트 저장소)

---

**성공적인 테스트를 위해 단계별로 차근차근 진행하세요! 🚀**


