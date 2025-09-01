package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationMessage;
import org.example.notificationservice.entity.NotificationHistory;
import org.example.notificationservice.repository.NotificationHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    
    private final NotificationHistoryRepository historyRepository;
    
    @Value("${app.notification.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    // 알림 전송용 별도 스레드 풀
    private final Executor deliveryExecutor = Executors.newFixedThreadPool(8);
    
    /**
     * 알림 전송 처리 (메인 진입점)
     */
    public boolean deliverNotification(NotificationMessage notification) {
        try {
            // 비동기로 알림 전송 처리
            CompletableFuture<Boolean> deliveryFuture = CompletableFuture.supplyAsync(() -> {
                return processNotificationDelivery(notification);
            }, deliveryExecutor);
            
            // 결과 대기 (타임아웃 설정)
            return deliveryFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Error in notification delivery process - ID: {}", notification.getNotificationId(), e);
            updateHistoryAsFailed(notification.getNotificationId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 알림 전송 실제 처리
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    private boolean processNotificationDelivery(NotificationMessage notification) {
        try {
            log.debug("Processing notification delivery - ID: {}, Channel: {}, Priority: {}", 
                notification.getNotificationId(), notification.getChannel(), notification.getPriority());
            
            boolean success = false;
            
            // 채널별 전송 처리
            switch (notification.getChannel()) {
                case PUSH:
                    success = sendPushNotification(notification);
                    break;
                case EMAIL:
                    success = sendEmailNotification(notification);
                    break;
                case SMS:
                    success = sendSmsNotification(notification);
                    break;
                case WEBHOOK:
                    success = sendWebhookNotification(notification);
                    break;
                default:
                    log.warn("Unsupported notification channel: {}", notification.getChannel());
                    success = false;
            }
            
            // 이력 업데이트
            if (success) {
                updateHistoryAsSent(notification.getNotificationId());
            } else {
                updateHistoryAsFailed(notification.getNotificationId(), "Delivery failed for channel: " + notification.getChannel());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Error processing notification delivery - ID: {}", notification.getNotificationId(), e);
            updateHistoryAsFailed(notification.getNotificationId(), e.getMessage());
            throw e; // Retry 처리를 위해 예외 재던짐
        }
    }
    
    /**
     * 푸시 알림 전송
     */
    private boolean sendPushNotification(NotificationMessage notification) {
        try {
            log.info("Sending PUSH notification - User: {}, Message: {}", 
                notification.getUserId(), notification.getMessage());
            
            // TODO: 실제 푸시 알림 서비스 연동 (Firebase, APNS 등)
            // 현재는 로그로 시뮬레이션
            
            // 우선순위에 따른 처리
            if (notification.getPriority() == NotificationMessage.Priority.URGENT) {
                log.warn("🚨 URGENT PUSH: {} - {}", notification.getStockName(), notification.getMessage());
            } else {
                log.info("📱 PUSH: {} - {}", notification.getStockName(), notification.getMessage());
            }
            
            // 시뮬레이션 딜레이
            Thread.sleep(100);
            
            return true; // 성공으로 시뮬레이션
            
        } catch (Exception e) {
            log.error("Failed to send push notification - User: {}", notification.getUserId(), e);
            return false;
        }
    }
    
    /**
     * 이메일 알림 전송
     */
    private boolean sendEmailNotification(NotificationMessage notification) {
        try {
            log.info("Sending EMAIL notification - User: {}, Message: {}", 
                notification.getUserId(), notification.getMessage());
            
            // TODO: 실제 이메일 서비스 연동
            log.info("📧 EMAIL: {} - {}", notification.getStockName(), notification.getMessage());
            
            Thread.sleep(200);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send email notification - User: {}", notification.getUserId(), e);
            return false;
        }
    }
    
    /**
     * SMS 알림 전송
     */
    private boolean sendSmsNotification(NotificationMessage notification) {
        try {
            log.info("Sending SMS notification - User: {}, Message: {}", 
                notification.getUserId(), notification.getMessage());
            
            // TODO: 실제 SMS 서비스 연동
            log.info("📲 SMS: {} - {}", notification.getStockName(), notification.getMessage());
            
            Thread.sleep(150);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send SMS notification - User: {}", notification.getUserId(), e);
            return false;
        }
    }
    
    /**
     * 웹훅 알림 전송
     */
    private boolean sendWebhookNotification(NotificationMessage notification) {
        try {
            log.info("Sending WEBHOOK notification - User: {}, Message: {}", 
                notification.getUserId(), notification.getMessage());
            
            // TODO: 실제 웹훅 호출
            log.info("🔗 WEBHOOK: {} - {}", notification.getStockName(), notification.getMessage());
            
            Thread.sleep(300);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send webhook notification - User: {}", notification.getUserId(), e);
            return false;
        }
    }
    
    /**
     * 이력을 전송 성공으로 업데이트
     */
    @Transactional
    protected void updateHistoryAsSent(Long notificationId) {
        try {
            if (notificationId != null) {
                historyRepository.findById(notificationId).ifPresent(history -> {
                    history.markAsSent();
                    historyRepository.save(history);
                    log.debug("Notification history updated as SENT - ID: {}", notificationId);
                });
            }
        } catch (Exception e) {
            log.error("Error updating notification history as sent - ID: {}", notificationId, e);
        }
    }
    
    /**
     * 이력을 전송 실패로 업데이트
     */
    @Transactional
    protected void updateHistoryAsFailed(Long notificationId, String errorMessage) {
        try {
            if (notificationId != null) {
                historyRepository.findById(notificationId).ifPresent(history -> {
                    history.markAsFailed(errorMessage);
                    historyRepository.save(history);
                    log.debug("Notification history updated as FAILED - ID: {}", notificationId);
                });
            }
        } catch (Exception e) {
            log.error("Error updating notification history as failed - ID: {}", notificationId, e);
        }
    }
    
    /**
     * 전송 통계 조회
     */
    public DeliveryStats getDeliveryStats() {
        long sentCount = historyRepository.countByStatus(NotificationHistory.NotificationStatus.SENT);
        long failedCount = historyRepository.countByStatus(NotificationHistory.NotificationStatus.FAILED);
        long pendingCount = historyRepository.countByStatus(NotificationHistory.NotificationStatus.PENDING);
        
        return DeliveryStats.builder()
            .sentCount(sentCount)
            .failedCount(failedCount)
            .pendingCount(pendingCount)
            .totalCount(sentCount + failedCount + pendingCount)
            .successRate(sentCount + failedCount > 0 ? (double) sentCount / (sentCount + failedCount) * 100 : 0.0)
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DeliveryStats {
        private long sentCount;
        private long failedCount;
        private long pendingCount;
        private long totalCount;
        private double successRate;
    }
}
