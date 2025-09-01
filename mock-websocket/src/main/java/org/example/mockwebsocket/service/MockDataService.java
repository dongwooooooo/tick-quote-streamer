package org.example.mockwebsocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.mockwebsocket.dto.KisOrderbookMessage;
import org.example.mockwebsocket.dto.KisQuoteMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class MockDataService {

    private final ObjectMapper objectMapper;
    private final Random random;
    private final ConcurrentMap<String, StockState> stockStates;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public MockDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.random = new Random();
        this.stockStates = new ConcurrentHashMap<>();
        
        // 기본 종목 상태 초기화
        initializeStockStates();
    }

    private void initializeStockStates() {
        stockStates.put("005930", new StockState("005930", 71000, 70000, 72000)); // 삼성전자
        stockStates.put("000660", new StockState("000660", 85000, 84000, 86000)); // SK하이닉스
        stockStates.put("373220", new StockState("373220", 450000, 440000, 460000)); // LG에너지솔루션
        stockStates.put("207940", new StockState("207940", 780000, 770000, 790000)); // 삼성바이오로직스
        stockStates.put("005935", new StockState("005935", 65000, 64000, 66000)); // 삼성전자우
        stockStates.put("012450", new StockState("012450", 160000, 155000, 165000)); // 한화에어로스페이스
        stockStates.put("005380", new StockState("005380", 240000, 235000, 245000)); // 현대차
        stockStates.put("329180", new StockState("329180", 140000, 135000, 145000)); // HD현대중공업
        stockStates.put("034020", new StockState("034020", 15000, 14500, 15500)); // 두산에너빌리티
        stockStates.put("105560", new StockState("105560", 55000, 54000, 56000)); // KB금융
    }

    public String generateQuoteMessage(String stockCode) {
        try {
            StockState state = stockStates.get(stockCode);
            if (state == null) {
                return null;
            }

            // 가격 변동 시뮬레이션 (±2% 범위)
            double changeRate = (random.nextDouble() - 0.5) * 0.04; // -2% ~ +2%
            int newPrice = (int) (state.getCurrentPrice() * (1 + changeRate));
            
            // 범위 제한
            newPrice = Math.max(state.getMinPrice(), Math.min(state.getMaxPrice(), newPrice));
            
            int changeAmount = newPrice - state.getCurrentPrice();
            double changePercent = state.getCurrentPrice() > 0 ? 
                ((double) changeAmount / state.getCurrentPrice()) * 100 : 0;

            // 거래량 랜덤 생성
            int volume = random.nextInt(10000) + 1000;
            long accVolume = random.nextInt(1000000) + 100000; // 누적거래량

            state.setCurrentPrice(newPrice);
            state.updatePriceHistory(newPrice);

            // KIS API 실제 형식: ^ 구분자로 분리된 문자열
            // 형식: 0|H0STCNT0|001|종목코드^시간^현재가^전일대비부호^전일대비^전일대비율^...
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String priceChangeSign = changeAmount > 0 ? "2" : changeAmount < 0 ? "5" : "3"; // 2:상승, 5:하락, 3:보합
            
            StringBuilder message = new StringBuilder();
            message.append("0|H0STCNT0|001|")
                   .append(stockCode).append("^")          // 종목코드
                   .append(timestamp).append("^")          // 체결시간
                   .append(newPrice).append("^")           // 현재가
                   .append(priceChangeSign).append("^")    // 전일대비부호
                   .append(Math.abs(changeAmount)).append("^")  // 전일대비
                   .append(String.format("%.2f", Math.abs(changePercent))).append("^")  // 전일대비율
                   .append(String.format("%.2f", (double)newPrice)).append("^")  // 가중평균가격
                   .append(state.getOpenPrice()).append("^")     // 시가
                   .append(state.getHighPrice()).append("^")     // 고가
                   .append(state.getLowPrice()).append("^")      // 저가
                   .append(newPrice + 100).append("^")           // 매도호가1 (임시)
                   .append(newPrice - 100).append("^")           // 매수호가1 (임시)
                   .append(volume).append("^")                   // 체결거래량
                   .append(accVolume).append("^")                // 누적거래량
                   .append(accVolume * newPrice).append("^")     // 누적거래대금
                   .append(random.nextInt(1000) + 500).append("^")  // 매도체결건수
                   .append(random.nextInt(1000) + 500).append("^")  // 매수체결건수
                   .append(random.nextInt(200) - 100).append("^")   // 순매수체결건수
                   .append(String.format("%.2f", random.nextDouble() * 200)).append("^")  // 체결강도
                   .append(random.nextInt(100000) + 50000).append("^")  // 총매도수량
                   .append(random.nextInt(100000) + 50000).append("^")  // 총매수수량
                   .append("5^")  // 체결구분
                   .append(String.format("%.2f", random.nextDouble())).append("^")  // 매수비율
                   .append(String.format("%.2f", random.nextDouble() * 100)).append("^")  // 전일거래량대비등락율
                   .append("090000^")  // 시가시간
                   .append("2^")       // 시가대비구분
                   .append("100^")     // 시가대비
                   .append(timestamp).append("^")  // 최고가시간
                   .append("2^")       // 고가대비구분
                   .append("50^")      // 고가대비
                   .append(timestamp).append("^")  // 최저가시간
                   .append("5^")       // 저가대비구분
                   .append("80^")      // 저가대비
                   .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("^")  // 영업일자
                   .append("20^")      // 신장운영구분코드
                   .append("N^")       // 거래정지여부
                   .append(random.nextInt(50000) + 10000).append("^")  // 매도호가잔량
                   .append(random.nextInt(50000) + 10000).append("^")  // 매수호가잔량
                   .append(random.nextInt(200000) + 100000).append("^") // 총매도호가잔량
                   .append(random.nextInt(200000) + 100000).append("^") // 총매수호가잔량
                   .append(String.format("%.2f", random.nextDouble())).append("^")  // 거래량회전율
                   .append(random.nextInt(500000) + 100000).append("^") // 전일동시간누적거래량
                   .append(String.format("%.2f", random.nextDouble() * 100)).append("^")  // 전일동시간누적거래량비율
                   .append("0^")       // 시간구분코드
                   .append("^")        // 임의종료구분코드
                   .append(newPrice);  // 정적VI발동기준가

            return message.toString();
        } catch (Exception e) {
            log.error("Error generating quote message for stock: {}", stockCode, e);
            return null;
        }
    }

    public String generateOrderbookMessage(String stockCode) {
        try {
            StockState state = stockStates.get(stockCode);
            if (state == null) {
                return null;
            }

            int basePrice = state.getCurrentPrice();
            
            // 호가 생성 (현재가 기준으로 ±0.5% 범위)
            int bidPrice1 = basePrice - random.nextInt(100) - 50;
            int askPrice1 = basePrice + random.nextInt(100) + 50;
            
            KisOrderbookMessage message = KisOrderbookMessage.builder()
                    .tr_id("H0STASP0")
                    .tr_key(stockCode)
                    .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                    .bid_price_1(String.valueOf(bidPrice1))
                    .bid_volume_1(String.valueOf(random.nextInt(5000) + 1000))
                    .ask_price_1(String.valueOf(askPrice1))
                    .ask_volume_1(String.valueOf(random.nextInt(5000) + 1000))
                    .bid_price_2(String.valueOf(bidPrice1 - 100))
                    .bid_volume_2(String.valueOf(random.nextInt(3000) + 500))
                    .ask_price_2(String.valueOf(askPrice1 + 100))
                    .ask_volume_2(String.valueOf(random.nextInt(3000) + 500))
                    .bid_price_3(String.valueOf(bidPrice1 - 200))
                    .bid_volume_3(String.valueOf(random.nextInt(2000) + 300))
                    .ask_price_3(String.valueOf(askPrice1 + 200))
                    .ask_volume_3(String.valueOf(random.nextInt(2000) + 300))
                    .total_bid_volume(String.valueOf(random.nextInt(50000) + 10000))
                    .total_ask_volume(String.valueOf(random.nextInt(50000) + 10000))
                    .build();

            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error generating orderbook message for stock: {}", stockCode, e);
            return null;
        }
    }

    private static class StockState {
        private final String stockCode;
        private int currentPrice;
        private final int openPrice;
        private int highPrice;
        private int lowPrice;
        private final int minPrice;
        private final int maxPrice;

        public StockState(String stockCode, int basePrice, int minPrice, int maxPrice) {
            this.stockCode = stockCode;
            this.currentPrice = basePrice;
            this.openPrice = basePrice;
            this.highPrice = basePrice;
            this.lowPrice = basePrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }

        public void updatePriceHistory(int price) {
            if (price > highPrice) {
                highPrice = price;
            }
            if (price < lowPrice) {
                lowPrice = price;
            }
        }

        // Getters and Setters
        public String getStockCode() { return stockCode; }
        public int getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(int currentPrice) { this.currentPrice = currentPrice; }
        public int getOpenPrice() { return openPrice; }
        public int getHighPrice() { return highPrice; }
        public int getLowPrice() { return lowPrice; }
        public int getMinPrice() { return minPrice; }
        public int getMaxPrice() { return maxPrice; }
    }
}

