package org.example.dataprocessor.repository;

import org.example.dataprocessor.entity.OrderbookLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderbookLevelRepository extends JpaRepository<OrderbookLevel, Long> {
    
    List<OrderbookLevel> findByOrderbookIdOrderByOrderTypeDescPriceLevelAsc(Long orderbookId);
    
    @Query("SELECT ol FROM OrderbookLevel ol WHERE ol.orderbookId = :orderbookId AND ol.orderType = :orderType ORDER BY ol.priceLevel ASC")
    List<OrderbookLevel> findByOrderbookIdAndOrderType(
        @Param("orderbookId") Long orderbookId, 
        @Param("orderType") OrderbookLevel.OrderType orderType
    );
    
    @Query("SELECT ol FROM OrderbookLevel ol WHERE ol.orderbookId IN :orderbookIds ORDER BY ol.orderbookId, ol.orderType DESC, ol.priceLevel ASC")
    List<OrderbookLevel> findByOrderbookIds(@Param("orderbookIds") List<Long> orderbookIds);
}

