package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.entity.RejectedCategoryEntity;
import com.techbloghub.persistence.entity.RejectedTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RejectedCategoryRepository extends JpaRepository<RejectedCategoryEntity, Long> {
    
    /**
     * 특정 카테고리명과 포스트에 대한 거부 카테고리 조회
     */
    Optional<RejectedCategoryEntity> findByCategoryNameAndPost(String categoryName, PostEntity post);
    
    /**
     * 상태별 거부 카테고리 조회 (빈도순 정렬)
     */
    List<RejectedCategoryEntity> findByStatusOrderByFrequencyCountDesc(RejectedTagEntity.RejectedStatus status);
    
    /**
     * 최소 빈도 이상의 승인 후보 카테고리들 조회
     */
    @Query("""
        SELECT r FROM RejectedCategoryEntity r 
        WHERE r.status = :status 
        AND r.frequencyCount >= :minFrequency
        ORDER BY r.frequencyCount DESC
        """)
    List<RejectedCategoryEntity> findApprovalCandidates(
        @Param("status") RejectedTagEntity.RejectedStatus status,
        @Param("minFrequency") Integer minFrequency
    );
    
    /**
     * 최근 일정 기간 내 거부된 카테고리들 조회
     */
    @Query("""
        SELECT r FROM RejectedCategoryEntity r 
        WHERE r.status = :status 
        AND r.createdAt >= :since
        ORDER BY r.frequencyCount DESC
        """)
    List<RejectedCategoryEntity> findRecentRejected(
        @Param("status") RejectedTagEntity.RejectedStatus status,
        @Param("since") LocalDateTime since
    );
}