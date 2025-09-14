package com.techbloghub.admin.controller;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.TaggingResult;
import com.techbloghub.domain.port.in.AutoTaggingUseCase;
import com.techbloghub.domain.port.out.LlmTaggerPort;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * LLM 태깅 테스트용 관리자 컨트롤러
 */
@RestController
@RequestMapping("/admin/llm-tagging")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LLM 태깅", description = "LLM 태깅 관리 API")
public class LlmTaggingController {
    
    private final AutoTaggingUseCase autoTaggingUseCase;
    private final PostRepositoryPort postRepositoryPort;
    
    @PostMapping("/tag-post/{postId}")
    @Operation(summary = "포스트 자동 태깅", description = "특정 포스트를 LLM으로 자동 태깅합니다.")
    public TaggingResult tagPost(@PathVariable Long postId) {
        log.info("Auto tagging post: {}", postId);
            
        return autoTaggingUseCase.autoTag(postId);
    }
    
    public record TaggingTestRequest(
        String title,
        String content,
        String summary
    ) {}
}