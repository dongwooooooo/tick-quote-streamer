package org.example.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.KisQuoteMessage;
import org.example.notificationservice.service.ConditionEvaluationService;
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
    
    private final ConditionEvaluationService conditionEvaluationService;
    private final ObjectMapper objectMapper;
    
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
            log.trace("Received quote message for condition evaluation - Topic: {}, Key: {}, Offset: {}", 
                topic, key, offset);
            
            // JSON 메시지 파싱
            KisQuoteMessage quoteMessage = objectMapper.readValue(message, KisQuoteMessage.class);
            
            // 조건 평가 수행
            conditionEvaluationService.evaluateQuoteConditions(
                quoteMessage.getTrKey(),
                quoteMessage.getPriceAsBigDecimal(),
                quoteMessage.getVolumeAsLong(),
                quoteMessage.getChangeRateAsBigDecimal()
            );
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.trace("Quote message processed for condition evaluation - Stock: {}", quoteMessage.getTrKey());
            
        } catch (Exception e) {
            log.error("Error processing quote message for condition evaluation - Topic: {}, Key: {}, Message: {}", 
                topic, key, message, e);
            
            // 에러 발생 시에도 acknowledge (알림은 최대한 실시간성 중시)
            acknowledgment.acknowledge();
        }
    }
}

