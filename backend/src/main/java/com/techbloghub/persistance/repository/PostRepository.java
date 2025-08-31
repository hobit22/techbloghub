package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, PostRepositoryCustom {
}