# 실시간 주식 시세 스트리밍 시스템 아키텍처

## 시스템 개요
실시간 주식 시세 및 호가 정보를 WebSocket으로 수집하고, Kafka를 통해 처리하여 클라이언트에게 SSE로 전송하는 MSA 기반 시스템입니다.

## 서비스 구성

### 1. quote-stream-collector
- **역할**: KIS WebSocket API에서 실시간 시세/호가 데이터 수집
- **기술**: Spring Boot, WebSocket Client, Kafka Producer
- **출력**: `quote-stream`, `orderbook-stream` Kafka 토픽

### 2. data-processor  
- **역할**: Kafka에서 데이터 소비, MySQL 저장, 캐시 관리
- **기술**: Spring Boot, Kafka Consumer, JPA, Caffeine Cache
- **저장**: MySQL + 인메모리 캐시

### 3. sse-streamer
- **역할**: 클라이언트 SSE 연결 관리 및 실시간 데이터 스트리밍
- **기술**: Spring Boot, SSE, Kafka Consumer
- **지원**: 최대 100,000 동시 연결

### 4. notification-service
- **역할**: 사용자 알림 조건 평가 및 알림 전송
- **기술**: Spring Boot, Kafka Consumer/Producer, EDA
- **성능**: 100,000건 이상 조건 평가

## 데이터 플로우
```
KIS WebSocket → quote-stream-collector → Kafka → data-processor → MySQL/Cache
                                               → sse-streamer → SSE Client
                                               → notification-service → Alert
```

## 성능 목표
- **처리량**: 초당 10,000건 이상
- **지연시간**: 1초 이내 처리 완료
- **동시 접속**: 최대 100,000명
- **정합성**: 100% 데이터 정확성 보장