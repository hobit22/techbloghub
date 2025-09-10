package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, PostRepositoryCustom {
    
    @EntityGraph("PostEntity.withAllRelations")
    @Query("SELECT p FROM PostEntity p ORDER BY p.publishedAt DESC")
    Page<PostEntity> findAllWithRelationsOrderByPublishedAtDesc(Pageable pageable);
    
    @EntityGraph("PostEntity.withAllRelations")
    Optional<PostEntity> findById(Long id);
}