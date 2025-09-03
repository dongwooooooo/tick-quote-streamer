package org.example.collector.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.example.collector.config.KisWebSocketProperties;
import org.example.collector.dto.KisOrderbookData;
import org.example.collector.dto.KisQuoteData;
import org.example.collector.dto.KisSubscribeRequest;
import org.example.collector.service.KafkaProducerService;
import org.example.collector.service.KisAuthService;
import org.example.collector.service.StockService;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

@Slf4j
@Component
public class KisWebSocketClient {

    private final KisWebSocketProperties properties;
    private final KafkaProducerService kafkaProducerService;
    private final KisAuthService kisAuthService;
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final SSLContext sslContext;
    
    private WebSocketClient webSocketClient;
    private boolean isConnected = false;

    public KisWebSocketClient(
            KisWebSocketProperties properties,
            KafkaProducerService kafkaProducerService,
            KisAuthService kisAuthService,
            StockService stockService,
            ObjectMapper objectMapper,
            @Nullable SSLContext sslContext) {
        this.properties = properties;
        this.kafkaProducerService = kafkaProducerService;
        this.kisAuthService = kisAuthService;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
        this.sslContext = sslContext;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void connect() {
        try {
            // 먼저 인증 토큰과 승인키를 가져옴
            String accessToken = kisAuthService.getAccessToken();
            String approvalKey = kisAuthService.getWebSocketApprovalKey();
            
            if (accessToken == null || approvalKey == null) {
                log.error("Failed to obtain KIS API authentication tokens");
                scheduleReconnect();
                return;
            }
            
            URI serverUri = new URI(properties.getWebsocket().getDomain());
            log.info("Connecting to KIS WebSocket server: {}", serverUri);
            log.info("Using access token: {}...", accessToken.substring(0, Math.min(10, accessToken.length())));
            log.info("Using approval key: {}...", approvalKey.substring(0, Math.min(10, approvalKey.length())));
            
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("WebSocket connection opened to KIS server: {}", serverUri);
                    isConnected = true;
                    startSubscriptions(properties.getTargetStockNames());
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("WebSocket connection closed. Code: {}, Reason: {}, Remote: {}", 
                            code, reason, remote);
                    isConnected = false;
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket connection error", ex);
                    isConnected = false;
                }
            };
            
            // KIS API 인증 헤더 추가
            webSocketClient.addHeader("authorization", "Bearer " + accessToken);
            webSocketClient.addHeader("appkey", properties.getApp().getKey());
            webSocketClient.addHeader("appsecret", properties.getApp().getSecret());
            webSocketClient.addHeader("custtype", "P"); // 개인고객 타입
            webSocketClient.addHeader("tr_type", "1"); // 등록
            webSocketClient.addHeader("content-type", "utf-8");

            // SSL 설정 추가 (KIS API를 위한 인증서 검증 우회)  
            if (serverUri.getScheme().equals("wss")) {
                setupSSLContext();
                // WebSocket 클라이언트에 직접 SSL 설정 적용
                webSocketClient.setSocketFactory(javax.net.ssl.SSLSocketFactory.getDefault());
            }
            
            webSocketClient.connect();
            
        } catch (Exception e) {
            log.error("Failed to connect to KIS WebSocket server", e);
            scheduleReconnect();
        }
    }
    
    private void setupSSLContext() {
        try {
            // Spring의 SSL 설정 사용
            if (sslContext != null) {
                log.info("Spring SSL 설정을 WebSocket 클라이언트에 적용");
                if (webSocketClient != null) {
                    webSocketClient.setSocketFactory(sslContext.getSocketFactory());
                }
            } else {
                log.info("기본 SSL 설정 사용 (표준 인증서 검증)");
            }
        } catch (Exception e) {
            log.error("Failed to setup SSL for WebSocket", e);
        }
    }

    private void startSubscriptions(List<String> stockNames) {
        // 구독 시작을 약간 지연

        List<String> stockCodes = stockService.getTargetStockCodes(stockNames);
        scheduler.schedule(() -> {
            for (String stockCode : stockCodes) {
                subscribeToQuote(stockCode);
                subscribeToOrderbook(stockCode);
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void subscribeToQuote(String stockCode) {
        try {
            String approvalKey = kisAuthService.getWebSocketApprovalKey();
            KisSubscribeRequest request = KisSubscribeRequest.builder()
                    .header(KisSubscribeRequest.Header.builder()
                            .approval_key(approvalKey)
                            .custtype("P")
                            .tr_type("1")
                            .content_type("utf-8")
                            .build())
                    .body(KisSubscribeRequest.Body.builder()
                            .input(KisSubscribeRequest.Input.builder()
                                    .tr_id("H0STCNT0")
                                    .tr_key(stockCode)
                                    .build())
                            .build())
                    .build();

            String message = objectMapper.writeValueAsString(request);
            webSocketClient.send(message);
            log.info("Subscribed to quote data for stock: {}", stockCode);
            
        } catch (Exception e) {
            log.error("Failed to subscribe to quote data for stock: {}", stockCode, e);
        }
    }

    private void subscribeToOrderbook(String stockCode) {
        try {
            // 호가 구독을 위해 약간의 지연 추가
            Thread.sleep(500);
            
            String approvalKey = kisAuthService.getWebSocketApprovalKey();
            KisSubscribeRequest request = KisSubscribeRequest.builder()
                    .header(KisSubscribeRequest.Header.builder()
                            .approval_key(approvalKey)
                            .custtype("P")
                            .tr_type("1")
                            .content_type("utf-8")
                            .build())
                    .body(KisSubscribeRequest.Body.builder()
                            .input(KisSubscribeRequest.Input.builder()
                                    .tr_id("H0STASP0")
                                    .tr_key(stockCode)
                                    .build())
                            .build())
                    .build();

            String message = objectMapper.writeValueAsString(request);
            webSocketClient.send(message);
            log.info("Subscribed to orderbook data for stock: {}", stockCode);
            
        } catch (Exception e) {
            log.error("Failed to subscribe to orderbook data for stock: {}", stockCode, e);
        }
    }

    private void handleMessage(String message) {
        try {
            log.debug("Received message: {}", message);
            
            // PINGPONG 메시지 처리
            if (message.contains("PINGPONG")) {
                log.debug("Received PINGPONG message, sending response");
                webSocketClient.send(message); // PINGPONG 응답
                return;
            }
            
            // KIS API 실제 형식 파싱: 0|TR_ID|001|DATA
            if (message.startsWith("0|H0STCNT0|001|")) {
                // 시세 데이터 처리
                String data = message.substring("0|H0STCNT0|001|".length());
                String[] fields = data.split("\\^");
                
                if (fields.length >= 15) {
                    KisQuoteData quoteData = parseQuoteData(fields);
                    kafkaProducerService.sendQuoteMessage(quoteData);
                }
                
            } else if (message.startsWith("0|H0STASP0|001|")) {
                // 호가 데이터 처리
                String data = message.substring("0|H0STASP0|001|".length());
                String[] fields = data.split("\\^");
                
                if (fields.length >= 20) {
                    KisOrderbookData orderbookData = parseOrderbookData(fields);
                    kafkaProducerService.sendOrderbookMessage(orderbookData);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to handle received message: {}", message, e);
        }
    }

    private KisQuoteData parseQuoteData(String[] fields) {
        KisQuoteData data = new KisQuoteData();
        data.setTr_id("H0STCNT0");
        data.setTr_key(fields[0]);        // 종목코드
        data.setTimestamp(fields[1]);     // 체결시간
        data.setPrice(fields[2]);         // 현재가
        data.setVolume(fields[12]);       // 체결거래량
        data.setChange_amount(fields[4]); // 전일대비
        data.setChange_rate(fields[5]);   // 전일대비율
        data.setHigh_price(fields[8]);    // 고가
        data.setLow_price(fields[9]);     // 저가
        data.setOpen_price(fields[7]);    // 시가
        return data;
    }

    private KisOrderbookData parseOrderbookData(String[] fields) {
        KisOrderbookData data = new KisOrderbookData();
        data.setTr_id("H0STASP0");
        data.setTr_key(fields[0]);         // 종목코드
        data.setTimestamp(fields[1]);      // 호가시간
        
        // 호가 데이터는 더 복잡한 구조이므로 간단하게 처리
        if (fields.length >= 15) {
            data.setBid_price_1(fields[3]);
            data.setAsk_price_1(fields[4]);
            data.setBid_volume_1(fields[13]);
            data.setAsk_volume_1(fields[14]);
        }
        
        return data;
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            log.info("Attempting to reconnect to KIS WebSocket server...");
            connect();
        }, 5, TimeUnit.SECONDS);
    }

    public void disconnect() {
        isConnected = false;
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        scheduler.shutdown();
    }

    public boolean isConnected() {
        return isConnected && webSocketClient != null && webSocketClient.isOpen();
    }
}
