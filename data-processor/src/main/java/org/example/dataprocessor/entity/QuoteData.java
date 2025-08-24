package org.example.dataprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_data", indexes = {
    @Index(name = "idx_stock_code_time", columnList = "stock_code, trade_time"),
    @Index(name = "idx_trade_time", columnList = "trade_time"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;
    
    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price;
    
    @Column(name = "volume", nullable = false)
    private Long volume;
    
    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount;
    
    @Column(name = "change_rate", precision = 8, scale = 4)
    private BigDecimal changeRate;
    
    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;
    
    @Column(name = "low_price", precision = 15, scale = 2)
    private BigDecimal lowPrice;
    
    @Column(name = "open_price", precision = 15, scale = 2)
    private BigDecimal openPrice;
    
    @Column(name = "trade_time", nullable = false)
    private LocalDateTime tradeTime;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", referencedColumnName = "stock_code", insertable = false, updatable = false)
    private Stock stock;
}

