# 테스트 코드 작성을 위한 서비스별 기능명세서

## 📋 테스트 작성 권장 순서

1. **Mock WebSocket Server** - 가장 단순한 구조
2. **Data Processor** - 핵심 비즈니스 로직
3. **Collector** - 외부 의존성이 많음
4. **SSE Streamer** - 실시간 스트리밍 로직
5. **Notification Service** - 복잡한 비즈니스 규칙
6. **Integration Tests** - 전체 시스템 통합

---

## 1. Mock WebSocket Server 테스트 명세

### 📌 서비스 개요
- **목적**: KIS API WebSocket 서버 시뮬레이션
- **포트**: 8090
- **주요 기능**: 실시간 주식 데이터 생성 및 WebSocket 전송

### 🔧 핵심 컴포넌트

#### A. KisWebSocketHandler
```java
// 위치: mock-websocket/src/main/java/.../handler/KisWebSocketHandler.java
// 역할: WebSocket 연결 및 메시지 처리
```

**주요 메서드:**
- `afterConnectionEstablished()`: 연결 생성
- `afterConnectionClosed()`: 연결 종료
- `handleTextMessage()`: 구독 요청 처리
- `sendQuoteData()`: 주식 데이터 전송
- `sendOrderbookData()`: 호가 데이터 전송

#### B. MockDataService
```java
// 위치: mock-websocket/src/main/java/.../service/MockDataService.java
// 역할: 가짜 주식 데이터 생성
```

**주요 메서드:**
- `generateQuoteData()`: 시세 데이터 생성
- `generateOrderbookData()`: 호가 데이터 생성

### 🧪 테스트 시나리오

#### 1. WebSocket 연결 테스트
```java
@Test
void testWebSocketConnection() {
    // Given: WebSocket 클라이언트 생성
    // When: 서버에 연결 시도
    // Then: 연결 성공 확인
}
```

#### 2. 구독 메시지 처리 테스트
```java
@Test
void testSubscriptionRequest() {
    // Given: 유효한 구독 요청 메시지
    // When: 메시지 전송
    // Then: 구독 성공 응답 확인
}
```

#### 3. 데이터 생성 테스트
```java
@Test
void testDataGeneration() {
    // Given: 종목 코드
    // When: 데이터 생성 요청
    // Then: KIS API 형식의 데이터 반환 확인
}
```

### 🚫 엣지 케이스

1. **잘못된 구독 메시지**: 형식 오류, 필수 필드 누락
2. **존재하지 않는 종목**: 미등록 종목 코드 요청
3. **동시 다중 연결**: 여러 클라이언트 동시 접속
4. **연결 중단**: 클라이언트 갑작스런 연결 종료
5. **메시지 전송 실패**: 네트워크 오류 시뮬레이션

### 📊 검증 포인트

- WebSocket 연결 상태 관리
- 메시지 형식 검증 (KIS API 스펙 준수)
- 다중 클라이언트 지원
- 메모리 리소스 관리 (세션 정리)
- 오류 상황 처리

---

## 2. Data Processor 테스트 명세

### 📌 서비스 개요
- **목적**: Kafka 메시지 소비 및 데이터베이스 저장
- **포트**: 8082
- **주요 기능**: 실시간 데이터 처리 및 캐싱

### 🔧 핵심 컴포넌트

#### A. QuoteStreamConsumer
```java
// 위치: data-processor/src/main/java/.../consumer/QuoteStreamConsumer.java
// 역할: quote-stream 토픽 메시지 소비
```

**주요 메서드:**
- `handleQuoteMessage()`: 메시지 처리
- `acknowledgeMessage()`: 수동 ACK

#### B. QuoteDataService
```java
// 위치: data-processor/src/main/java/.../service/QuoteDataService.java
// 역할: 비즈니스 로직 처리
```

**주요 메서드:**
- `processQuoteData()`: 데이터 처리 로직
- `saveQuoteData()`: 데이터베이스 저장
- `updateCache()`: 캐시 업데이트
- `checkDuplicate()`: 중복 데이터 검사

#### C. Entity & Repository
```java
// QuoteData, Orderbook, OrderbookLevel 엔티티
// QuoteDataRepository, OrderbookRepository 등
```

### 🧪 테스트 시나리오

