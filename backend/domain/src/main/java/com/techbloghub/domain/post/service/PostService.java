package com.techbloghub.domain.post.service;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.post.port.PostRepositoryPort;
import com.techbloghub.domain.post.usecase.PostUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService implements PostUseCase {

    private final PostRepositoryPort postRepositoryPort;

    @Override
    public Post getPostDetail(Long postId) {
        return postRepositoryPort.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 postId 입니다."));
    }
}
