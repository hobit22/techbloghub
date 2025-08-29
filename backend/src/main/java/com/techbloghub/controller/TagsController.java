package com.techbloghub.controller;

import com.techbloghub.dto.TagsResponse;
import com.techbloghub.entity.Tags;
import com.techbloghub.service.TagsService;
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

    private final TagsService tagsService;

    @GetMapping
    @Operation(summary = "태그 목록 조회", description = "모든 태그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "태그 목록 조회 성공")
    public ResponseEntity<List<TagsResponse>> getAllTags() {
        List<Tags> tags = tagsService.getAllTags();
        List<TagsResponse> response = tags.stream()
                .map(TagsResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}