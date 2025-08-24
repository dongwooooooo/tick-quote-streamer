package org.example.dataprocessor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dataprocessor.entity.QuoteData;
import org.example.dataprocessor.entity.Orderbook;
import org.example.dataprocessor.service.QuoteDataService;
import org.example.dataprocessor.service.OrderbookDataService;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/data-processor")
@RequiredArgsConstructor
public class DataProcessorController {
    
    private final QuoteDataService quoteDataService;
    private final OrderbookDataService orderbookDataService;
    private final CacheManager cacheManager;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        // 캐시 통계
        Map<String, Object> cacheStats = new HashMap<>();
        if (cacheManager.getCache("latestQuotes") != null) {
            cacheStats.put("latestQuotes", "available");
        }
        if (cacheManager.getCache("latestOrderbooks") != null) {
            cacheStats.put("latestOrderbooks", "available");
        }
        health.put("cache", cacheStats);
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/quotes/{stockCode}/latest")
    public ResponseEntity<QuoteData> getLatestQuote(@PathVariable String stockCode) {
        Optional<QuoteData> latestQuote = quoteDataService.getLatestQuoteData(stockCode);
        return latestQuote.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/quotes/{stockCode}")
    public ResponseEntity<List<QuoteData>> getRecentQuotes(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<QuoteData> quotes = quoteDataService.getRecentQuoteData(stockCode, limit);
        return ResponseEntity.ok(quotes);
    }
    
    @GetMapping("/orderbooks/{stockCode}/latest")
    public ResponseEntity<Orderbook> getLatestOrderbook(@PathVariable String stockCode) {
        Optional<Orderbook> latestOrderbook = orderbookDataService.getLatestOrderbook(stockCode);
        return latestOrderbook.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/orderbooks/{stockCode}")
    public ResponseEntity<List<Orderbook>> getRecentOrderbooks(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "5") int limit
    ) {
        List<Orderbook> orderbooks = orderbookDataService.getRecentOrderbooks(stockCode, limit);
        return ResponseEntity.ok(orderbooks);
    }
    
    @GetMapping("/stats/{stockCode}")
    public ResponseEntity<Map<String, Object>> getStockStats(@PathVariable String stockCode) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("stockCode", stockCode);
        stats.put("quoteCount", quoteDataService.getQuoteCount(stockCode, oneHourAgo));
        stats.put("orderbookCount", orderbookDataService.getOrderbookCount(stockCode, oneHourAgo));
        stats.put("timeRange", "last 1 hour");
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                }
            });
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All caches cleared");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

