# 성능 기반 고도화 시나리오 및 판단 기준

> **기본 원칙**: 기초 구현 완료 → 모니터링 데이터 수집 → 성능 기준 검증 → 필요시 고도화 적용

---

## 📊 모니터링 해야 할 핵심 메트릭

### **1. 데이터 처리 지연 (Latency)**
```
측정 지점: WebSocket 수신 → Kafka 전송 → DB 저장 → SSE 전송
목표: 전체 파이프라인 1초 이내
경고 기준: 평균 800ms 이상 또는 P99 2초 이상
```

### **2. 처리량 (Throughput)**
```
측정 대상: 초당 처리 메시지 수
목표: 10,000건/초 이상
경고 기준: 8,000건/초 미만 지속
```

### **3. 시스템 리소스**
```
CPU: 평균 70% 이상 시 스케일 아웃 검토
Memory: JVM heap 80% 이상 시 튜닝 필요
Kafka Lag: 1,000건 이상 누적 시 Consumer 증설
```

---

## 🔧 고도화 시나리오 및 적용 기준

### **시나리오 1: Redis 도입**

#### **적용 조건:**
- MySQL 조회 응답시간이 **50ms 초과**하는 경우가 10% 이상
- SSE 클라이언트 수가 **10,000명 이상**일 때 DB 부하 증가
- Caffeine Cache 히트율이 **85% 미만**인 경우

#### **구체적 예시:**
```bash
# MySQL 슬로우 쿼리 로그에서 확인
SELECT stock_code, COUNT(*) FROM quote_data 
WHERE created_at > NOW() - INTERVAL 1 MINUTE 
AND response_time > 0.05 
GROUP BY stock_code 
HAVING COUNT(*) > (총_쿼리_수 * 0.1);

# 적용 시점: 위 쿼리 결과가 지속적으로 나올 때
```

#### **구현 방안:**
```yaml
# Redis 클러스터 구성
- 종목별 최신 시세: key="quote:{stock_code}", TTL=60초
- 종목별 최신 호가: key="orderbook:{stock_code}", TTL=30초
- 인기 종목 리스트: key="hot_stocks", TTL=300초
```

---

### **시나리오 2: Kafka 파티션 분리**

#### **적용 조건:**
- 특정 종목의 메시지가 **전체의 30% 이상** 차지
- Consumer Lag이 **특정 파티션에서 지속적으로 발생**
- 인기 종목 처리로 인한 **다른 종목 지연** 발생

#### **구체적 예시:**
```sql
-- 일일 메시지 비중 분석
SELECT 
    stock_code,
    COUNT(*) as message_count,
    COUNT(*) * 100.0 / SUM(COUNT(*)) OVER() as percentage
FROM quote_data 
WHERE created_at > CURDATE()
GROUP BY stock_code 
ORDER BY message_count DESC 
LIMIT 10;

-- 삼성전자(005930)가 35% 차지 → 전용 토픽 생성
```

#### **구현 방안:**
```
기존: quote-stream (6 partitions)
분리: quote-stream-hot (삼성전자, 네이버 등), quote-stream-normal
Consumer Group도 분리하여 독립적 처리
```

---

### **시나리오 3: Idempotency 보장**

#### **적용 조건:**
- **중복 메시지 발생률 1% 이상**
- 네트워크 불안정으로 **재전송 빈발**
- 데이터 정합성 이슈 발생

#### **구체적 예시:**
```sql
-- 중복 데이터 검출
SELECT stock_code, trade_time, COUNT(*) as dup_count
FROM quote_data 
WHERE created_at > NOW() - INTERVAL 1 HOUR
GROUP BY stock_code, trade_time 
HAVING COUNT(*) > 1
ORDER BY dup_count DESC;

-- 5% 이상 중복 발견시 Idempotency Key 도입
```

