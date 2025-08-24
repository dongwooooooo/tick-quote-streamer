package org.example.dataprocessor.repository;

import org.example.dataprocessor.entity.QuoteData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteDataRepository extends JpaRepository<QuoteData, Long> {
    
    List<QuoteData> findByStockCodeOrderByTradeTimeDesc(String stockCode);
    
    List<QuoteData> findByStockCodeOrderByTradeTimeDesc(String stockCode, Pageable pageable);
    
    @Query("SELECT q FROM QuoteData q WHERE q.stockCode = :stockCode AND q.tradeTime BETWEEN :startTime AND :endTime ORDER BY q.tradeTime DESC")
    List<QuoteData> findByStockCodeAndTradeTimeBetween(
        @Param("stockCode") String stockCode, 
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT q FROM QuoteData q WHERE q.stockCode = :stockCode ORDER BY q.tradeTime DESC LIMIT 1")
    Optional<QuoteData> findLatestByStockCode(@Param("stockCode") String stockCode);
    
    @Query("SELECT q FROM QuoteData q WHERE q.tradeTime >= :afterTime ORDER BY q.tradeTime DESC")
    List<QuoteData> findRecentQuotes(@Param("afterTime") LocalDateTime afterTime);
    
    @Query("SELECT COUNT(q) FROM QuoteData q WHERE q.stockCode = :stockCode AND q.tradeTime >= :afterTime")
    long countByStockCodeAndTradeTimeAfter(@Param("stockCode") String stockCode, @Param("afterTime") LocalDateTime afterTime);
}