#### 1. Kafka 메시지 소비 테스트
```java
@Test
void testKafkaMessageConsumption() {
    // Given: 유효한 Kafka 메시지
    // When: 메시지 소비
    // Then: 정상 처리 및 ACK 확인
}
```

#### 2. 데이터 저장 테스트
```java
@Test
void testDataSaving() {
    // Given: 파싱된 주식 데이터
    // When: 저장 로직 실행
    // Then: 데이터베이스에 정확히 저장됨 확인
}
```

#### 3. 캐시 업데이트 테스트
```java
@Test
void testCacheUpdate() {
    // Given: 새로운 주식 데이터
    // When: 캐시 업데이트
    // Then: 최신 데이터로 캐시 갱신 확인
}
```

#### 4. 중복 데이터 처리 테스트
```java
@Test
void testDuplicateDataHandling() {
    // Given: 동일한 sequence_number 데이터
    // When: 중복 데이터 처리
    // Then: 중복 저장 방지 확인
}
```

### 🚫 엣지 케이스

1. **잘못된 JSON 형식**: 파싱 불가능한 메시지
2. **필수 필드 누락**: stock_code, price 등 누락
3. **데이터 타입 오류**: 숫자 필드에 문자열
4. **데이터베이스 연결 실패**: DB 다운 상황
5. **메모리 부족**: 대량 데이터 처리 시
6. **트랜잭션 롤백**: 저장 중 오류 발생
7. **Kafka 연결 끊김**: 네트워크 장애
8. **캐시 만료**: TTL 초과 상황

### 📊 검증 포인트

- 메시지 파싱 정확성
- 데이터 무결성 (제약조건 검사)
- 트랜잭션 처리 (ACID 보장)
- 캐시 일관성
- 오류 복구 메커니즘
- 성능 (처리 시간, 메모리 사용량)

---

## 3. Collector 테스트 명세

### 📌 서비스 개요
- **목적**: WebSocket으로 데이터 수집 후 Kafka 전송
- **포트**: 8081
- **주요 기능**: 외부 API 연동 및 메시지 브로커 역할

### 🔧 핵심 컴포넌트

#### A. KisWebSocketClient
```java
// 위치: collector/src/main/java/.../client/KisWebSocketClient.java
// 역할: WebSocket 클라이언트 관리
```

**주요 메서드:**
- `connect()`: WebSocket 연결
- `subscribeToQuote()`: 주식 구독
- `subscribeToOrderbook()`: 호가 구독
- `handleMessage()`: 메시지 처리
- `scheduleReconnect()`: 재연결 로직

#### B. KafkaProducerService
```java
// 위치: collector/src/main/java/.../service/KafkaProducerService.java
// 역할: Kafka 메시지 전송
```

**주요 메서드:**
- `sendQuoteMessage()`: 주식 데이터 전송
- `sendOrderbookMessage()`: 호가 데이터 전송

#### C. Message DTOs
```java
// KisQuoteData, KisOrderbookData 등
// 메시지 파싱 및 변환 담당
```

### 🧪 테스트 시나리오

#### 1. WebSocket 연결 테스트
```java
@Test
void testWebSocketConnection() {
    // Given: Mock WebSocket 서버
    // When: 연결 시도
    // Then: 연결 성공 및 상태 확인
}
```

#### 2. 메시지 파싱 테스트
```java
@Test
void testMessageParsing() {
    // Given: KIS API 형식 메시지
    // When: 메시지 파싱
    // Then: 올바른 DTO 객체 생성 확인
}
```

#### 3. Kafka 전송 테스트
```java
@Test
void testKafkaMessageSending() {
    // Given: 파싱된 데이터
    // When: Kafka 전송
    // Then: 메시지 정상 전송 확인
}
```

#### 4. 재연결 로직 테스트
```java
@Test
void testReconnectionLogic() {
    // Given: WebSocket 연결 끊김
    // When: 재연결 시도
    // Then: 자동 재연결 성공 확인
}
```

### 🚫 엣지 케이스

