package com.techbloghub.api.controller;

import com.techbloghub.api.dto.PostResponse;
import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.post.usecase.PostUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostUseCase postUseCase;

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostDetail(@PathVariable Long postId) {
        Post post = postUseCase.getPostDetail(postId);
        return ResponseEntity.ok(PostResponse.from(post));
    }

}
