package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.CreateConditionRequest;
import org.example.notificationservice.entity.NotificationCondition;
import org.example.notificationservice.entity.NotificationHistory;
import org.example.notificationservice.repository.NotificationConditionRepository;
import org.example.notificationservice.repository.NotificationHistoryRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConditionService {
    
    private final NotificationConditionRepository conditionRepository;
    private final NotificationHistoryRepository historyRepository;
    
    /**
     * 알림 조건 생성
     */
    @Transactional
    public NotificationCondition createCondition(CreateConditionRequest request) {
        NotificationCondition condition = request.toEntity();
        
        // 중복 조건 체크 (같은 사용자, 종목, 조건 타입, 목표값)
        List<NotificationCondition> existingConditions = conditionRepository
            .findByUserIdAndStockCodeOrderByCreatedAtDesc(request.getUserId(), request.getStockCode());
        
        boolean isDuplicate = existingConditions.stream()
            .anyMatch(existing -> 
                existing.getConditionType() == request.getConditionType() &&
                existing.getTargetValue().compareTo(request.getTargetValue()) == 0 &&
                existing.getIsActive()
            );
        
        if (isDuplicate) {
            throw new IllegalArgumentException("동일한 조건이 이미 존재합니다.");
        }
        
        NotificationCondition savedCondition = conditionRepository.save(condition);
        
        log.info("Notification condition created - ID: {}, User: {}, Stock: {}, Type: {}, Target: {}", 
            savedCondition.getId(), savedCondition.getUserId(), savedCondition.getStockCode(), 
            savedCondition.getConditionType(), savedCondition.getTargetValue());
        
        return savedCondition;
    }
    
    /**
     * 사용자별 알림 조건 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationCondition> getUserConditions(String userId) {
        return conditionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 사용자별 활성화된 알림 조건 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationCondition> getUserActiveConditions(String userId) {
        return conditionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 알림 조건 수정
     */
    @Transactional
    public NotificationCondition updateCondition(Long conditionId, CreateConditionRequest request) {
        NotificationCondition condition = conditionRepository.findById(conditionId)
            .orElseThrow(() -> new IllegalArgumentException("조건을 찾을 수 없습니다: " + conditionId));
        
        // 업데이트
        condition.setConditionType(request.getConditionType());
        condition.setTargetValue(request.getTargetValue());
        condition.setDescription(request.getDescription());
        
        // 조건이 변경되었으므로 재활성화
        if (!condition.getIsActive()) {
            condition.reactivate();
        }
        
        NotificationCondition savedCondition = conditionRepository.save(condition);
        
        log.info("Notification condition updated - ID: {}, Type: {}, Target: {}", 
            savedCondition.getId(), savedCondition.getConditionType(), savedCondition.getTargetValue());
        
        return savedCondition;
    }
    
    /**
     * 알림 조건 삭제
     */
    @Transactional
    public void deleteCondition(Long conditionId) {
        NotificationCondition condition = conditionRepository.findById(conditionId)
            .orElseThrow(() -> new IllegalArgumentException("조건을 찾을 수 없습니다: " + conditionId));
        
        conditionRepository.delete(condition);
        
        log.info("Notification condition deleted - ID: {}, User: {}, Stock: {}", 
            conditionId, condition.getUserId(), condition.getStockCode());
    }
    
    /**
     * 알림 조건 활성화/비활성화 토글
     */
    @Transactional
    public NotificationCondition toggleCondition(Long conditionId) {
        NotificationCondition condition = conditionRepository.findById(conditionId)
            .orElseThrow(() -> new IllegalArgumentException("조건을 찾을 수 없습니다: " + conditionId));
        
        if (condition.getIsActive()) {
            condition.setIsActive(false);
            log.info("Notification condition deactivated - ID: {}", conditionId);
        } else {
            condition.reactivate();
            log.info("Notification condition reactivated - ID: {}", conditionId);
        }
        
        return conditionRepository.save(condition);
    }
    
    /**
     * 사용자별 알림 이력 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationHistory> getUserNotificationHistory(String userId, Pageable pageable) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 종목별 알림 이력 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationHistory> getStockNotificationHistory(String stockCode) {
        return historyRepository.findByStockCodeOrderByCreatedAtDesc(stockCode);
    }
    
    /**
     * 조건별 알림 이력 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationHistory> getConditionHistory(Long conditionId) {
        return historyRepository.findByConditionIdOrderByCreatedAtDesc(conditionId);
    }
    
    /**
     * 사용자의 조건 수 조회
     */
    @Transactional(readOnly = true)
    public long getUserConditionCount(String userId) {
        return conditionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).size();
    }
    
    /**
     * 종목별 조건 수 조회
     */
    @Transactional(readOnly = true)
    public long getStockConditionCount(String stockCode) {
        return conditionRepository.countActiveConditionsByStock(stockCode);
    }
}

