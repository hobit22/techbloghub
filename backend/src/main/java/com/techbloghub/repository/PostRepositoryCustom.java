package com.techbloghub.repository;

import com.techbloghub.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepositoryCustom {
    
    Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable);
    
    List<Post> findRecentPosts(LocalDateTime since);
    
    Page<Post> findByTagNames(List<String> tagNames, Pageable pageable);
    
    Page<Post> findByCategoryNames(List<String> categoryNames, Pageable pageable);
}