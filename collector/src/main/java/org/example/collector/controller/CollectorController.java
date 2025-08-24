package org.example.collector.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.collector.service.CollectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/collector")
public class CollectorController {

    private final CollectorService collectorService;

    public CollectorController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean isConnected = collectorService.isConnected();
        
        Map<String, Object> status = Map.of(
            "connected", isConnected,
            "service", "quote-stream-collector",
            "message", isConnected ? "WebSocket connection active" : "WebSocket connection inactive"
        );
        
        return ResponseEntity.ok(status);
    }
}

