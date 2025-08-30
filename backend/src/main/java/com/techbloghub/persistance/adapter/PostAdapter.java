package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.*;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.persistance.entity.*;
import com.techbloghub.persistance.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PostAdapter implements PostRepositoryPort {
    
    private final PostRepository postRepository;

    @Override
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id).map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable) {
        return postRepository.findAllOrderByPublishedAtDesc(pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable) {
        return postRepository.findByBlogIdOrderByPublishedAtDesc(blogId, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public List<Post> findRecentPosts(LocalDateTime since) {
        return postRepository.findRecentPosts(since)
                .stream()
                .map(PostEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Post> findByTagNames(List<String> tagNames, Pageable pageable) {
        return postRepository.findByTagNames(tagNames, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> findByCategoryNames(List<String> categoryNames, Pageable pageable) {
        return postRepository.findByCategoryNames(categoryNames, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> searchByKeyword(String keyword, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public Page<Post> searchWithFilters(String keyword, List<String> companies,
                                       List<String> tags, List<String> categories,
                                       Pageable pageable) {
        return Page.empty();
    }

    @Override
    public long countByBlogId(Long blogId) {
        return postRepository.countByBlogId(blogId);
    }

    

}