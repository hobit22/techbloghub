package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, PostRepositoryCustom {
    
    @EntityGraph("PostEntity.withAllRelations")
    Optional<PostEntity> findById(Long id);
    
    boolean existsByNormalizedUrl(String normalizedUrl);
}