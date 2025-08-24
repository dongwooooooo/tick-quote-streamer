package org.example.ssestreamer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssestreamer.dto.KisOrderbookMessage;
import org.example.ssestreamer.dto.StreamOrderbookData;
import org.example.ssestreamer.service.StreamDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamOrderbookConsumer {
    
    private final StreamDataService streamDataService;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    @KafkaListener(
        topics = "${app.kafka.topics.orderbook-stream:orderbook-stream}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderbookMessage(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received orderbook message for streaming - Topic: {}, Partition: {}, Key: {}, Offset: {}", 
                topic, partition, key, offset);
            
            // JSON 메시지 파싱
            KisOrderbookMessage orderbookMessage = objectMapper.readValue(message, KisOrderbookMessage.class);
            
            // Stream DTO로 변환
            StreamOrderbookData streamOrderbookData = convertToStreamOrderbookData(orderbookMessage);
            
            // SSE 브로드캐스트
            streamDataService.broadcastOrderbookData(orderbookMessage.getTrKey(), streamOrderbookData);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.trace("Orderbook message streamed successfully for stock: {}", orderbookMessage.getTrKey());
            
        } catch (Exception e) {
            log.error("Error processing orderbook message for streaming - Topic: {}, Key: {}, Message: {}", 
                topic, key, message, e);
            
            // 에러 발생 시에도 acknowledge (스트리밍은 실시간성이 중요)
            acknowledgment.acknowledge();
        }
    }
    
    /**
     * KIS 메시지를 스트림용 DTO로 변환
     */
    private StreamOrderbookData convertToStreamOrderbookData(KisOrderbookMessage kisMessage) {
        // 매수호가 레벨 변환
        List<StreamOrderbookData.OrderbookLevelData> bidLevels = 
            kisMessage.getBidLevels().stream()
                .map(level -> StreamOrderbookData.OrderbookLevelData.builder()
                    .level(level.getPriceLevel())
                    .price(level.getPrice())
                    .volume(level.getVolume())
                    .build())
                .collect(Collectors.toList());
        
        // 매도호가 레벨 변환
        List<StreamOrderbookData.OrderbookLevelData> askLevels = 
            kisMessage.getAskLevels().stream()
                .map(level -> StreamOrderbookData.OrderbookLevelData.builder()
                    .level(level.getPriceLevel())
                    .price(level.getPrice())
                    .volume(level.getVolume())
                    .build())
                .collect(Collectors.toList());
        
        return StreamOrderbookData.builder()
            .stockCode(kisMessage.getTrKey())
            .quoteTime(parseQuoteTime(kisMessage.getTimestamp()))
            .sequenceNumber(kisMessage.getSequenceNumberAsLong())
            .totalBidVolume(kisMessage.getTotalBidVolumeAsLong())
            .totalAskVolume(kisMessage.getTotalAskVolumeAsLong())
            .bidLevels(bidLevels)
            .askLevels(askLevels)
            .build();
    }
    
    /**
     * KIS 타임스탬프를 LocalDateTime으로 변환
     */
    private LocalDateTime parseQuoteTime(String timestamp) {
        try {
            if (timestamp != null && timestamp.length() == 6) {
                LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
                return today.with(java.time.LocalTime.parse(timestamp, TIME_FORMATTER));
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            log.debug("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }
}

