package org.example.dataprocessor.repository;

import org.example.dataprocessor.entity.Orderbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderbookRepository extends JpaRepository<Orderbook, Long> {
    
    List<Orderbook> findByStockCodeOrderByQuoteTimeDesc(String stockCode);
    
    List<Orderbook> findByStockCodeOrderByQuoteTimeDesc(String stockCode, Pageable pageable);
    
    @Query("SELECT o FROM Orderbook o WHERE o.stockCode = :stockCode AND o.quoteTime BETWEEN :startTime AND :endTime ORDER BY o.quoteTime DESC")
    List<Orderbook> findByStockCodeAndQuoteTimeBetween(
        @Param("stockCode") String stockCode, 
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT o FROM Orderbook o WHERE o.stockCode = :stockCode ORDER BY o.quoteTime DESC LIMIT 1")
    Optional<Orderbook> findLatestByStockCode(@Param("stockCode") String stockCode);
    
    @Query("SELECT o FROM Orderbook o WHERE o.quoteTime >= :afterTime ORDER BY o.quoteTime DESC")
    List<Orderbook> findRecentOrderbooks(@Param("afterTime") LocalDateTime afterTime);
    
    @Query("SELECT COUNT(o) FROM Orderbook o WHERE o.stockCode = :stockCode AND o.quoteTime >= :afterTime")
    long countByStockCodeAndQuoteTimeAfter(@Param("stockCode") String stockCode, @Param("afterTime") LocalDateTime afterTime);
    
    @Query("SELECT o FROM Orderbook o WHERE o.sequenceNumber = :sequenceNumber")
    Optional<Orderbook> findBySequenceNumber(@Param("sequenceNumber") Long sequenceNumber);
}

