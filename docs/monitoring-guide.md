# 모니터링 가이드

## 개요

Stock Streaming System의 모니터링은 **Prometheus + Grafana** 스택을 사용하여 구성됩니다.
Kafka 메트릭은 **Kafka Exporter**와 **Burrow**를 통해 수집되며, 애플리케이션 메트릭은 **Micrometer**를 통해 수집됩니다.

## 모니터링 아키텍처

```
Application Services → Micrometer → Prometheus → Grafana
     ↓
Kafka Cluster → Kafka Exporter → Prometheus → Grafana
     ↓
Kafka Cluster → Burrow → Prometheus → Grafana
     ↓
Spring Boot Actuator (metrics endpoint)
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

# 모니터링 서비스 시작 (Kafka Exporter, Burrow 포함)
docker-compose --profile monitoring up -d prometheus grafana kafka-ui kafka-exporter burrow
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
| Kafka Exporter | http://localhost:9308/metrics | Kafka 메트릭 엔드포인트 |
| Burrow | http://localhost:8000 | Consumer Lag 모니터링 API |

## 주요 메트릭

### 1. 애플리케이션 메트릭

- **HTTP 요청 처리율**: `rate(http_server_requests_seconds_count[1m])`
- **HTTP 응답 시간**: `http_server_requests_seconds`
- **JVM 메모리 사용량**: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- **CPU 사용률**: `system_cpu_usage`

### 2. Kafka 메트릭 (Kafka Exporter)

- **Consumer Lag**: `kafka_consumergroup_lag`
- **메시지 처리 속도**: `rate(kafka_consumergroup_current_offset[1m])`
- **브로커 상태**: `kafka_cluster_partition_underreplicated_partition_count`
- **토픽 처리량**: `rate(kafka_broker_topic_metrics_bytes_in_total[1m])`

### 3. Consumer Lag 모니터링 (Burrow)

- **상세 Lag 정보**: `burrow_partition_lag`
- **Consumer 그룹 상태**: `burrow_consumer_status`
- **파티션별 Lag**: `burrow_partition_lag{cluster="local"}`

### 4. 비즈니스 메트릭 (Custom)

- **종목별 메시지 수**: `collector_messages_total{symbol="005930", type="quote"}`
- **종목별 TPS**: `rate(collector_messages_total[1m])`
- **컨슈머 처리시간**: `data_processor_processing_seconds`
- **컨슈머 처리량**: `data_processor_messages_total`
- **컨슈머 실패율**: `data_processor_failures_total`

## 대시보드

### 1. Stock Streaming System Overview

기본 제공되는 대시보드로 다음 정보를 포함합니다:

1. **HTTP Request Rate**: 서비스별 요청 처리율
2. **JVM Memory Usage**: 힙 메모리 사용량 모니터링
3. **Kafka Consumer Lag**: 메시지 처리 지연 모니터링  
4. **System CPU Usage**: CPU 사용률

### 2. Kafka & Streaming Observability

새로 추가된 전문 대시보드로 다음 정보를 포함합니다:

1. **Kafka Consumer Lag (Top 10)**: 파티션별 지연 모니터링
2. **Consumer Processing Time**: p95/p99 처리시간
3. **Consumer Throughput**: 초당 메시지 처리량
4. **Stock TPS by Symbol**: 종목별 초당 거래량 (Top 20)
5. **Consumer Failure Rate**: 처리 실패율
6. **Kafka Cluster Health**: 브로커 상태 모니터링
7. **Kafka Throughput**: 바이트 단위 처리량
8. **Burrow Consumer Lag**: 상세 지연 정보

### 대시보드 임포트

```bash
# Grafana UI에서 직접 임포트
# 1. Grafana 접속 (http://localhost:3000)
# 2. "+" → "Import" 
# 3. /infra/grafana/dashboards/kafka-streaming-observability.json 파일 업로드
```

### 사용자 정의 대시보드 추가

Grafana UI에서 직접 대시보드를 생성하거나, JSON 파일을 `/infra/grafana/dashboards/` 디렉토리에 추가할 수 있습니다.

## 핵심 쿼리 모음

### Kafka Consumer Lag 모니터링

```promql
# Top 10 파티션 Lag
topk(10, kafka_consumergroup_lag{group="data-processor"})

# 특정 토픽의 Lag
kafka_consumergroup_lag{group="data-processor", topic="quote-stream"}

# Burrow를 통한 상세 Lag
burrow_partition_lag{cluster="local"}
```

### 종목별 TPS 모니터링

```promql
# 종목별 초당 메시지 수 (Top 20)
topk(20, sum by (symbol, type) (rate(collector_messages_total[1m])))

# 특정 종목의 TPS
rate(collector_messages_total{symbol="005930", type="quote"}[1m])
```

### 컨슈머 성능 모니터링

```promql
# 처리시간 p95/p99
histogram_quantile(0.95, sum by (le) (rate(data_processor_processing_seconds_bucket[5m])))
histogram_quantile(0.99, sum by (le) (rate(data_processor_processing_seconds_bucket[5m])))

# 처리량 (초당 메시지)
sum by (type) (rate(data_processor_messages_total[1m]))

# 실패율
sum(rate(data_processor_failures_total[5m])) / sum(rate(data_processor_messages_total[5m]))
```

### Kafka 클러스터 상태

```promql
# Under-replicated 파티션
kafka_cluster_partition_underreplicated_partition_count

# 토픽 처리량
sum(rate(kafka_broker_topic_metrics_bytes_in_total[1m]))
sum(rate(kafka_broker_topic_metrics_bytes_out_total[1m]))
```

## 알림 설정

### Prometheus Alert Rules

다음 경보 룰을 `infra/prometheus/rules/` 디렉토리에 추가할 수 있습니다:

```yaml
groups:
- name: kafka-alerts
  rules:
  - alert: HighConsumerLag
    expr: kafka_consumergroup_lag > 1000
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High consumer lag detected"
      description: "Consumer group {{ $labels.group }} has lag of {{ $value }}"

  - alert: ConsumerFailureRate
    expr: sum(rate(data_processor_failures_total[5m])) / sum(rate(data_processor_messages_total[5m])) > 0.05
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "High consumer failure rate"
      description: "Consumer failure rate is {{ $value | humanizePercentage }}"

  - alert: UnderReplicatedPartitions
    expr: kafka_cluster_partition_underreplicated_partition_count > 0
    for: 1m
    labels:
      severity: warning
    annotations:
      summary: "Under-replicated partitions detected"
      description: "{{ $value }} partitions are under-replicated"
```

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
   - kafka-exporter, burrow 타겟이 정상적으로 스크랩되는지 확인

3. **Kafka Exporter 상태 확인**:
   ```bash
   curl http://localhost:9308/metrics | grep kafka_consumergroup_lag
   ```

4. **Burrow 상태 확인**:
   ```bash
   curl http://localhost:8000/v3/kafka/local/consumer/data-processor/lag
   ```

5. **네트워크 연결 확인**:
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
- **Consumer 처리시간 p95**: > 1초
- **Consumer 실패율**: > 5%
- **DB 커넥션 풀 사용률**: > 80%
- **Under-replicated 파티션**: > 0개

### 최적화 시나리오

성능 문제 발생 시 `docs/performance-enhancement-scenarios.md` 참고하여 단계적 최적화를 진행합니다.
