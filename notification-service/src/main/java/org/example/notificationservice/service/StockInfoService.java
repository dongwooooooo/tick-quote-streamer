package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.entity.Stock;
import org.example.notificationservice.repository.StockRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfoService {
    
    private final StockRepository stockRepository;
    
    /**
     * 종목명 조회 (캐시 적용)
     */
    @Cacheable(value = "stockNames", key = "#stockCode")
    public Optional<String> getStockName(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
            .map(Stock::getStockName);
    }
    
    /**
     * 최신 주식 데이터 조회 (Mock 구현)
     * 실제로는 data-processor 서비스나 캐시에서 조회해야 함
     */
    public Optional<StockData> getLatestStockData(String stockCode) {
        // TODO: 실제로는 data-processor 서비스 호출 또는 캐시에서 조회
        // 현재는 Mock 데이터 반환
        log.debug("Getting latest stock data for: {} (Mock implementation)", stockCode);
        
        return Optional.of(StockData.builder()
            .stockCode(stockCode)
            .price(BigDecimal.valueOf(70000))
            .volume(1000L)
            .changeRate(BigDecimal.valueOf(1.5))
            .build());
    }
    
    @lombok.Data
    @lombok.Builder
    public static class StockData {
        private String stockCode;
        private BigDecimal price;
        private Long volume;
        private BigDecimal changeRate;
    }
}

