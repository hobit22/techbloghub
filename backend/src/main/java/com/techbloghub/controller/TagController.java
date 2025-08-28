package com.techbloghub.controller;

import com.techbloghub.entity.Tag;
import com.techbloghub.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "tags", description = "태그 관리 API")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "태그 목록 조회", description = "모든 태그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "태그 목록 조회 성공")
    public ResponseEntity<List<com.techbloghub.entity.Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }
}