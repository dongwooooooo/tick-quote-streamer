package org.example.ssestreamer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssestreamer.dto.SseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionManager {
    
    private final ObjectMapper objectMapper;
    
    @Value("${app.sse.connection-timeout:300000}")
    private long connectionTimeout;
    
    @Value("${app.sse.heartbeat-interval:30000}")
    private long heartbeatInterval;
    
    @Value("${app.sse.max-connections:10000}")
    private int maxConnections;
    
    // 클라이언트 연결 관리
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();
    
    // 클라이언트별 구독 종목 관리
    private final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    
    // 종목별 구독자 관리
    private final Map<String, Set<String>> stockSubscribers = new ConcurrentHashMap<>();
    
    // 클라이언트별 메시지 버퍼
    private final Map<String, Queue<SseMessage>> clientBuffers = new ConcurrentHashMap<>();
    
    // 연결 통계
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    
    // 하트비트 스케줄러
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    
    // 생성자에서 하트비트 스케줄링 시작
    @jakarta.annotation.PostConstruct
    public void initHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 
            heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createConnection(String clientId, List<String> stockCodes) {
        if (connections.size() >= maxConnections) {
            log.warn("Maximum connections reached: {}", maxConnections);
            throw new RuntimeException("Maximum connections exceeded");
        }
        
        SseEmitter emitter = new SseEmitter(connectionTimeout);
        connections.put(clientId, emitter);
        clientSubscriptions.put(clientId, new HashSet<>(stockCodes));
        clientBuffers.put(clientId, new LinkedList<>());
        
        // 종목별 구독자 등록
        for (String stockCode : stockCodes) {
            stockSubscribers.computeIfAbsent(stockCode, k -> new HashSet<>()).add(clientId);
        }
        
        // 연결 완료/해제 핸들러
        emitter.onCompletion(() -> removeConnection(clientId));
        emitter.onTimeout(() -> removeConnection(clientId));
        emitter.onError(throwable -> {
            log.error("SSE connection error for client: {}", clientId, throwable);
            removeConnection(clientId);
        });
        
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        
        log.info("New SSE connection created - Client: {}, Stocks: {}, Active: {}", 
            clientId, stockCodes, activeConnections.get());
        
        // 구독 확인 메시지 전송
        for (String stockCode : stockCodes) {
            sendToClient(clientId, SseMessage.subscribeAck(stockCode));
        }
        
        return emitter;
    }
    
    /**
     * 특정 종목의 모든 구독자에게 메시지 브로드캐스트
     */
    public void broadcastToStock(String stockCode, SseMessage message) {
        Set<String> subscribers = stockSubscribers.get(stockCode);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        
        log.debug("Broadcasting to {} subscribers for stock: {}", subscribers.size(), stockCode);
        
        List<String> disconnectedClients = new ArrayList<>();
        
        for (String clientId : subscribers) {
            try {
                if (!sendToClient(clientId, message)) {
                    disconnectedClients.add(clientId);
                }
            } catch (Exception e) {
                log.warn("Failed to send message to client: {}", clientId, e);
                disconnectedClients.add(clientId);
            }
        }
        
        // 연결이 끊긴 클라이언트들 정리
        for (String clientId : disconnectedClients) {
            removeConnection(clientId);
        }
    }
    
    /**
     * 특정 클라이언트에게 메시지 전송
     */
    public boolean sendToClient(String clientId, SseMessage message) {
        SseEmitter emitter = connections.get(clientId);
        if (emitter == null) {
            return false;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            emitter.send(SseEmitter.event()
                .name(message.getType().name().toLowerCase())
                .data(jsonMessage));
            
            log.trace("Message sent to client: {} - Type: {}", clientId, message.getType());
            return true;
            
        } catch (IOException e) {
            log.warn("Failed to send SSE message to client: {}", clientId, e);
            return false;
        }
    }
    
    /**
     * 연결 제거
     */
    private void removeConnection(String clientId) {
        SseEmitter emitter = connections.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for client: {}", clientId);
            }
        }
        
        // 구독 정보 정리
        Set<String> subscribedStocks = clientSubscriptions.remove(clientId);
        if (subscribedStocks != null) {
            for (String stockCode : subscribedStocks) {
                Set<String> subscribers = stockSubscribers.get(stockCode);
                if (subscribers != null) {
                    subscribers.remove(clientId);
                    if (subscribers.isEmpty()) {
                        stockSubscribers.remove(stockCode);
                    }
                }
            }
        }
        
        clientBuffers.remove(clientId);
        activeConnections.decrementAndGet();
        
        log.info("SSE connection removed - Client: {}, Active: {}", clientId, activeConnections.get());
    }
    
    /**
     * 하트비트 전송
     */
    private void sendHeartbeat() {
        if (connections.isEmpty()) {
            return;
        }
        
        SseMessage heartbeat = SseMessage.heartbeat();
        List<String> disconnectedClients = new ArrayList<>();
        
        for (String clientId : connections.keySet()) {
            if (!sendToClient(clientId, heartbeat)) {
                disconnectedClients.add(clientId);
            }
        }
        
        for (String clientId : disconnectedClients) {
            removeConnection(clientId);
        }
        
        log.debug("Heartbeat sent to {} clients", connections.size());
    }
    
    /**
     * 연결 통계 조회
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", totalConnections.get());
        stats.put("activeConnections", activeConnections.get());
        stats.put("subscribedStocks", stockSubscribers.size());
        stats.put("timestamp", LocalDateTime.now());
        
        Map<String, Integer> stockStats = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : stockSubscribers.entrySet()) {
            stockStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("stockSubscriptions", stockStats);
        
        return stats;
    }
    
    /**
     * 특정 종목의 구독자 수 조회
     */
    public int getSubscriberCount(String stockCode) {
        Set<String> subscribers = stockSubscribers.get(stockCode);
        return subscribers != null ? subscribers.size() : 0;
    }
    
    /**
     * 모든 연결 정리 (애플리케이션 종료 시)
     */
    public void shutdown() {
        log.info("Shutting down SSE Connection Manager...");
        
        for (SseEmitter emitter : connections.values()) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter during shutdown");
            }
        }
        
        connections.clear();
        clientSubscriptions.clear();
        stockSubscribers.clear();
        clientBuffers.clear();
        
        heartbeatScheduler.shutdown();
        log.info("SSE Connection Manager shut down completed");
    }
}
