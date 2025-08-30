package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagsRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByName(String name);

    boolean existsByName(String name);
}