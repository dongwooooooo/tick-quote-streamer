package org.example.collector.service;

import lombok.extern.slf4j.Slf4j;
import org.example.collector.client.KisWebSocketClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class CollectorService {

    private final KisWebSocketClient kisWebSocketClient;

    public CollectorService(KisWebSocketClient kisWebSocketClient) {
        this.kisWebSocketClient = kisWebSocketClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startCollection() {
        log.info("Starting KIS WebSocket data collection...");
        kisWebSocketClient.connect();
    }

    @PreDestroy
    public void stopCollection() {
        log.info("Stopping KIS WebSocket data collection...");
        kisWebSocketClient.disconnect();
    }

    public boolean isConnected() {
        return kisWebSocketClient.isConnected();
    }
}