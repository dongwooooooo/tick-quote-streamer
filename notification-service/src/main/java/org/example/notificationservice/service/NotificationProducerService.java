package org.example.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.notification-alerts:notification-alerts}")
    private String notificationTopic;
    
    /**
     * 알림 메시지 Kafka 전송
     */
    public void sendNotification(NotificationMessage notification) {
        try {
            String messageJson = objectMapper.writeValueAsString(notification);
            String key = generateMessageKey(notification);
            
            log.debug("Sending notification to Kafka - User: {}, Stock: {}, Type: {}", 
                notification.getUserId(), notification.getStockCode(), notification.getConditionType());
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(notificationTopic, key, messageJson);
            
            future.thenAccept(result -> {
                log.debug("Notification sent successfully - Offset: {}, Partition: {}", 
                    result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            }).exceptionally(throwable -> {
                log.error("Failed to send notification - User: {}, Stock: {}", 
                    notification.getUserId(), notification.getStockCode(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("Error serializing notification message - User: {}, Stock: {}", 
                notification.getUserId(), notification.getStockCode(), e);
        }
    }
    
    /**
     * 우선순위 기반 알림 전송
     */
    public void sendPriorityNotification(NotificationMessage notification) {
        try {
            // 우선순위가 높은 경우 별도 처리 (파티션 지정 등)
            if (notification.getPriority() == NotificationMessage.Priority.URGENT) {
                sendUrgentNotification(notification);
            } else {
                sendNotification(notification);
            }
            
        } catch (Exception e) {
            log.error("Error sending priority notification", e);
        }
    }
    
    /**
     * 긴급 알림 전송 (특정 파티션 사용)
     */
    private void sendUrgentNotification(NotificationMessage notification) {
        try {
            String messageJson = objectMapper.writeValueAsString(notification);
            String key = generateMessageKey(notification);
            
            // 긴급 알림은 특정 파티션(0번)으로 전송하여 우선 처리
            int urgentPartition = 0;
            
            log.info("Sending URGENT notification - User: {}, Stock: {}", 
                notification.getUserId(), notification.getStockCode());
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(notificationTopic, urgentPartition, key, messageJson);
            
            future.thenAccept(result -> {
                log.info("Urgent notification sent - Offset: {}, Partition: {}", 
                    result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            }).exceptionally(throwable -> {
                log.error("Failed to send urgent notification - User: {}, Stock: {}", 
                    notification.getUserId(), notification.getStockCode(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("Error sending urgent notification", e);
        }
    }
    
    /**
     * 배치 알림 전송
     */
    public void sendBatchNotifications(java.util.List<NotificationMessage> notifications) {
        try {
            log.debug("Sending batch notifications - Count: {}", notifications.size());
            
            for (NotificationMessage notification : notifications) {
                sendNotification(notification);
            }
            
            log.debug("Batch notifications sent successfully - Count: {}", notifications.size());
            
        } catch (Exception e) {
            log.error("Error sending batch notifications", e);
        }
    }
    
    /**
     * 메시지 키 생성 (사용자 ID 기반 파티셔닝)
     */
    private String generateMessageKey(NotificationMessage notification) {
        return notification.getUserId() + ":" + notification.getStockCode();
    }
    
    /**
     * 알림 재전송 (실패한 알림 재시도용)
     */
    public void resendNotification(NotificationMessage notification, int retryCount) {
        try {
            log.info("Resending notification - User: {}, Stock: {}, Retry: {}", 
                notification.getUserId(), notification.getStockCode(), retryCount);
            
            // 재전송 시 메타데이터 추가
            notification.setNotificationId(notification.getNotificationId()); // ID 유지
            
            sendNotification(notification);
            
        } catch (Exception e) {
            log.error("Error resending notification - Retry: {}", retryCount, e);
        }
    }
}

