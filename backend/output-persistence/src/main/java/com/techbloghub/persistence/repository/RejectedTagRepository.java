package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.entity.RejectedTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RejectedTagRepository extends JpaRepository<RejectedTagEntity, Long> {
    
    /**
     * 특정 태그명과 포스트에 대한 거부 태그 조회
     */
    Optional<RejectedTagEntity> findByTagNameAndPost(String tagName, PostEntity post);
    
    /**
     * 상태별 거부 태그 조회 (빈도순 정렬)
     */
    List<RejectedTagEntity> findByStatusOrderByFrequencyCountDesc(RejectedTagEntity.RejectedStatus status);
    
    /**
     * 최소 빈도 이상의 승인 후보 태그들 조회
     */
    @Query("""
        SELECT r FROM RejectedTagEntity r 
        WHERE r.status = :status 
        AND r.frequencyCount >= :minFrequency
        ORDER BY r.frequencyCount DESC
        """)
    List<RejectedTagEntity> findApprovalCandidates(
        @Param("status") RejectedTagEntity.RejectedStatus status,
        @Param("minFrequency") Integer minFrequency
    );
    
    /**
     * 최근 일정 기간 내 거부된 태그들 조회
     */
    @Query("""
        SELECT r FROM RejectedTagEntity r 
        WHERE r.status = :status 
        AND r.createdAt >= :since
        ORDER BY r.frequencyCount DESC
        """)
    List<RejectedTagEntity> findRecentRejected(
        @Param("status") RejectedTagEntity.RejectedStatus status,
        @Param("since") LocalDateTime since
    );
    
    /**
     * 태그명으로 그룹화된 통계 조회
     */
    @Query("""
        SELECT r.tagName, 
               COUNT(r) as occurrenceCount,
               SUM(r.frequencyCount) as totalFrequency,
               MIN(r.createdAt) as firstSeen,
               MAX(r.createdAt) as lastSeen,
               COUNT(DISTINCT r.post) as uniquePosts
        FROM RejectedTagEntity r 
        WHERE r.frequencyCount >= :minFrequency
        GROUP BY r.tagName, r.status
        ORDER BY SUM(r.frequencyCount) DESC
        """)
    List<Object[]> findTagStatistics(@Param("minFrequency") Integer minFrequency);
}