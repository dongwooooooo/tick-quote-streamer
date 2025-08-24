package org.example.dataprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "orderbook_levels", indexes = {
    @Index(name = "idx_orderbook_id", columnList = "orderbook_id"),
    @Index(name = "idx_orderbook_type_level", columnList = "orderbook_id, order_type, price_level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderbookLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "orderbook_id", nullable = false)
    private Long orderbookId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 4, nullable = false)
    private OrderType orderType;
    
    @Column(name = "price_level", nullable = false)
    private Integer priceLevel;
    
    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price;
    
    @Column(name = "volume", nullable = false)
    private Long volume;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderbook_id", insertable = false, updatable = false)
    private Orderbook orderbook;
    
    public enum OrderType {
        BID, ASK
    }
}

