package com.techbloghub.persistence.repository;

import com.techbloghub.domain.model.TaggingProcessStatus;
import com.techbloghub.persistence.entity.PostEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, PostRepositoryCustom {
    
    @EntityGraph("PostEntity.withAllRelations")
    Optional<PostEntity> findById(Long id);
    
    boolean existsByNormalizedUrl(String normalizedUrl);
    
    @Modifying
    @Query("UPDATE PostEntity p SET p.taggingProcessStatus = :status WHERE p.id = :postId")
    void updateTaggingProcessStatus(@Param("postId") Long postId, @Param("status") TaggingProcessStatus status);
}