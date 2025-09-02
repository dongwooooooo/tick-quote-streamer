package org.example.dataprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dataprocessor.dto.KisOrderbookMessage;
import org.example.dataprocessor.service.OrderbookDataService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderbookStreamConsumer {
    
    private final OrderbookDataService orderbookDataService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
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
        Timer.Sample sample = Timer.start();
        try {
            log.debug("Received orderbook message - Topic: {}, Partition: {}, Key: {}, Offset: {}", 
                topic, partition, key, offset);
            
            // JSON 메시지 파싱
            KisOrderbookMessage orderbookMessage = objectMapper.readValue(message, KisOrderbookMessage.class);
            
            // 데이터 처리 및 저장
            orderbookDataService.processOrderbookMessage(orderbookMessage);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed orderbook message for stock: {}", orderbookMessage.getTrKey());
            meterRegistry.counter("data_processor_messages_total", "type", "orderbook", "symbol", orderbookMessage.getTrKey()).increment();
            sample.stop(Timer.builder("data_processor_processing_seconds")
                .description("Orderbook message processing time")
                .tag("type", "orderbook")
                .tag("symbol", orderbookMessage.getTrKey())
                .register(meterRegistry));
            
        } catch (Exception e) {
            log.error("Error processing orderbook message - Topic: {}, Partition: {}, Key: {}, Offset: {}, Message: {}", 
                topic, partition, key, offset, message, e);
            
            // 에러 발생 시에도 일단 acknowledge (DLQ 처리는 추후 구현)
            acknowledgment.acknowledge();
            meterRegistry.counter("data_processor_failures_total", "type", "orderbook").increment();
        }
    }
}
