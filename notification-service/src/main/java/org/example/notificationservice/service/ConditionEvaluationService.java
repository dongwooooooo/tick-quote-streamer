package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationMessage;
import org.example.notificationservice.entity.NotificationCondition;
import org.example.notificationservice.entity.NotificationHistory;
import org.example.notificationservice.repository.NotificationConditionRepository;
import org.example.notificationservice.repository.NotificationHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionEvaluationService {
    
    private final NotificationConditionRepository conditionRepository;
    private final NotificationHistoryRepository historyRepository;
    private final NotificationProducerService notificationProducerService;
    private final StockInfoService stockInfoService;
    
    // 조건 평가용 별도 스레드 풀
    private final Executor evaluationExecutor = Executors.newFixedThreadPool(4);
    
    /**
     * 특정 종목의 시세 데이터로 조건 평가
     */
    @Transactional
    public void evaluateQuoteConditions(String stockCode, BigDecimal currentPrice, Long currentVolume, BigDecimal changeRate) {
        try {
            log.debug("Evaluating quote conditions for stock: {} - Price: {}, Volume: {}, Change: {}%", 
                stockCode, currentPrice, currentVolume, changeRate);
            
            // 해당 종목의 활성화된 조건들 조회
            List<NotificationCondition> conditions = conditionRepository.findByStockCodeAndIsActiveTrue(stockCode);
            
            if (conditions.isEmpty()) {
                return;
            }
            
            log.debug("Found {} active conditions for stock: {}", conditions.size(), stockCode);
            
            // 비동기로 조건 평가 처리
            CompletableFuture.runAsync(() -> {
                evaluateConditionsAsync(conditions, currentPrice, currentVolume, changeRate);
            }, evaluationExecutor);
            
        } catch (Exception e) {
            log.error("Error evaluating quote conditions for stock: {}", stockCode, e);
        }
    }
    
    /**
     * 조건 평가 비동기 처리
     */
    private void evaluateConditionsAsync(List<NotificationCondition> conditions, BigDecimal currentPrice, Long currentVolume, BigDecimal changeRate) {
        for (NotificationCondition condition : conditions) {
            try {
                evaluateSingleCondition(condition, currentPrice, currentVolume, changeRate);
            } catch (Exception e) {
                log.error("Error evaluating condition: {} for stock: {}", condition.getId(), condition.getStockCode(), e);
            }
        }
    }
    
    /**
     * 개별 조건 평가
     */
    @Transactional
    public void evaluateSingleCondition(NotificationCondition condition, BigDecimal currentPrice, Long currentVolume, BigDecimal changeRate) {
        // 조건 충족 여부 확인
        boolean isConditionMet = condition.isConditionMet(currentPrice, currentVolume, changeRate);
        
        if (!isConditionMet) {
            return;
        }
        
        log.info("Condition triggered - ID: {}, User: {}, Stock: {}, Type: {}, Target: {}", 
            condition.getId(), condition.getUserId(), condition.getStockCode(), 
            condition.getConditionType(), condition.getTargetValue());
        
        // 조건 트리거 처리
        BigDecimal triggeredValue = getTriggeredValue(condition.getConditionType(), currentPrice, currentVolume, changeRate);
        condition.trigger(triggeredValue);
        conditionRepository.save(condition);
        
        // 종목명 조회
        String stockName = stockInfoService.getStockName(condition.getStockCode())
            .orElse(condition.getStockCode());
        
        // 알림 메시지 생성
        NotificationMessage notificationMessage = NotificationMessage.createPriceAlert(
            condition.getUserId(),
            condition.getStockCode(),
            stockName,
            condition.getConditionType(),
            condition.getTargetValue(),
            triggeredValue
        );
        
        // 알림 이력 저장
        NotificationHistory history = NotificationHistory.builder()
            .userId(condition.getUserId())
            .stockCode(condition.getStockCode())
            .conditionId(condition.getId())
            .conditionType(condition.getConditionType())
            .targetValue(condition.getTargetValue())
            .triggeredValue(triggeredValue)
            .message(notificationMessage.getMessage())
            .status(NotificationHistory.NotificationStatus.PENDING)
            .build();
        
        NotificationHistory savedHistory = historyRepository.save(history);
        notificationMessage.setNotificationId(savedHistory.getId());
        
        // Kafka로 알림 전송
        notificationProducerService.sendNotification(notificationMessage);
        
        log.info("Notification sent for condition: {} - User: {}, Stock: {}", 
            condition.getId(), condition.getUserId(), condition.getStockCode());
    }
    
    /**
     * 배치 조건 평가 (전체 활성 조건 대상)
     */
    @Transactional(readOnly = true)
    public void evaluateAllConditionsBatch(int batchSize) {
        try {
            log.debug("Starting batch condition evaluation with batch size: {}", batchSize);
            
            int page = 0;
            List<NotificationCondition> conditions;
            
            do {
                conditions = conditionRepository.findActiveConditionsBatch(PageRequest.of(page, batchSize));
                
                if (!conditions.isEmpty()) {
                    log.debug("Processing batch {} with {} conditions", page, conditions.size());
                    
                    // 종목별로 그룹화하여 처리
                    conditions.stream()
                        .collect(java.util.stream.Collectors.groupingBy(NotificationCondition::getStockCode))
                        .forEach(this::evaluateStockConditions);
                }
                
                page++;
            } while (!conditions.isEmpty());
            
            log.debug("Batch condition evaluation completed");
            
        } catch (Exception e) {
            log.error("Error in batch condition evaluation", e);
        }
    }
    
    /**
     * 특정 종목의 조건들 평가
     */
    private void evaluateStockConditions(String stockCode, List<NotificationCondition> conditions) {
        try {
            // 최신 시세 정보 조회 (캐시 또는 DB에서)
            var stockData = stockInfoService.getLatestStockData(stockCode);
            
            if (stockData.isPresent()) {
                var data = stockData.get();
                evaluateConditionsAsync(conditions, data.getPrice(), data.getVolume(), data.getChangeRate());
            }
            
        } catch (Exception e) {
            log.error("Error evaluating conditions for stock: {}", stockCode, e);
        }
    }
    
    /**
     * 조건 타입에 따른 트리거된 값 추출
     */
    private BigDecimal getTriggeredValue(NotificationCondition.ConditionType conditionType, 
                                       BigDecimal currentPrice, Long currentVolume, BigDecimal changeRate) {
        switch (conditionType) {
            case PRICE_ABOVE:
            case PRICE_BELOW:
                return currentPrice;
            case VOLUME_ABOVE:
                return currentVolume != null ? BigDecimal.valueOf(currentVolume) : BigDecimal.ZERO;
            case CHANGE_RATE_ABOVE:
            case CHANGE_RATE_BELOW:
                return changeRate != null ? changeRate : BigDecimal.ZERO;
            default:
                return currentPrice;
        }
    }
    
    /**
     * 조건 평가 통계 조회
     */
    public ConditionEvaluationStats getEvaluationStats() {
        long totalActiveConditions = conditionRepository.countByIsActiveTrue();
        long pendingNotifications = historyRepository.countByStatus(NotificationHistory.NotificationStatus.PENDING);
        long sentNotifications = historyRepository.countByStatus(NotificationHistory.NotificationStatus.SENT);
        long failedNotifications = historyRepository.countByStatus(NotificationHistory.NotificationStatus.FAILED);
        
        return ConditionEvaluationStats.builder()
            .totalActiveConditions(totalActiveConditions)
            .pendingNotifications(pendingNotifications)
            .sentNotifications(sentNotifications)
            .failedNotifications(failedNotifications)
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ConditionEvaluationStats {
        private long totalActiveConditions;
        private long pendingNotifications;
        private long sentNotifications;
        private long failedNotifications;
    }
}

