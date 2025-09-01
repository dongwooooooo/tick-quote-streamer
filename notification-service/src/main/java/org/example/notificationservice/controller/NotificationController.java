package org.example.notificationservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.CreateConditionRequest;
import org.example.notificationservice.entity.NotificationCondition;
import org.example.notificationservice.entity.NotificationHistory;
import org.example.notificationservice.service.ConditionEvaluationService;
import org.example.notificationservice.service.NotificationConditionService;
import org.example.notificationservice.service.NotificationDeliveryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationConditionService conditionService;
    private final ConditionEvaluationService evaluationService;
    private final NotificationDeliveryService deliveryService;
    
    /**
     * 알림 조건 생성
     */
    @PostMapping("/conditions")
    public ResponseEntity<NotificationCondition> createCondition(@RequestBody CreateConditionRequest request) {
        try {
            NotificationCondition condition = conditionService.createCondition(request);
            log.info("Notification condition created - ID: {}, User: {}, Stock: {}", 
                condition.getId(), condition.getUserId(), condition.getStockCode());
            return ResponseEntity.ok(condition);
        } catch (Exception e) {
            log.error("Error creating notification condition", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 사용자별 알림 조건 조회
     */
    @GetMapping("/conditions/user/{userId}")
    public ResponseEntity<List<NotificationCondition>> getUserConditions(@PathVariable String userId) {
        List<NotificationCondition> conditions = conditionService.getUserConditions(userId);
        return ResponseEntity.ok(conditions);
    }
    
    /**
     * 활성화된 알림 조건 조회
     */
    @GetMapping("/conditions/user/{userId}/active")
    public ResponseEntity<List<NotificationCondition>> getUserActiveConditions(@PathVariable String userId) {
        List<NotificationCondition> conditions = conditionService.getUserActiveConditions(userId);
        return ResponseEntity.ok(conditions);
    }
    
    /**
     * 알림 조건 수정
     */
    @PutMapping("/conditions/{conditionId}")
    public ResponseEntity<NotificationCondition> updateCondition(
        @PathVariable Long conditionId,
        @RequestBody CreateConditionRequest request
    ) {
        try {
            NotificationCondition condition = conditionService.updateCondition(conditionId, request);
            return ResponseEntity.ok(condition);
        } catch (Exception e) {
            log.error("Error updating notification condition - ID: {}", conditionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 알림 조건 삭제
     */
    @DeleteMapping("/conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long conditionId) {
        try {
            conditionService.deleteCondition(conditionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting notification condition - ID: {}", conditionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 알림 조건 활성화/비활성화
     */
    @PatchMapping("/conditions/{conditionId}/toggle")
    public ResponseEntity<NotificationCondition> toggleCondition(@PathVariable Long conditionId) {
        try {
            NotificationCondition condition = conditionService.toggleCondition(conditionId);
            return ResponseEntity.ok(condition);
        } catch (Exception e) {
            log.error("Error toggling notification condition - ID: {}", conditionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 알림 이력 조회
     */
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<List<NotificationHistory>> getUserNotificationHistory(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<NotificationHistory> history = conditionService.getUserNotificationHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }
    
    /**
     * 종목별 알림 이력 조회
     */
    @GetMapping("/history/stock/{stockCode}")
    public ResponseEntity<List<NotificationHistory>> getStockNotificationHistory(@PathVariable String stockCode) {
        List<NotificationHistory> history = conditionService.getStockNotificationHistory(stockCode);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 알림 서비스 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 조건 평가 통계
        ConditionEvaluationService.ConditionEvaluationStats evalStats = evaluationService.getEvaluationStats();
        stats.put("evaluation", evalStats);
        
        // 전송 통계
        NotificationDeliveryService.DeliveryStats deliveryStats = deliveryService.getDeliveryStats();
        stats.put("delivery", deliveryStats);
        
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "notification-service");
        health.put("timestamp", LocalDateTime.now());
        
        // 간단한 통계 포함
        ConditionEvaluationService.ConditionEvaluationStats stats = evaluationService.getEvaluationStats();
        health.put("activeConditions", stats.getTotalActiveConditions());
        health.put("pendingNotifications", stats.getPendingNotifications());
        
        return ResponseEntity.ok(health);
    }
}
