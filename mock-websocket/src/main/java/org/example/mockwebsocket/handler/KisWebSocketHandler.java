package org.example.mockwebsocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.mockwebsocket.dto.KisSubscribeMessage;
import org.example.mockwebsocket.service.MockDataService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KisWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final MockDataService mockDataService;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<WebSocketSession, SubscriptionInfo> sessionSubscriptions;

    public KisWebSocketHandler(ObjectMapper objectMapper, MockDataService mockDataService, ScheduledExecutorService scheduler) {
        this.objectMapper = objectMapper;
        this.mockDataService = mockDataService;
        this.scheduler = scheduler;
        this.sessionSubscriptions = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("Received message from {}: {}", session.getId(), payload);

            KisSubscribeMessage subscribeMessage = objectMapper.readValue(payload, KisSubscribeMessage.class);
            handleSubscription(session, subscribeMessage);

        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    private void handleSubscription(WebSocketSession session, KisSubscribeMessage subscribeMessage) {
        String trId = subscribeMessage.getBody().getInput().getTr_id();
        String stockCode = subscribeMessage.getBody().getInput().getTr_key();

        log.info("Subscription request - TR_ID: {}, Stock: {}", trId, stockCode);

        // 기존 구독 정보가 있다면 취소
        SubscriptionInfo existingInfo = sessionSubscriptions.get(session);
        if (existingInfo != null) {
            existingInfo.future.cancel(true);
        }

        // 새로운 구독 시작
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(stockCode, trId);
        
        if ("H0STCNT0".equals(trId)) {
            // 시세 데이터 전송
            subscriptionInfo.future = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (session.isOpen()) {
                        String quoteData = mockDataService.generateQuoteMessage(stockCode);
                        if (quoteData != null) {
                            session.sendMessage(new TextMessage(quoteData));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending quote data to session {}: {}", session.getId(), e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);
            
        } else if ("H0STASP0".equals(trId)) {
            // 호가 데이터 전송
            subscriptionInfo.future = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (session.isOpen()) {
                        String orderbookData = mockDataService.generateOrderbookMessage(stockCode);
                        if (orderbookData != null) {
                            session.sendMessage(new TextMessage(orderbookData));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending orderbook data to session {}: {}", session.getId(), e.getMessage());
                }
            }, 0, 2, TimeUnit.SECONDS);
        }

        sessionSubscriptions.put(session, subscriptionInfo);
        log.info("Started data streaming for session {} - Stock: {}, TR_ID: {}", 
                session.getId(), stockCode, trId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        
        // 구독 정보 정리
        SubscriptionInfo subscriptionInfo = sessionSubscriptions.remove(session);
        if (subscriptionInfo != null && subscriptionInfo.future != null) {
            subscriptionInfo.future.cancel(true);
            log.info("Cancelled subscription for session: {}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    private static class SubscriptionInfo {
        final String stockCode;
        final String trId;
        java.util.concurrent.ScheduledFuture<?> future;

        SubscriptionInfo(String stockCode, String trId) {
            this.stockCode = stockCode;
            this.trId = trId;
        }
    }
}
