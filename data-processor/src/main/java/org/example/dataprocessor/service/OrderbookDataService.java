package org.example.dataprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dataprocessor.dto.KisOrderbookMessage;
import org.example.dataprocessor.entity.Orderbook;
import org.example.dataprocessor.entity.OrderbookLevel;
import org.example.dataprocessor.repository.OrderbookRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderbookDataService {
    
    private final OrderbookRepository orderbookRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    @Transactional
    @CachePut(value = "latestOrderbooks", key = "#orderbookMessage.trKey")
    public Orderbook processOrderbookMessage(KisOrderbookMessage orderbookMessage) {
        try {
            log.debug("Processing orderbook message for stock: {}", orderbookMessage.getTrKey());
            
            // 중복 검사 (sequence_number 기반)
            if (orderbookMessage.getSequenceNumberAsLong() > 0) {
                Optional<Orderbook> existingOrderbook = orderbookRepository.findBySequenceNumber(
                    orderbookMessage.getSequenceNumberAsLong());
                if (existingOrderbook.isPresent()) {
                    log.debug("Duplicate orderbook message detected - sequence: {}", 
                        orderbookMessage.getSequenceNumberAsLong());
                    return existingOrderbook.get();
                }
            }
            
            // Orderbook 마스터 데이터 생성
            Orderbook orderbook = Orderbook.builder()
                .stockCode(orderbookMessage.getTrKey())
                .quoteTime(parseQuoteTime(orderbookMessage.getTimestamp()))
                .sequenceNumber(orderbookMessage.getSequenceNumberAsLong())
                .totalBidVolume(orderbookMessage.getTotalBidVolumeAsLong())
                .totalAskVolume(orderbookMessage.getTotalAskVolumeAsLong())
                .build();
            
            Orderbook savedOrderbook = orderbookRepository.save(orderbook);
            
            // OrderbookLevel 상세 데이터 생성
            List<OrderbookLevel> orderbookLevels = new ArrayList<>();
            
            // 매수호가 레벨들 추가
            List<KisOrderbookMessage.OrderbookLevelDto> bidLevels = orderbookMessage.getBidLevels();
            for (KisOrderbookMessage.OrderbookLevelDto bidLevel : bidLevels) {
                orderbookLevels.add(OrderbookLevel.builder()
                    .orderbookId(savedOrderbook.getId())
                    .orderType(OrderbookLevel.OrderType.BID)
                    .priceLevel(bidLevel.getPriceLevel())
                    .price(bidLevel.getPrice())
                    .volume(bidLevel.getVolume())
                    .build());
            }
            
            // 매도호가 레벨들 추가
            List<KisOrderbookMessage.OrderbookLevelDto> askLevels = orderbookMessage.getAskLevels();
            for (KisOrderbookMessage.OrderbookLevelDto askLevel : askLevels) {
                orderbookLevels.add(OrderbookLevel.builder()
                    .orderbookId(savedOrderbook.getId())
                    .orderType(OrderbookLevel.OrderType.ASK)
                    .priceLevel(askLevel.getPriceLevel())
                    .price(askLevel.getPrice())
                    .volume(askLevel.getVolume())
                    .build());
            }
            
            savedOrderbook.setOrderbookLevels(orderbookLevels);
            
            log.debug("Successfully saved orderbook data - ID: {}, Stock: {}, Levels: {}", 
                savedOrderbook.getId(), savedOrderbook.getStockCode(), orderbookLevels.size());
            
            return savedOrderbook;
            
        } catch (Exception e) {
            log.error("Error processing orderbook message for stock: {}", orderbookMessage.getTrKey(), e);
            throw new RuntimeException("Failed to process orderbook message", e);
        }
    }
    
    @Cacheable(value = "latestOrderbooks", key = "#stockCode")
    public Optional<Orderbook> getLatestOrderbook(String stockCode) {
        return orderbookRepository.findLatestByStockCode(stockCode);
    }
    
    public List<Orderbook> getRecentOrderbooks(String stockCode, int limit) {
        return orderbookRepository.findByStockCodeOrderByQuoteTimeDesc(stockCode, 
            org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    public List<Orderbook> getOrderbooksBetween(String stockCode, LocalDateTime startTime, LocalDateTime endTime) {
        return orderbookRepository.findByStockCodeAndQuoteTimeBetween(stockCode, startTime, endTime);
    }
    
    public long getOrderbookCount(String stockCode, LocalDateTime afterTime) {
        return orderbookRepository.countByStockCodeAndQuoteTimeAfter(stockCode, afterTime);
    }
    
    private LocalDateTime parseQuoteTime(String timestamp) {
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