#### **구현 방안:**
```java
// Redis 기반 Idempotency 체크
String idempotencyKey = stock_code + "_" + timestamp + "_" + sequence;
if (redisTemplate.opsForValue().setIfAbsent(key, "processed", Duration.ofMinutes(5))) {
    // 신규 데이터 처리
    processMessage(message);
}
```

---

### **시나리오 4: Time-Series Database 도입**

#### **적용 조건:**
- 분/시/일봉 **쿼리 응답시간 500ms 이상**
- 과거 데이터 조회 시 **MySQL 부하 급증**
- 차트 데이터 요청이 **전체 트래픽의 40% 이상**

#### **구체적 예시:**
```sql
-- 무거운 집계 쿼리 예시
SELECT 
    stock_code,
    DATE_FORMAT(trade_time, '%Y-%m-%d %H:%i:00') as minute_time,
    FIRST_VALUE(price) OVER (PARTITION BY stock_code, minute_window ORDER BY trade_time) as open_price,
    MAX(price) as high_price,
    MIN(price) as low_price,
    LAST_VALUE(price) OVER (PARTITION BY stock_code, minute_window ORDER BY trade_time) as close_price,
    SUM(volume) as total_volume
FROM quote_data 
WHERE trade_time BETWEEN '2024-01-01' AND '2024-01-31'
GROUP BY stock_code, minute_time;

-- 이런 쿼리가 자주 실행되고 느릴 때 TSDB 도입
```

#### **구현 방안:**
```
InfluxDB/TimescaleDB 도입
- Raw Data: MySQL (실시간 처리용)
- Aggregated Data: TSDB (차트/분석용)
- 별도 Aggregation Service로 주기적 집계
```

---

### **시나리오 5: 기술적 지표 계산**

#### **적용 조건:**
- 기술적 지표 **요청 빈도 높음** (일 1만회 이상)
- 실시간 계산으로 인한 **CPU 사용량 90% 이상**
- 사용자가 **커스텀 지표 설정** 요구

#### **구체적 예시:**
```java
// 현재: 요청시마다 실시간 계산 (부하 높음)
@GetMapping("/indicators/{stockCode}")
public IndicatorResponse getIndicators(@PathVariable String stockCode) {
    List<Quote> quotes = quoteService.getRecentQuotes(stockCode, 20);
    double ma20 = calculateMA(quotes, 20);  // 매번 계산
    double rsi = calculateRSI(quotes, 14);  // 매번 계산
    return new IndicatorResponse(ma20, rsi);
}

// 개선: 별도 Consumer에서 사전 계산 후 캐시 저장
```

#### **구현 방안:**
```
별도 Kafka Consumer Group: "technical-indicators"
→ 실시간 시세 수신시 지표 계산
→ Redis에 캐시 저장: "indicators:{stock_code}"
→ API는 캐시에서 조회만
```

---

## 📈 성능 개선 효과 측정 방법

### **Before/After 비교 메트릭:**
1. **응답시간**: P50, P90, P99 지연시간 비교
2. **처리량**: 동일 부하에서 처리 가능한 TPS 비교  
3. **리소스 효율성**: CPU/Memory 사용률 개선도
4. **안정성**: 에러율, 타임아웃 발생률 감소

### **A/B 테스트 방법:**
```
1. 카나리 배포: 전체 트래픽의 10%만 신규 구조 적용
2. 동일 부하 조건에서 1주일간 모니터링
3. 성능 지표 개선 확인 후 점진적 확대
4. 문제 발생시 즉시 롤백 가능한 구조 유지
```

---

## 🎯 최종 판단 기준

### **고도화 진행 조건:**
- **성능 개선**: 기존 대비 20% 이상 향상
- **안정성 유지**: 에러율 증가 없음
- **복잡도 대비 효과**: 구현 비용 대비 명확한 이익
- **운영 편의성**: 모니터링 및 장애 대응 복잡도 수용 가능

### **고도화 보류 조건:**
- 현재 성능이 목표 기준을 충족
- 트래픽 증가 예상 없음  
- 개발 리소스 부족
- 시스템 복잡도 급증으로 운영 리스크 증가