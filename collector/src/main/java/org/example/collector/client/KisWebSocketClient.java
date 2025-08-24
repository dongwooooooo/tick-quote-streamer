package org.example.collector.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.collector.config.KisWebSocketProperties;
import org.example.collector.dto.KisOrderbookData;
import org.example.collector.dto.KisQuoteData;
import org.example.collector.dto.KisSubscribeRequest;
import org.example.collector.service.KafkaProducerService;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;

@Slf4j
@Component
public class KisWebSocketClient {

    private final KisWebSocketProperties properties;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    
    private WebSocketClient webSocketClient;
    private boolean isConnected = false;

    public KisWebSocketClient(KisWebSocketProperties properties, 
                             KafkaProducerService kafkaProducerService,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void connect() {
        try {
            URI serverUri = new URI(properties.getWebsocket().getUrl());
            
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("WebSocket connection opened to KIS server: {}", serverUri);
                    isConnected = true;
                    startSubscriptions();
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

            // SSL 설정 추가 (KIS API를 위한 인증서 검증 우회)
            if (serverUri.getScheme().equals("wss")) {
                setupSSLContext();
            }
            
            webSocketClient.connect();
            
        } catch (Exception e) {
            log.error("Failed to connect to KIS WebSocket server", e);
            scheduleReconnect();
        }
    }
    
    private void setupSSLContext() {
        try {
            // 모든 인증서를 허용하는 TrustManager 생성 (개발/테스트용)
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            // 모든 호스트명을 허용하는 HostnameVerifier 생성
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 글로벌 SSL 설정 (WebSocket 라이브러리용)
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
            webSocketClient.setSocketFactory(sslContext.getSocketFactory());
            
            log.debug("SSL context and hostname verifier configured for KIS API connection");
        } catch (Exception e) {
            log.warn("Failed to setup SSL context", e);
        }
    }

    private void startSubscriptions() {
        // 구독 시작을 약간 지연
        scheduler.schedule(() -> {
            for (String stockCode : properties.getTargetStocks()) {
                subscribeToQuote(stockCode);
                subscribeToOrderbook(stockCode);
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void subscribeToQuote(String stockCode) {
        try {
            KisSubscribeRequest request = KisSubscribeRequest.builder()
                    .header(KisSubscribeRequest.Header.builder()
                            .approval_key(properties.getApp().getKey())
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
            
            KisSubscribeRequest request = KisSubscribeRequest.builder()
                    .header(KisSubscribeRequest.Header.builder()
                            .approval_key(properties.getApp().getKey())
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
