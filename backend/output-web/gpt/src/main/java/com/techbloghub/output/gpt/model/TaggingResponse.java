package com.techbloghub.output.gpt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Spring AI Structured Output을 위한 태깅 응답 모델
 */
public record TaggingResponse(
    @JsonProperty("categories") List<String> categories,
    @JsonProperty("tags") List<String> tags
) {
}