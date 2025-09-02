package org.example.dataprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dataprocessor.dto.KisQuoteMessage;
import org.example.dataprocessor.service.QuoteDataService;
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
public class QuoteStreamConsumer {
    
    private final QuoteDataService quoteDataService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
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
        Timer.Sample sample = Timer.start();
        try {
            log.debug("Received quote message - Topic: {}, Partition: {}, Key: {}, Offset: {}", 
                topic, partition, key, offset);
            
            // JSON 메시지 파싱
            KisQuoteMessage quoteMessage = objectMapper.readValue(message, KisQuoteMessage.class);
            
            // 데이터 처리 및 저장
            quoteDataService.processQuoteMessage(quoteMessage);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed quote message for stock: {}", quoteMessage.getTrKey());
            meterRegistry.counter("data_processor_messages_total", "type", "quote", "symbol", quoteMessage.getTrKey()).increment();
            sample.stop(Timer.builder("data_processor_processing_seconds")
                .description("Quote message processing time")
                .tag("type", "quote")
                .tag("symbol", quoteMessage.getTrKey())
                .register(meterRegistry));
            
        } catch (Exception e) {
            log.error("Error processing quote message - Topic: {}, Partition: {}, Key: {}, Offset: {}, Message: {}", 
                topic, partition, key, offset, message, e);
            
            // 에러 발생 시에도 일단 acknowledge (DLQ 처리는 추후 구현)
            acknowledgment.acknowledge();
            meterRegistry.counter("data_processor_failures_total", "type", "quote").increment();
        }
    }
}
