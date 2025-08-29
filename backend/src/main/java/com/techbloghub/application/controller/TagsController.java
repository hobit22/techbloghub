package com.techbloghub.application.controller;

import com.techbloghub.application.dto.TagsResponse;
import com.techbloghub.domain.port.in.TagUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "tags", description = "태그 관리 API")
public class TagsController {

    private final TagUseCase tagUseCase;

    @GetMapping
    @Operation(summary = "태그 목록 조회", description = "모든 태그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "태그 목록 조회 성공")
    public ResponseEntity<List<TagsResponse>> getAllTags() {
        List<com.techbloghub.domain.model.Tag> tags = tagUseCase.getAllTags();
        
        List<TagsResponse> responses = tags.stream()
                .map(this::toTagsResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    private TagsResponse toTagsResponse(com.techbloghub.domain.model.Tag tag) {
        return TagsResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}