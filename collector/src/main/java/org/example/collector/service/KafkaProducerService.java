package org.example.collector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.collector.dto.KisOrderbookData;
import org.example.collector.dto.KisQuoteData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaProducerService {

    private static final String QUOTE_TOPIC = "quote-stream";
    private static final String ORDERBOOK_TOPIC = "orderbook-stream";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendQuoteMessage(KisQuoteData quoteData) {
        try {
            String stockCode = quoteData.getTr_key();
            String message = objectMapper.writeValueAsString(quoteData);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(QUOTE_TOPIC, stockCode, message);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send quote message for stock: {}", stockCode, throwable);
                } else {
                    log.debug("Quote message sent successfully for stock: {} to partition: {}", 
                            stockCode, result.getRecordMetadata().partition());
                }
            });
            
        } catch (Exception e) {
            log.error("Error processing quote message", e);
        }
    }

    public void sendOrderbookMessage(KisOrderbookData orderbookData) {
        try {
            String stockCode = orderbookData.getTr_key();
            String message = objectMapper.writeValueAsString(orderbookData);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(ORDERBOOK_TOPIC, stockCode, message);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send orderbook message for stock: {}", stockCode, throwable);
                } else {
                    log.debug("Orderbook message sent successfully for stock: {} to partition: {}", 
                            stockCode, result.getRecordMetadata().partition());
                }
            });
            
        } catch (Exception e) {
            log.error("Error processing orderbook message", e);
        }
    }
}