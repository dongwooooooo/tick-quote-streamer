package org.example.mockwebsocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mock/oauth")
public class MockOAuthController {

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> getAccessToken(@RequestBody Map<String, String> request) {
        log.info("Mock OAuth Token request: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "mock_access_token_" + System.currentTimeMillis());
        response.put("token_type", "Bearer");
        response.put("expires_in", 86400);
        
        log.info("Mock OAuth Token response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/access")
    public ResponseEntity<Map<String, Object>> getWebSocketApprovalKey(@RequestBody Map<String, String> request) {
        log.info("Mock WebSocket Approval request: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("approval_key", "mock_approval_key_" + System.currentTimeMillis());
        
        log.info("Mock WebSocket Approval response: {}", response);
        return ResponseEntity.ok(response);
    }
}
