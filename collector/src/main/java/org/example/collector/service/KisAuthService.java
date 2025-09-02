package org.example.collector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import org.example.collector.config.KisWebSocketProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {
    
    private final KisWebSocketProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private String accessToken;
    private String websocketApprovalKey;
    private LocalDateTime tokenExpireTime;
    
    public String getAccessToken() {
        // 토큰이 없거나 만료되었으면 새로 발급
        if (accessToken == null || isTokenExpired()) {
            log.info("Fetching new access token...");
            String newToken = fetchAccessToken();
            if (newToken != null && !newToken.equals(accessToken)) {
                accessToken = newToken;
                // 토큰이 갱신되면 승인키도 새로 발급받아야 함
                websocketApprovalKey = null;
            }
        } else {
            log.debug("Using cached access token (expires at: {})", tokenExpireTime);
        }
        return accessToken;
    }
    
    private boolean isTokenExpired() {
        if (tokenExpireTime == null) {
            return true;
        }
        // 만료 5분 전에 갱신 (안전 마진)
        return LocalDateTime.now().isAfter(tokenExpireTime.minusMinutes(5));
    }
    
    public String getWebSocketApprovalKey() {
        // Access Token이 없으면 먼저 발급
        String token = getAccessToken();
        if (token == null) {
            log.error("Cannot get WebSocket approval key: Access token is null");
            return null;
        }
        
        if (websocketApprovalKey == null) {
            websocketApprovalKey = fetchWebSocketApprovalKey();
        }
        return websocketApprovalKey;
    }
    
    private String fetchAccessToken() {
        try {
            String tokenUrl = properties.getRest().tokenAPIUrl();
            log.info("Requesting access token from KIS API: {}", tokenUrl);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");
            requestBody.put("appkey", properties.getApp().getKey());
            log.info("Requesting access token with appkey: {}...", properties.getApp().getKey().substring(0, 10));
            requestBody.put("appsecret", properties.getApp().getSecret());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("Requesting access token with appkey: {}...", properties.getApp().getKey().substring(0, 10));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            log.info("Response status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String token = (String) responseBody.get("access_token");
                Object expiresIn = responseBody.get("expires_in");
                
                if (token != null) {
                    // 토큰 만료 시간 설정 (기본값 24시간)
                    int expireSeconds;
                    if (expiresIn instanceof Integer i) {
                        expireSeconds = i;
                    } else {
                        expireSeconds = 86400;
                    }
                    tokenExpireTime = LocalDateTime.now().plusSeconds(expireSeconds);
                    
                    log.info("Successfully obtained access token: {}...", token.substring(0, 20));
                    log.info("Token expires at: {}", tokenExpireTime);
                    return token;
                } else {
                    log.error("Access token is null in response: {}", responseBody);
                    return null;
                }
            }
            
            log.error("Failed to obtain access token. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            return null;
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            
            // Rate Limiting 오류 처리
            if (errorMessage != null && errorMessage.contains("EGW00133")) {
                log.warn("Rate limiting detected (EGW00133). Token fetch will be retried later.");
                // Rate Limiting 시 토큰 만료 시간을 5분 후로 설정하여 재시도 방지
                tokenExpireTime = LocalDateTime.now().plusMinutes(5);
                log.warn("Next token fetch will be attempted at: {}", tokenExpireTime);
            } else {
                log.error("Error fetching access token from {}: {}", properties.getRest().tokenAPIUrl(), errorMessage);
            }
            return null;
        }
    }
    
    private String fetchWebSocketApprovalKey() {
        try {
            String accessUrl = properties.getRest().accessAPIUrl();
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");
            requestBody.put("appkey", properties.getApp().getKey());
            requestBody.put("secretkey", properties.getApp().getSecret());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + getAccessToken());
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("Requesting WebSocket approval key from KIS API...");

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    accessUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String approvalKey = (String) responseBody.get("approval_key");
                log.info("Successfully obtained WebSocket approval key");
                return approvalKey;
            }
            
            log.error("Failed to obtain WebSocket approval key: {}", response.getBody());
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching WebSocket approval key", e);
            return null;
        }
    }
}
