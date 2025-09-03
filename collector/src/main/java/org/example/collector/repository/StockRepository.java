package org.example.collector.repository;

import org.example.collector.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    @Query("select s.stockCode from Stock s where s.stockName in :stockNames")
    List<String> findAllByStockNameIsIn(List<String> stockNames);
}
