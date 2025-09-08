package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    
    
    @Query("""
        SELECT c FROM CategoryEntity c 
        WHERE LOWER(c.name) IN ('spring', 'java', 'kotlin', 'react', 'vue', 'angular', 'javascript', 'typescript', 
                              'python', 'golang', 'rust', 'docker', 'kubernetes', 'aws', 'gcp', 'azure',
                              'database', 'mysql', 'postgresql', 'redis', 'elasticsearch', 'mongodb',
                              'microservices', 'devops', 'ci/cd', 'git', 'linux', 'frontend', 'backend',
                              'mobile', 'android', 'ios', 'flutter', 'machine learning', 'ai', 'data science')
        """)
    List<CategoryEntity> findTechCategories();
}