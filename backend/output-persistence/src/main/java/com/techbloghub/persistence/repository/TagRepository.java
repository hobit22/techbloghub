package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {
    
    Optional<TagEntity> findByName(String name);
    
    List<TagEntity> findByNameIn(Set<String> names);
    
    List<TagEntity> findByNameContaining(String keyword);
    
    boolean existsByName(String name);
    
}