package org.example.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.notificationservice.entity.NotificationCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {
    
    @JsonProperty("notification_id")
    private Long notificationId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("stock_code")
    private String stockCode;
    
    @JsonProperty("stock_name")
    private String stockName;
    
    @JsonProperty("condition_type")
    private NotificationCondition.ConditionType conditionType;
    
    @JsonProperty("target_value")
    private BigDecimal targetValue;
    
    @JsonProperty("triggered_value")
    private BigDecimal triggeredValue;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("triggered_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggeredAt;
    
    @JsonProperty("priority")
    @Builder.Default
    private Priority priority = Priority.NORMAL;
    
    @JsonProperty("channel")
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.PUSH;
    
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    public enum NotificationChannel {
        PUSH,    // 푸시 알림
        EMAIL,   // 이메일
        SMS,     // SMS
        WEBHOOK  // 웹훅
    }
    
    /**
     * 알림 메시지 생성 헬퍼 메서드
     */
    public static NotificationMessage createPriceAlert(
        String userId, 
        String stockCode, 
        String stockName,
        NotificationCondition.ConditionType conditionType,
        BigDecimal targetValue,
        BigDecimal currentValue
    ) {
        String message = generateMessage(stockName, conditionType, targetValue, currentValue);
        Priority priority = determinePriority(conditionType, targetValue, currentValue);
        
        return NotificationMessage.builder()
            .userId(userId)
            .stockCode(stockCode)
            .stockName(stockName)
            .conditionType(conditionType)
            .targetValue(targetValue)
            .triggeredValue(currentValue)
            .message(message)
            .triggeredAt(LocalDateTime.now())
            .priority(priority)
            .channel(NotificationChannel.PUSH)
            .build();
    }
    
    /**
     * 알림 메시지 생성
     */
    private static String generateMessage(
        String stockName, 
        NotificationCondition.ConditionType conditionType,
        BigDecimal targetValue,
        BigDecimal currentValue
    ) {
        switch (conditionType) {
            case PRICE_ABOVE:
                return String.format("%s이 목표가 %,.0f원을 돌파했습니다! (현재가: %,.0f원)", 
                    stockName, targetValue, currentValue);
            case PRICE_BELOW:
                return String.format("%s이 목표가 %,.0f원 아래로 떨어졌습니다! (현재가: %,.0f원)", 
                    stockName, targetValue, currentValue);
            case VOLUME_ABOVE:
                return String.format("%s에서 거래량이 급증했습니다! (목표: %,.0f, 현재: %,.0f)", 
                    stockName, targetValue, currentValue);
            case CHANGE_RATE_ABOVE:
                return String.format("%s이 급상승했습니다! (목표: %,.2f%%, 현재: %,.2f%%)", 
                    stockName, targetValue, currentValue);
            case CHANGE_RATE_BELOW:
                return String.format("%s이 급하락했습니다! (목표: %,.2f%%, 현재: %,.2f%%)", 
                    stockName, targetValue, currentValue);
            default:
                return String.format("%s 알림이 발생했습니다.", stockName);
        }
    }
    
    /**
     * 우선순위 결정
     */
    private static Priority determinePriority(
        NotificationCondition.ConditionType conditionType,
        BigDecimal targetValue,
        BigDecimal currentValue
    ) {
        // 변동률 기반 급상승/급하락은 긴급
        if (conditionType == NotificationCondition.ConditionType.CHANGE_RATE_ABOVE ||
            conditionType == NotificationCondition.ConditionType.CHANGE_RATE_BELOW) {
            
            BigDecimal changeRate = currentValue.abs();
            if (changeRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
                return Priority.URGENT;
            } else if (changeRate.compareTo(BigDecimal.valueOf(5)) >= 0) {
                return Priority.HIGH;
            }
        }
        
        // 거래량 급증은 높은 우선순위
        if (conditionType == NotificationCondition.ConditionType.VOLUME_ABOVE) {
            return Priority.HIGH;
        }
        
        return Priority.NORMAL;
    }
}

