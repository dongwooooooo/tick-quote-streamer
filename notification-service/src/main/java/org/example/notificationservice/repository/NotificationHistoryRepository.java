package org.example.notificationservice.repository;

import org.example.notificationservice.entity.NotificationHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    
    /**
     * 특정 사용자의 알림 이력 조회 (최신순)
     */
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 사용자의 알림 이력 조회 (페이지네이션)
     */
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 특정 종목의 알림 이력 조회
     */
    List<NotificationHistory> findByStockCodeOrderByCreatedAtDesc(String stockCode);
    
    /**
     * 실패한 알림들 조회 (재시도 대상)
     */
    List<NotificationHistory> findByStatusAndRetryCountLessThan(
        NotificationHistory.NotificationStatus status, 
        Integer maxRetryCount
    );
    
    /**
     * 특정 기간의 알림 이력 조회
     */
    @Query("SELECT nh FROM NotificationHistory nh WHERE nh.createdAt BETWEEN :startTime AND :endTime ORDER BY nh.createdAt DESC")
    List<NotificationHistory> findByCreatedAtBetween(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 특정 조건의 알림 이력 조회
     */
    List<NotificationHistory> findByConditionIdOrderByCreatedAtDesc(Long conditionId);
    
    /**
     * 상태별 알림 수 조회
     */
    long countByStatus(NotificationHistory.NotificationStatus status);
    
    /**
     * 특정 기간의 상태별 알림 수 조회
     */
    @Query("SELECT COUNT(nh) FROM NotificationHistory nh WHERE nh.status = :status AND nh.createdAt >= :afterTime")
    long countByStatusAndCreatedAtAfter(
        @Param("status") NotificationHistory.NotificationStatus status, 
        @Param("afterTime") LocalDateTime afterTime
    );
    
    /**
     * 오래된 이력 정리용 - 특정 기간 이전 데이터 조회
     */
    @Query("SELECT nh FROM NotificationHistory nh WHERE nh.createdAt < :beforeTime")
    List<NotificationHistory> findOldHistories(@Param("beforeTime") LocalDateTime beforeTime, Pageable pageable);
}

