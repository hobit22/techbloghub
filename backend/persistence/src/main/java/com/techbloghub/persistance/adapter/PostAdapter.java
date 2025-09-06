package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.persistance.entity.PostEntity;
import com.techbloghub.persistance.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PostAdapter implements PostRepositoryPort {

    private final PostRepository postRepository;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        return postRepository.searchPosts(searchCondition, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postRepository.findByIdWithBlog(id)
                .map(PostEntity::toDomain);
    }

    @Override
    public Post save(Post post) {
        PostEntity entity = PostEntity.fromDomain(post);
        PostEntity savedEntity = postRepository.save(entity);
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