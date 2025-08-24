package org.example.ssestreamer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SseMessage {
    
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("stock_code")
    private String stockCode;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("sequence")
    private Long sequence;
    
    public enum MessageType {
        QUOTE,          // 시세 데이터
        ORDERBOOK,      // 호가 데이터
        HEARTBEAT,      // 연결 유지용 하트비트
        SUBSCRIBE_ACK,  // 구독 확인
        ERROR,          // 에러 메시지
        MARKET_STATUS   // 시장 상태 변경
    }
    
    // 편의 메서드들
    public static SseMessage quote(String stockCode, StreamQuoteData quoteData) {
        return SseMessage.builder()
                .type(MessageType.QUOTE)
                .stockCode(stockCode)
                .data(quoteData)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static SseMessage orderbook(String stockCode, StreamOrderbookData orderbookData) {
        return SseMessage.builder()
                .type(MessageType.ORDERBOOK)
                .stockCode(stockCode)
                .data(orderbookData)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static SseMessage heartbeat() {
        return SseMessage.builder()
                .type(MessageType.HEARTBEAT)
                .data("ping")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static SseMessage subscribeAck(String stockCode) {
        return SseMessage.builder()
                .type(MessageType.SUBSCRIBE_ACK)
                .stockCode(stockCode)
                .data("subscribed")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static SseMessage error(String message) {
        return SseMessage.builder()
                .type(MessageType.ERROR)
                .data(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

