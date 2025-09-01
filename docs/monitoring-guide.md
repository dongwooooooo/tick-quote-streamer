# 모니터링 가이드

## 개요

Stock Streaming System의 모니터링은 **Prometheus + Grafana** 스택을 사용하여 구성됩니다.

## 모니터링 아키텍처

```
Application Services → Prometheus → Grafana
     ↓
Spring Boot Actuator
   (metrics endpoint)
```

## 설정 및 시작

### 1. 환경 변수 설정

```bash
cp docker-compose.env.example .env
```

### 2. 모니터링 서비스 시작

```bash
# 기본 인프라 서비스 먼저 시작
docker-compose up -d zookeeper kafka mysql

# 모니터링 서비스 시작  
docker-compose --profile monitoring up -d prometheus grafana kafka-ui
```

### 3. 애플리케이션 서비스 시작

```bash
# Docker로 실행하는 경우
docker-compose up -d data-processor sse-streamer notification-service

# 로컬에서 실행하는 경우 (개발 환경)
cd mock-websocket && ./gradlew bootRun &
cd collector && ./gradlew bootRun &
```

## 접속 정보

| 서비스 | URL | 설명 |
|--------|-----|------|
| Grafana | http://localhost:3000 | 메인 대시보드 (admin/admin123) |
| Prometheus | http://localhost:9090 | 메트릭 수집 서버 |
| Kafka UI | http://localhost:8080 | Kafka 토픽/메시지 모니터링 |

## 주요 메트릭

### 1. 애플리케이션 메트릭

- **HTTP 요청 처리율**: `rate(http_server_requests_seconds_count[1m])`
- **HTTP 응답 시간**: `http_server_requests_seconds`
- **JVM 메모리 사용량**: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- **CPU 사용률**: `system_cpu_usage`

### 2. Kafka 메트릭

- **Consumer Lag**: `kafka_consumer_lag_sum`
- **메시지 처리 속도**: `rate(kafka_consumer_records_consumed_total[1m])`

### 3. 비즈니스 메트릭

- **주식 데이터 처리 건수**: Custom metrics via Micrometer
- **SSE 연결 수**: Custom metrics via Micrometer
- **알림 발송 건수**: Custom metrics via Micrometer

## 대시보드

### Stock Streaming System Overview

기본 제공되는 대시보드로 다음 정보를 포함합니다:

1. **HTTP Request Rate**: 서비스별 요청 처리율
2. **JVM Memory Usage**: 힙 메모리 사용량 모니터링
3. **Kafka Consumer Lag**: 메시지 처리 지연 모니터링  
4. **System CPU Usage**: CPU 사용률

### 사용자 정의 대시보드 추가

Grafana UI에서 직접 대시보드를 생성하거나, JSON 파일을 `/infra/grafana/dashboards/` 디렉토리에 추가할 수 있습니다.

## 알림 설정

### Prometheus Alert Rules

향후 추가 예정:
- 높은 메모리 사용률 (> 80%)
- 높은 CPU 사용률 (> 80%)  
- Kafka Consumer Lag 임계치 초과
- 서비스 다운 감지

### Grafana 알림

Grafana에서 직접 알림 채널을 설정할 수 있습니다:
- Slack 연동
- 이메일 알림
- Webhook 알림

## 트러블슈팅

### 메트릭이 수집되지 않는 경우

1. **서비스 상태 확인**:
   ```bash
   curl http://localhost:8082/actuator/health
   curl http://localhost:8082/actuator/prometheus
   ```

2. **Prometheus Target 상태 확인**:
   - http://localhost:9090/targets 에서 모든 target이 UP 상태인지 확인

3. **네트워크 연결 확인**:
   ```bash
   docker network ls
   docker network inspect tick-quote-streamer_stock-network
   ```

### Grafana 대시보드가 로드되지 않는 경우

1. **데이터소스 연결 확인**: Grafana > Configuration > Data Sources
2. **대시보드 프로비저닝 확인**: `/infra/grafana/provisioning/` 디렉토리 권한
3. **로그 확인**:
   ```bash
   docker logs grafana
   ```

## 성능 최적화 모니터링

다음 메트릭들을 지속적으로 모니터링하여 성능 최적화 시점을 결정합니다:

### 임계치

- **응답 시간**: > 3초
- **메모리 사용률**: > 80%
- **CPU 사용률**: > 80%
- **Kafka Consumer Lag**: > 1000개
- **DB 커넥션 풀 사용률**: > 80%

### 최적화 시나리오

성능 문제 발생 시 `docs/performance-enhancement-scenarios.md` 참고하여 단계적 최적화를 진행합니다.
