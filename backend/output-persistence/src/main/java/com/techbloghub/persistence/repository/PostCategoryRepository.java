package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategoryEntity, Long> {
}