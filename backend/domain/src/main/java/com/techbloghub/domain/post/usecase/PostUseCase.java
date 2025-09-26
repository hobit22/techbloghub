package com.techbloghub.domain.post.usecase;

import com.techbloghub.domain.post.model.Post;

public interface PostUseCase {
    Post getPostDetail(Long postId);
}
