package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    
    Optional<CategoryEntity> findByName(String name);
    
    List<CategoryEntity> findByNameIn(Set<String> names);
    
    List<CategoryEntity> findByNameContaining(String keyword);
    
    boolean existsByName(String name);
}