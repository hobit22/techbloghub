package com.techbloghub.repository;

import com.techbloghub.entity.Tags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagsRepository extends JpaRepository<Tags, Long> {

    Optional<Tags> findByName(String name);

    boolean existsByName(String name);
}