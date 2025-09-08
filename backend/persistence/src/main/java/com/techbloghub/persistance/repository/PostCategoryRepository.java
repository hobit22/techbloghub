package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategoryEntity, Long> {
    
    /**
     * 특정 포스트의 모든 카테고리 관계 조회
     */
    List<PostCategoryEntity> findByPostId(Long postId);
    
    /**
     * 특정 카테고리의 모든 포스트 관계 조회
     */
    List<PostCategoryEntity> findByCategoryId(Long categoryId);
    
    /**
     * 특정 포스트-카테고리 관계 존재 여부 확인
     */
    boolean existsByPostIdAndCategoryId(Long postId, Long categoryId);
    
    /**
     * 특정 포스트의 모든 카테고리 관계 삭제
     */
    @Modifying
    @Query("DELETE FROM PostCategoryEntity pc WHERE pc.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
    
    /**
     * 특정 카테고리의 모든 포스트 관계 삭제
     */
    @Modifying
    @Query("DELETE FROM PostCategoryEntity pc WHERE pc.categoryId = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 사용되지 않는 카테고리 ID 목록 조회
     */
    @Query("SELECT DISTINCT c.id FROM CategoryEntity c WHERE c.id NOT IN (SELECT pc.categoryId FROM PostCategoryEntity pc)")
    List<Long> findUnusedCategoryIds();
}