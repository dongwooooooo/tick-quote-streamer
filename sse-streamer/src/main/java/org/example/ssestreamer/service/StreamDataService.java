package org.example.ssestreamer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssestreamer.dto.SseMessage;
import org.example.ssestreamer.dto.StreamOrderbookData;
import org.example.ssestreamer.dto.StreamQuoteData;
import org.example.ssestreamer.entity.QuoteData;
import org.example.ssestreamer.entity.Stock;
import org.example.ssestreamer.repository.QuoteDataRepository;
import org.example.ssestreamer.repository.StockRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamDataService {
    
    private final SseConnectionManager sseConnectionManager;
    private final StockRepository stockRepository;
    private final QuoteDataRepository quoteDataRepository;
    
    /**
     * 시세 데이터를 SSE로 브로드캐스트
     */
    public void broadcastQuoteData(String stockCode, StreamQuoteData quoteData) {
        try {
            // 종목명 정보 조회 및 설정
            Optional<Stock> stock = getStockInfo(stockCode);
            if (stock.isPresent()) {
                quoteData.setStockName(stock.get().getStockName());
            }
            
            // 시장 상태 설정
            quoteData.setMarketStatus(getCurrentMarketStatus());
            quoteData.setTimestamp(LocalDateTime.now());
            
            // SSE 메시지 생성 및 브로드캐스트
            SseMessage message = SseMessage.quote(stockCode, quoteData);
            sseConnectionManager.broadcastToStock(stockCode, message);
            
            log.debug("Quote data broadcasted for stock: {} to {} subscribers", 
                stockCode, sseConnectionManager.getSubscriberCount(stockCode));
                
        } catch (Exception e) {
            log.error("Error broadcasting quote data for stock: {}", stockCode, e);
        }
    }
    
    /**
     * 호가 데이터를 SSE로 브로드캐스트
     */
    public void broadcastOrderbookData(String stockCode, StreamOrderbookData orderbookData) {
        try {
            // 종목명 정보 조회 및 설정
            Optional<Stock> stock = getStockInfo(stockCode);
            if (stock.isPresent()) {
                orderbookData.setStockName(stock.get().getStockName());
            }
            
            orderbookData.setTimestamp(LocalDateTime.now());
            
            // SSE 메시지 생성 및 브로드캐스트
            SseMessage message = SseMessage.orderbook(stockCode, orderbookData);
            sseConnectionManager.broadcastToStock(stockCode, message);
            
            log.debug("Orderbook data broadcasted for stock: {} to {} subscribers", 
                stockCode, sseConnectionManager.getSubscriberCount(stockCode));
                
        } catch (Exception e) {
            log.error("Error broadcasting orderbook data for stock: {}", stockCode, e);
        }
    }
    
    /**
     * 종목 정보 조회 (캐시 적용)
     */
    @Cacheable(value = "stockInfo", key = "#stockCode")
    public Optional<Stock> getStockInfo(String stockCode) {
        return stockRepository.findByStockCode(stockCode);
    }
    
    /**
     * 최신 시세 데이터 조회 (캐시 적용)
     */
    @Cacheable(value = "recentQuotes", key = "#stockCode")
    public Optional<QuoteData> getLatestQuoteData(String stockCode) {
        return quoteDataRepository.findLatestByStockCode(stockCode);
    }
    
    /**
     * 현재 시장 상태 확인
     */
    private String getCurrentMarketStatus() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int timeInMinutes = hour * 60 + minute;
        
        // 9:00 - 15:30 (한국 주식시장 시간)
        int marketOpen = 9 * 60;      // 9:00
        int marketClose = 15 * 60 + 30; // 15:30
        
        if (timeInMinutes >= marketOpen && timeInMinutes <= marketClose) {
            return "OPEN";
        } else if (timeInMinutes < marketOpen) {
            return "PRE_MARKET";
        } else {
            return "AFTER_MARKET";
        }
    }
    
    /**
     * 연결 통계 조회
     */
    public Object getConnectionStats() {
        return sseConnectionManager.getConnectionStats();
    }
}

