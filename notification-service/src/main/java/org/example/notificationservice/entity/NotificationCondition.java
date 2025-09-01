package org.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_conditions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_stock_code_active", columnList = "stock_code, is_active"),
    @Index(name = "idx_condition_type", columnList = "condition_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCondition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 20, nullable = false)
    private ConditionType conditionType;
    
    @Column(name = "target_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetValue;
    
    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ConditionType {
        PRICE_ABOVE("가격 상승"),      // 목표가 이상
        PRICE_BELOW("가격 하락"),      // 목표가 이하
        VOLUME_ABOVE("거래량 급증"),   // 거래량 이상
        CHANGE_RATE_ABOVE("급상승"),   // 변동률 이상
        CHANGE_RATE_BELOW("급하락");   // 변동률 이하
        
        private final String description;
        
        ConditionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 조건이 충족되었는지 확인
     */
    public boolean isConditionMet(BigDecimal currentPrice, Long currentVolume, BigDecimal changeRate) {
        switch (conditionType) {
            case PRICE_ABOVE:
                return currentPrice.compareTo(targetValue) >= 0;
            case PRICE_BELOW:
                return currentPrice.compareTo(targetValue) <= 0;
            case VOLUME_ABOVE:
                return currentVolume != null && 
                       BigDecimal.valueOf(currentVolume).compareTo(targetValue) >= 0;
            case CHANGE_RATE_ABOVE:
                return changeRate != null && changeRate.compareTo(targetValue) >= 0;
            case CHANGE_RATE_BELOW:
                return changeRate != null && changeRate.compareTo(targetValue) <= 0;
            default:
                return false;
        }
    }
    
    /**
     * 조건 트리거 처리
     */
    public void trigger(BigDecimal currentValue) {
        this.currentValue = currentValue;
        this.triggeredAt = LocalDateTime.now();
        this.isActive = false; // 한 번 트리거되면 비활성화
    }
    
    /**
     * 조건 재활성화
     */
    public void reactivate() {
        this.isActive = true;
        this.triggeredAt = null;
        this.currentValue = null;
    }
}

