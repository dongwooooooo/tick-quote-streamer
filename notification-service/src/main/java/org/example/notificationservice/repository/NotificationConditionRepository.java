package org.example.notificationservice.repository;

import org.example.notificationservice.entity.NotificationCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationConditionRepository extends JpaRepository<NotificationCondition, Long> {
    
    /**
     * 활성화된 조건들 조회
     */
    List<NotificationCondition> findByIsActiveTrue();
    
    /**
     * 특정 종목의 활성화된 조건들 조회
     */
    List<NotificationCondition> findByStockCodeAndIsActiveTrue(String stockCode);
    
    /**
     * 특정 사용자의 조건들 조회
     */
    List<NotificationCondition> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 사용자의 활성화된 조건들 조회
     */
    List<NotificationCondition> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 사용자의 특정 종목 조건들 조회
     */
    List<NotificationCondition> findByUserIdAndStockCodeOrderByCreatedAtDesc(String userId, String stockCode);
    
    /**
     * 조건 타입별 활성화된 조건들 조회
     */
    List<NotificationCondition> findByConditionTypeAndIsActiveTrue(NotificationCondition.ConditionType conditionType);
    
    /**
     * 배치 처리용 - 페이지네이션으로 활성화된 조건들 조회
     */
    @Query("SELECT nc FROM NotificationCondition nc WHERE nc.isActive = true ORDER BY nc.stockCode, nc.id")
    List<NotificationCondition> findActiveConditionsBatch(Pageable pageable);
    
    /**
     * 특정 종목의 조건 수 조회
     */
    @Query("SELECT COUNT(nc) FROM NotificationCondition nc WHERE nc.stockCode = :stockCode AND nc.isActive = true")
    long countActiveConditionsByStock(@Param("stockCode") String stockCode);
    
    /**
     * 전체 활성화된 조건 수 조회
     */
    long countByIsActiveTrue();
}

