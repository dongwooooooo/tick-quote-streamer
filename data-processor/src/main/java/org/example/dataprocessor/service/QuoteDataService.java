package org.example.dataprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dataprocessor.dto.KisQuoteMessage;
import org.example.dataprocessor.entity.QuoteData;
import org.example.dataprocessor.repository.QuoteDataRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteDataService {
    
    private final QuoteDataRepository quoteDataRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    @Transactional
    @CachePut(value = "latestQuotes", key = "#quoteMessage.trKey")
    public QuoteData processQuoteMessage(KisQuoteMessage quoteMessage) {
        try {
            log.debug("Processing quote message for stock: {}", quoteMessage.getTrKey());
            
            QuoteData quoteData = QuoteData.builder()
                .stockCode(quoteMessage.getTrKey())
                .price(quoteMessage.getPriceAsBigDecimal())
                .volume(quoteMessage.getVolumeAsLong())
                .changeAmount(quoteMessage.getChangeAmountAsBigDecimal())
                .changeRate(quoteMessage.getChangeRateAsBigDecimal())
                .highPrice(quoteMessage.getHighPriceAsBigDecimal())
                .lowPrice(quoteMessage.getLowPriceAsBigDecimal())
                .openPrice(quoteMessage.getOpenPriceAsBigDecimal())
                .tradeTime(parseTradeTime(quoteMessage.getTimestamp()))
                .build();
            
            QuoteData savedQuoteData = quoteDataRepository.save(quoteData);
            
            log.debug("Successfully saved quote data - ID: {}, Stock: {}, Price: {}", 
                savedQuoteData.getId(), savedQuoteData.getStockCode(), savedQuoteData.getPrice());
            
            return savedQuoteData;
            
        } catch (Exception e) {
            log.error("Error processing quote message for stock: {}", quoteMessage.getTrKey(), e);
            throw new RuntimeException("Failed to process quote message", e);
        }
    }
    
    @Cacheable(value = "latestQuotes", key = "#stockCode")
    public Optional<QuoteData> getLatestQuoteData(String stockCode) {
        return quoteDataRepository.findLatestByStockCode(stockCode);
    }
    
    public List<QuoteData> getRecentQuoteData(String stockCode, int limit) {
        return quoteDataRepository.findByStockCodeOrderByTradeTimeDesc(stockCode, 
            org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    public List<QuoteData> getQuoteDataBetween(String stockCode, LocalDateTime startTime, LocalDateTime endTime) {
        return quoteDataRepository.findByStockCodeAndTradeTimeBetween(stockCode, startTime, endTime);
    }
    
    public long getQuoteCount(String stockCode, LocalDateTime afterTime) {
        return quoteDataRepository.countByStockCodeAndTradeTimeAfter(stockCode, afterTime);
    }
    
    private LocalDateTime parseTradeTime(String timestamp) {
        try {
            // KIS API timestamp 형식: "HHmmss" (예: "234154")
            if (timestamp != null && timestamp.length() == 6) {
                LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
                return today.with(java.time.LocalTime.parse(timestamp, TIME_FORMATTER));
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }
}

