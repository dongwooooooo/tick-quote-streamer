package org.example.ssestreamer.repository;

import org.example.ssestreamer.entity.QuoteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuoteDataRepository extends JpaRepository<QuoteData, Long> {
    
    @Query("SELECT q FROM QuoteData q WHERE q.stockCode = :stockCode ORDER BY q.tradeTime DESC LIMIT 1")
    Optional<QuoteData> findLatestByStockCode(@Param("stockCode") String stockCode);
}

