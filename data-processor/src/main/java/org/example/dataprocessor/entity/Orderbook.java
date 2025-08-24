package org.example.dataprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orderbooks", indexes = {
    @Index(name = "idx_stock_code_time", columnList = "stock_code, quote_time"),
    @Index(name = "idx_sequence", columnList = "sequence_number"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orderbook {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;
    
    @Column(name = "quote_time", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime quoteTime;
    
    @Column(name = "sequence_number")
    private Long sequenceNumber;
    
    @Column(name = "total_bid_volume")
    private Long totalBidVolume;
    
    @Column(name = "total_ask_volume")
    private Long totalAskVolume;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", referencedColumnName = "stock_code", insertable = false, updatable = false)
    private Stock stock;
    
    @OneToMany(mappedBy = "orderbook", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderbookLevel> orderbookLevels;
}

