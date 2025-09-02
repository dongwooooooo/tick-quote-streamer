# KIS API 실제 연동 설정 가이드

## 📋 사전 준비 사항

### 1. 한국투자증권 API 신청
1. [한국투자증권 OpenAPI](https://apiportal.koreainvestment.com/) 접속
2. 회원가입 및 API 신청
3. 앱 등록하여 **App Key**와 **App Secret** 발급받기
4. 계좌번호 확인

### 2. 필요한 정보
- **App Key**: 앱 등록 시 발급받은 키
- **App Secret**: 앱 등록 시 발급받은 시크릿

> 💡 **Note**: App Key와 App Secret만 있으면 시작할 수 있습니다!

## 🔧 환경 변수 설정

### 1. .env 파일 수정

프로젝트 루트의 `.env` 파일을 수정하세요:

```bash
# KIS API 설정 (실전투자용)
KIS_APP_KEY=발급받은_실전투자_앱키
KIS_APP_SECRET=발급받은_실전투자_앱시크릿

# KIS API 설정 (모의투자용 - 별도 발급 필요!)
# Rate Limiting 방지를 위해 실전투자와 다른 App Key 사용
KIS_MOCK_APP_KEY=발급받은_모의투자_앱키
KIS_MOCK_APP_SECRET=발급받은_모의투자_앱시크릿

# KIS API URL 설정
# 모의투자 환경 (추천)
KIS_WEBSOCKET_URL=wss://openapivts.koreainvestment.com:9443

# 실제 투자 환경 (주의!)
# KIS_WEBSOCKET_URL=wss://openapi.koreainvestment.com:9443

# 개발/테스트 환경 (Mock 서버)
# KIS_WEBSOCKET_URL=ws://localhost:8090/kis-mock
```

### 2. 환경별 URL 선택

| 환경 | URL | 설명 |
|------|-----|------|
| **모의투자** (추천) | `wss://openapivts.koreainvestment.com:9443` | 실제 데이터, 가상 거래 |
| **실제 투자** | `wss://openapi.koreainvestment.com:9443` | 실제 거래 환경 |
| **개발 테스트** | `ws://localhost:8090/kis-mock` | Mock 서버 |

## 🚀 적용 및 확인 방법

### 1. 환경 변수 적용

```bash
# 1. .env 파일 수정 후
source .env

# 2. Collector 서비스 재시작 (로컬 실행 중인 경우)
# 기존 프로세스 종료
pkill -f "gradlew bootRun"

# Mock WebSocket 서버 중지 (실제 API 사용 시)
docker-compose stop mock-websocket

# Collector 재시작
cd collector && ./gradlew bootRun
```

### 2. Docker 환경에서 적용

```bash
# 1. .env 파일 수정 후
# 2. Docker 서비스 재시작
docker-compose restart collector

# 3. 로그 확인
docker logs collector -f
```

### 3. 연결 상태 확인

#### A. Collector 로그 확인
```bash
# 로컬 실행 중인 경우
tail -f /tmp/collector.log

# Docker 실행 중인 경우  
docker logs collector -f
```

**성공적인 연결 시 로그 예시:**
```
2025-08-24 18:30:00 [main] INFO  o.e.c.client.KisWebSocketClient - Connecting to KIS WebSocket: wss://openapivts.koreainvestment.com:9443
2025-08-24 18:30:01 [WebSocketClient-1] INFO  o.e.c.client.KisWebSocketClient - Connected to KIS WebSocket successfully
2025-08-24 18:30:01 [WebSocketClient-1] INFO  o.e.c.client.KisWebSocketClient - Subscribing to stocks: [005930, 000660, ...]
```

**연결 실패 시 로그 예시:**
```
ERROR o.e.c.client.KisWebSocketClient - Failed to connect to KIS WebSocket
ERROR o.e.c.client.KisWebSocketClient - Authentication failed: Invalid App Key
```

#### B. Health Check 확인
```bash
curl http://localhost:8081/actuator/health
```

#### C. Kafka 메시지 확인
```bash
# Kafka UI 확인: http://localhost:8080
# 또는 CLI로 확인
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic quote-stream --from-beginning
```

## 🔍 트러블슈팅

### 1. 인증 실패
**증상**: "Authentication failed" 오류
**해결방법**:
- App Key, App Secret 재확인
- 한국투자증권 포털에서 API 승인 상태 확인
- 계좌번호 정확성 확인

### 2. 네트워크 연결 실패
**증상**: "Connection refused" 오류
**해결방법**:
- 인터넷 연결 상태 확인
- 방화벽 설정 확인 (9443 포트)
- URL 정확성 확인

### 3. 데이터 수신 없음
**증상**: 연결은 되지만 데이터가 오지 않음
**해결방법**:
- 장중 시간 확인 (09:00-15:30)
- 구독 종목 코드 정확성 확인
- KIS API 사용량 한도 확인

### 4. Access Token 만료
**증상**: "Token expired" 오류  
**해결방법**:
- KIS API에서 새로운 토큰 발급
- `.env` 파일의 `KIS_ACCESS_TOKEN` 업데이트

## 📊 모니터링

### 1. Grafana 대시보드
http://localhost:3000 에서 실시간 메트릭 확인:
- WebSocket 연결 상태
- 메시지 수신 속도
- 오류율

### 2. 로그 모니터링
```bash
# 실시간 로그 모니터링
tail -f /tmp/collector.log | grep -E "(ERROR|WARN|Connected|Disconnected)"
```

## 🔄 Mock 서버로 복원

개발/테스트를 위해 다시 Mock 서버로 돌아가려면:

```bash
# 1. .env 파일 수정
KIS_WEBSOCKET_URL=ws://localhost:8090/kis-mock

# 2. Mock 서버 시작
cd mock-websocket && ./gradlew bootRun &

# 3. Collector 재시작
cd collector && ./gradlew bootRun
```

## ⚠️ 주의사항

1. **실제 투자 환경 사용 시 주의**: 실제 거래가 발생할 수 있습니다
2. **API 사용량 한도**: KIS API는 일일 사용량 제한이 있습니다
3. **보안**: App Key, App Secret을 절대 공개하지 마세요
4. **장중 시간**: 주식 데이터는 장중(09:00-15:30)에만 수신됩니다
