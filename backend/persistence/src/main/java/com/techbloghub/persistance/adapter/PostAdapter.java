package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.persistance.entity.*;
import com.techbloghub.persistance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Transactional
public class PostAdapter implements PostRepositoryPort {

    private final PostRepository postRepository;
    private final BlogRepository blogRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        return postRepository.searchPosts(searchCondition, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postRepository.findByIdWithAllRelations(id)
                .map(PostEntity::toDomain);
    }

    @Override
    public Post save(Post post) {
        // 1. Blog 엔티티 조회
        BlogEntity blogEntity = null;
        if (post.getBlog() != null && post.getBlog().getId() != null) {
            blogEntity = blogRepository.findById(post.getBlog().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + post.getBlog().getId()));
        }

        // 2. Tag 엔티티들 조회/생성
        Set<TagEntity> tagEntities = post.getTags().stream()
                .map(tag -> {
                    if (tag.getId() != null) {
                        return tagRepository.findById(tag.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + tag.getId()));
                    } else {
                        // 이름으로 찾거나 새로 생성
                        return tagRepository.findByName(tag.getName())
                                .orElseGet(() -> tagRepository.save(TagEntity.fromDomain(tag)));
                    }
                })
                .collect(Collectors.toSet());

        // 3. Category 엔티티들 조회/생성
        Set<CategoryEntity> categoryEntities = post.getCategories().stream()
                .map(category -> {
                    if (category.getId() != null) {
                        return categoryRepository.findById(category.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + category.getId()));
                    } else {
                        // 이름으로 찾거나 새로 생성
                        return categoryRepository.findByName(category.getName())
                                .orElseGet(() -> categoryRepository.save(CategoryEntity.fromDomain(category)));
                    }
                })
                .collect(Collectors.toSet());

        // 4. PostEntity 생성 (관계 포함)
        PostEntity postEntity;
        if (blogEntity != null && !tagEntities.isEmpty() && !categoryEntities.isEmpty()) {
            postEntity = PostEntity.fromDomainWithRelations(post, blogEntity, tagEntities, categoryEntities);
        } else {
            postEntity = PostEntity.fromDomain(post);
            // 개별적으로 관계 설정
            if (blogEntity != null) {
                // Blog 관계는 PostEntity.fromDomain에서 처리되지 않으므로 별도 처리 필요
            }
        }

        // 5. PostEntity 저장
        PostEntity savedEntity = postRepository.save(postEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }

    @Override
    public Page<Post> findAllByOrderByPublishedAtDesc(Pageable pageable) {
        return postRepository.findAllByOrderByPublishedAtDesc(pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(PostEntity::toDomain);
    }
}