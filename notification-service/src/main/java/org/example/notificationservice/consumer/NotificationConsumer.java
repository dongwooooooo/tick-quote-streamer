package org.example.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationMessage;
import org.example.notificationservice.service.NotificationDeliveryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    
    private final NotificationDeliveryService notificationDeliveryService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${app.kafka.topics.notification-alerts:notification-alerts}",
        groupId = "notification-delivery-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotificationMessage(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received notification message - Topic: {}, Partition: {}, Key: {}, Offset: {}", 
                topic, partition, key, offset);
            
            // JSON 메시지 파싱
            NotificationMessage notification = objectMapper.readValue(message, NotificationMessage.class);
            
            // 알림 전송 처리
            boolean deliverySuccess = notificationDeliveryService.deliverNotification(notification);
            
            if (deliverySuccess) {
                log.info("Notification delivered successfully - ID: {}, User: {}, Stock: {}", 
                    notification.getNotificationId(), notification.getUserId(), notification.getStockCode());
            } else {
                log.warn("Notification delivery failed - ID: {}, User: {}, Stock: {}", 
                    notification.getNotificationId(), notification.getUserId(), notification.getStockCode());
            }
            
            // 수동 커밋 (성공/실패 관계없이 - 재시도는 별도 로직에서 처리)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing notification message - Topic: {}, Key: {}, Message: {}", 
                topic, key, message, e);
            
            // 파싱 에러 등은 재처리 불가능하므로 acknowledge
            acknowledgment.acknowledge();
        }
    }
}

