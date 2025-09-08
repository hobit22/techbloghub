package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTagEntity, Long> {
    
    /**
     * 특정 포스트의 모든 태그 관계 조회
     */
    List<PostTagEntity> findByPostId(Long postId);
    
    /**
     * 특정 태그의 모든 포스트 관계 조회
     */
    List<PostTagEntity> findByTagId(Long tagId);
    
    /**
     * 특정 포스트-태그 관계 존재 여부 확인
     */
    boolean existsByPostIdAndTagId(Long postId, Long tagId);
    
    /**
     * 특정 포스트의 모든 태그 관계 삭제
     */
    @Modifying
    @Query("DELETE FROM PostTagEntity pt WHERE pt.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
    
    /**
     * 특정 태그의 모든 포스트 관계 삭제
     */
    @Modifying
    @Query("DELETE FROM PostTagEntity pt WHERE pt.tagId = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);
    
    /**
     * 사용되지 않는 태그 ID 목록 조회
     */
    @Query("SELECT DISTINCT t.id FROM TagEntity t WHERE t.id NOT IN (SELECT pt.tagId FROM PostTagEntity pt)")
    List<Long> findUnusedTagIds();
}