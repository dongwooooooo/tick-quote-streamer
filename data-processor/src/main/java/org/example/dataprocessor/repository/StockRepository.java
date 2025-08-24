package org.example.dataprocessor.repository;

import org.example.dataprocessor.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    
    Optional<Stock> findByStockCode(String stockCode);
    
    List<Stock> findByMarketType(String marketType);
    
    @Query("SELECT s FROM Stock s WHERE s.stockName LIKE %:keyword%")
    List<Stock> findByStockNameContaining(String keyword);
    
    @Query("SELECT s.stockCode FROM Stock s")
    List<String> findAllStockCodes();
}

