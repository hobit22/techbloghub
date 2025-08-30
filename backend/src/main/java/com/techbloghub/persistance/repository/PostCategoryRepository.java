package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategoryEntity, Long> {
}