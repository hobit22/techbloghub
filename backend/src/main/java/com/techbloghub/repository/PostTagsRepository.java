package com.techbloghub.repository;

import com.techbloghub.entity.PostTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostTagsRepository extends JpaRepository<PostTags, Long> {
}