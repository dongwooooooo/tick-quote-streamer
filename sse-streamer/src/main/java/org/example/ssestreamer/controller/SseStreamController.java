package org.example.ssestreamer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssestreamer.service.SseConnectionManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SseStreamController {
    
    private final SseConnectionManager sseConnectionManager;
    
    /**
     * 실시간 스트림 연결
     * GET /api/stream/connect?stocks=005930,000660&client_id=unique_id
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectStream(
        @RequestParam("stocks") String stocks,
        @RequestParam(value = "client_id", required = false) String clientId
    ) {
        // 클라이언트 ID가 없으면 생성
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = "client_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // 종목 코드 파싱
        List<String> stockCodes = Arrays.asList(stocks.split(","));
        
        log.info("New SSE connection request - Client: {}, Stocks: {}", clientId, stockCodes);
        
        try {
            return sseConnectionManager.createConnection(clientId, stockCodes);
        } catch (Exception e) {
            log.error("Failed to create SSE connection for client: {}", clientId, e);
            throw new RuntimeException("Failed to create stream connection: " + e.getMessage());
        }
    }
    
    /**
     * 연결 상태 확인
     * GET /api/stream/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStreamStatus() {
        Map<String, Object> stats = sseConnectionManager.getConnectionStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 특정 종목의 구독자 수 조회
     * GET /api/stream/subscribers/{stockCode}
     */
    @GetMapping("/subscribers/{stockCode}")
    public ResponseEntity<Map<String, Object>> getStockSubscribers(@PathVariable String stockCode) {
        int subscriberCount = sseConnectionManager.getSubscriberCount(stockCode);
        
        Map<String, Object> response = Map.of(
            "stockCode", stockCode,
            "subscriberCount", subscriberCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 헬스체크
     * GET /api/stream/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = Map.of(
            "status", "UP",
            "service", "sse-streamer",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        
        return ResponseEntity.ok(health);
    }
}

