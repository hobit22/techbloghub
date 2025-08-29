package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {
    
    Page<PostEntity> findAllOrderByPublishedAtDesc(Pageable pageable);
    
    Page<PostEntity> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable);
    
    List<PostEntity> findRecentPosts(LocalDateTime since);
    
    Page<PostEntity> findByTagNames(List<String> tagNames, Pageable pageable);
    
    Page<PostEntity> findByCategoryNames(List<String> categoryNames, Pageable pageable);
    
    long countByBlogId(Long blogId);
}