package org.example.ssestreamer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssestreamer.dto.KisQuoteMessage;
import org.example.ssestreamer.dto.StreamQuoteData;
import org.example.ssestreamer.service.StreamDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamQuoteConsumer {
    
    private final StreamDataService streamDataService;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    @KafkaListener(
        topics = "${app.kafka.topics.quote-stream:quote-stream}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeQuoteMessage(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received quote message for streaming - Topic: {}, Partition: {}, Key: {}, Offset: {}", 
                topic, partition, key, offset);
            
            // JSON 메시지 파싱
            KisQuoteMessage quoteMessage = objectMapper.readValue(message, KisQuoteMessage.class);
            
            // Stream DTO로 변환
            StreamQuoteData streamQuoteData = convertToStreamQuoteData(quoteMessage);
            
            // SSE 브로드캐스트
            streamDataService.broadcastQuoteData(quoteMessage.getTrKey(), streamQuoteData);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.trace("Quote message streamed successfully for stock: {}", quoteMessage.getTrKey());
            
        } catch (Exception e) {
            log.error("Error processing quote message for streaming - Topic: {}, Key: {}, Message: {}", 
                topic, key, message, e);
            
            // 에러 발생 시에도 acknowledge (스트리밍은 실시간성이 중요)
            acknowledgment.acknowledge();
        }
    }
    
    /**
     * KIS 메시지를 스트림용 DTO로 변환
     */
    private StreamQuoteData convertToStreamQuoteData(KisQuoteMessage kisMessage) {
        return StreamQuoteData.builder()
            .stockCode(kisMessage.getTrKey())
            .price(kisMessage.getPriceAsBigDecimal())
            .volume(kisMessage.getVolumeAsLong())
            .changeAmount(kisMessage.getChangeAmountAsBigDecimal())
            .changeRate(kisMessage.getChangeRateAsBigDecimal())
            .highPrice(kisMessage.getHighPriceAsBigDecimal())
            .lowPrice(kisMessage.getLowPriceAsBigDecimal())
            .openPrice(kisMessage.getOpenPriceAsBigDecimal())
            .tradeTime(parseTradeTime(kisMessage.getTimestamp()))
            .build();
    }
    
    /**
     * KIS 타임스탬프를 LocalDateTime으로 변환
     */
    private LocalDateTime parseTradeTime(String timestamp) {
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

