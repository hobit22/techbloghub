package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTagEntity, Long> {
    
    /**
     * 사용되지 않는 태그 ID 목록 조회
     */
    @Query("SELECT DISTINCT t.id FROM TagEntity t WHERE t.id NOT IN (SELECT pt.tagId FROM PostTagEntity pt)")
    List<Long> findUnusedTagIds();
}