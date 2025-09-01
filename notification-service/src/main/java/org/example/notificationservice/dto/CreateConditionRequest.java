package org.example.notificationservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.notificationservice.entity.NotificationCondition;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConditionRequest {
    
    private String userId;
    
    private String stockCode;
    
    private NotificationCondition.ConditionType conditionType;
    
    private BigDecimal targetValue;
    
    private String description;
    
    /**
     * Entity로 변환
     */
    public NotificationCondition toEntity() {
        return NotificationCondition.builder()
            .userId(userId)
            .stockCode(stockCode)
            .conditionType(conditionType)
            .targetValue(targetValue)
            .description(description)
            .isActive(true)
            .build();
    }
}