1. **WebSocket 연결 실패**: 서버 다운, 네트워크 오류
2. **인증 실패**: 잘못된 API 키
3. **메시지 형식 오류**: 예상과 다른 데이터 형식
4. **Kafka 전송 실패**: 브로커 다운, 토픽 없음
5. **SSL 인증서 오류**: HTTPS 연결 문제
6. **메모리 누수**: 장시간 실행 시 리소스 누적
7. **스레드 데드락**: 동시성 문제
8. **백프레셔**: 처리 속도보다 빠른 데이터 유입

### 📊 검증 포인트

- WebSocket 생명주기 관리
- 메시지 파싱 정확성
- Kafka 파티셔닝 전략 (종목별)
- 오류 복구 및 재시도 로직
- 성능 및 처리량
- 리소스 사용량 모니터링

---

## 4. SSE Streamer 테스트 명세

### 📌 서비스 개요
- **목적**: 클라이언트에게 실시간 데이터 스트리밍
- **포트**: 8083
- **주요 기능**: Server-Sent Events를 통한 실시간 데이터 전송

### 🔧 핵심 컴포넌트

#### A. SseStreamController
```java
// 위치: sse-streamer/src/main/java/.../controller/SseStreamController.java
// 역할: SSE 연결 관리
```

**주요 메서드:**
- `connect()`: SSE 연결 생성
- `disconnect()`: 연결 종료
- `getStatus()`: 연결 상태 조회

#### B. SseConnectionManager
```java
// 위치: sse-streamer/src/main/java/.../service/SseConnectionManager.java
// 역할: 연결 관리 및 브로드캐스트
```

**주요 메서드:**
- `addConnection()`: 연결 추가
- `removeConnection()`: 연결 제거
- `broadcastToSubscribers()`: 구독자에게 브로드캐스트
- `sendHeartbeat()`: 하트비트 전송

#### C. StreamDataService
```java
// 역할: Kafka 메시지를 SSE로 변환
```

### 🧪 테스트 시나리오

#### 1. SSE 연결 테스트
```java
@Test
void testSseConnection() {
    // Given: SSE 클라이언트
    // When: 연결 요청
    // Then: 연결 성공 및 스트림 시작 확인
}
```

#### 2. 구독 관리 테스트
```java
@Test
void testSubscriptionManagement() {
    // Given: 특정 종목 구독 요청
    // When: 구독 처리
    // Then: 해당 종목 데이터만 수신 확인
}
```

#### 3. 브로드캐스트 테스트
```java
@Test
void testBroadcast() {
    // Given: 여러 클라이언트 연결
    // When: 데이터 브로드캐스트
    // Then: 모든 구독자에게 전송 확인
}
```

#### 4. 연결 정리 테스트
```java
@Test
void testConnectionCleanup() {
    // Given: 비정상 종료된 연결
    // When: 정리 작업 실행
    // Then: 메모리 누수 없이 정리 확인
}
```

### 🚫 엣지 케이스

1. **클라이언트 갑작스런 연결 종료**: 네트워크 끊김
2. **대량 동시 연결**: 부하 테스트
3. **느린 클라이언트**: 데이터 소비 속도 지연
4. **메모리 부족**: 대량 연결 시 리소스 부족
5. **잘못된 구독 요청**: 존재하지 않는 종목
6. **중복 연결**: 같은 클라이언트의 여러 연결
7. **타임아웃**: 장시간 비활성 연결
8. **백프레셔**: 클라이언트 처리 지연

### 📊 검증 포인트

- SSE 프로토콜 준수
- 연결 상태 관리
- 메모리 효율성
- 실시간 성능 (지연시간)
- 클라이언트별 필터링 정확성
- 장애 복구 메커니즘

---

## 5. Notification Service 테스트 명세

### 📌 서비스 개요
- **목적**: 알림 조건 관리 및 실시간 알림 발송
- **포트**: 8084
- **주요 기능**: 조건부 알림 시스템

### 🔧 핵심 컴포넌트

#### A. NotificationController
```java
// 위치: notification-service/src/main/java/.../controller/NotificationController.java
// 역할: 알림 조건 CRUD API
```

#### B. ConditionEvaluationService
```java
// 위치: notification-service/src/main/java/.../service/ConditionEvaluationService.java
// 역할: 알림 조건 평가
```

**주요 메서드:**
- `evaluateConditions()`: 조건 평가
- `triggerNotification()`: 알림 트리거
- `isConditionMet()`: 조건 충족 확인

