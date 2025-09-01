package org.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_user_id_created", columnList = "user_id, created_at"),
    @Index(name = "idx_stock_code_created", columnList = "stock_code, created_at"),
    @Index(name = "idx_condition_id", columnList = "condition_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;
    
    @Column(name = "condition_id", nullable = false)
    private Long conditionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 20, nullable = false)
    private NotificationCondition.ConditionType conditionType;
    
    @Column(name = "target_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetValue;
    
    @Column(name = "triggered_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal triggeredValue;
    
    @Column(name = "message", length = 500, nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", insertable = false, updatable = false)
    private NotificationCondition condition;
    
    public enum NotificationStatus {
        PENDING("대기중"),
        SENT("전송완료"),
        FAILED("전송실패"),
        RETRY("재시도중");
        
        private final String description;
        
        NotificationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 알림 전송 성공 처리
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    /**
     * 알림 전송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 재시도 처리
     */
    public void incrementRetry() {
        this.retryCount++;
        this.status = NotificationStatus.RETRY;
    }
}

