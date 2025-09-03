package org.example.collector.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collector.domain.entity.type.StockReasonType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "hot_stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotStock {


    @Id
    @Column(name = "stock_code", length = 10)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private StockReasonType reason;

    @Column(name = "score", precision = 10, scale = 4, nullable = false) // Corrected column name and type for score
    private BigDecimal score;

    @Column(name = "designated_at", nullable = false) // Added the missing designated_at field
    private LocalDateTime designatedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

