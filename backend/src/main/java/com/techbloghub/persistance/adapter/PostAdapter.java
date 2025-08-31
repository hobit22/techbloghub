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

@RequiredArgsConstructor
@Repository
public class PostAdapter implements PostRepositoryPort {

    private final PostRepository postRepository;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        return postRepository.searchPosts(searchCondition, pageable)
                .map(PostEntity::toDomain);
    }

}