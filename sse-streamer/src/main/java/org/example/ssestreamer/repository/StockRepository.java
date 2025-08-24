package org.example.ssestreamer.repository;

import org.example.ssestreamer.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    
    Optional<Stock> findByStockCode(String stockCode);
}