#### C. NotificationDeliveryService
```java
// 역할: 실제 알림 전송
```

### 🧪 테스트 시나리오

#### 1. 조건 등록 테스트
```java
@Test
void testConditionCreation() {
    // Given: 유효한 알림 조건
    // When: 조건 등록
    // Then: 데이터베이스에 저장 확인
}
```

#### 2. 조건 평가 테스트
```java
@Test
void testConditionEvaluation() {
    // Given: 등록된 조건과 실시간 데이터
    // When: 조건 평가 실행
    // Then: 올바른 판정 결과 확인
}
```

#### 3. 알림 발송 테스트
```java
@Test
void testNotificationDelivery() {
    // Given: 트리거된 알림
    // When: 알림 발송
    // Then: 성공적인 전송 확인
}
```

#### 4. 재시도 로직 테스트
```java
@Test
void testRetryMechanism() {
    // Given: 전송 실패한 알림
    // When: 재시도 실행
    // Then: 설정된 횟수만큼 재시도 확인
}
```

### 🚫 엣지 케이스

1. **잘못된 조건식**: 문법 오류, 논리적 모순
2. **중복 조건**: 동일한 조건의 중복 등록
3. **만료된 조건**: 비활성화된 조건 처리
4. **대량 조건**: 수만 개의 조건 동시 평가
5. **알림 전송 실패**: 외부 서비스 장애
6. **데이터 지연**: 실시간 데이터 지연 시 처리
7. **동시 트리거**: 같은 조건의 동시 트리거
8. **메모리 부족**: 대량 알림 대기열

### 📊 검증 포인트

- 조건 평가 정확성
- 알림 전송 신뢰성
- 성능 (대량 조건 처리)
- 중복 알림 방지
- 오류 처리 및 복구
- 알림 이력 관리

---

## 6. Integration Test 명세

### 📌 통합 테스트 개요
- **목적**: 전체 시스템 end-to-end 검증
- **범위**: 모든 서비스 간 상호작용

### 🧪 주요 시나리오

#### 1. 전체 데이터 플로우 테스트
```java
@Test
void testEndToEndDataFlow() {
    // Given: 모든 서비스 실행 상태
    // When: Mock 서버에서 데이터 생성
    // Then: SSE 클라이언트까지 데이터 도달 확인
}
```

#### 2. 알림 시스템 통합 테스트
```java
@Test
void testNotificationFlow() {
    // Given: 알림 조건 설정
    // When: 조건을 만족하는 데이터 발생
    // Then: 알림 발송까지 완료 확인
}
```

#### 3. 장애 복구 테스트
```java
@Test
void testFailureRecovery() {
    // Given: 일부 서비스 다운
    // When: 서비스 복구
    // Then: 시스템 정상화 확인
}
```

### 🚫 통합 테스트 엣지 케이스

1. **서비스 순차 재시작**: 의존성 순서 확인
2. **네트워크 분할**: 일부 서비스 간 통신 단절
3. **데이터 일관성**: 여러 서비스 간 데이터 동기화
4. **부하 테스트**: 전체 시스템 처리 한계
5. **메모리 누수**: 장시간 실행 시 리소스 관리

---

## 📚 테스트 작성 가이드라인

### 🔧 권장 테스트 도구

- **Unit Tests**: JUnit 5, Mockito
- **Integration Tests**: Spring Boot Test, TestContainers
- **WebSocket Tests**: Spring WebSocket Test
- **Kafka Tests**: EmbeddedKafka, TestContainers
- **Database Tests**: H2, TestContainers MySQL

### 📊 테스트 커버리지 목표

- **Unit Test**: 80% 이상
- **Integration Test**: 주요 플로우 100%
- **Edge Cases**: 중요 비즈니스 로직 100%

### 🎯 테스트 작성 우선순위

1. **핵심 비즈니스 로직** (데이터 처리, 조건 평가)
2. **외부 의존성** (Kafka, WebSocket, Database)
3. **오류 처리** (재시도, 복구 메커니즘)
4. **성능 임계점** (처리량, 응답시간)
5. **보안 및 검증** (입력 유효성, 권한)

이 명세서를 바탕으로 체계적인 테스트 코드를 작성하시면 됩니다! 어떤 서비스부터 테스트를 시작하시겠어요?
