package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostTagsRepository extends JpaRepository<PostTagEntity, Long> {
}