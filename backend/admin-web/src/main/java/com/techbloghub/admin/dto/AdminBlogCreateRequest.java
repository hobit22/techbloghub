package com.techbloghub.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@Schema(description = "어드민 블로그 생성 요청")
public class AdminBlogCreateRequest {

    @NotBlank(message = "블로그 이름은 필수입니다")
    @Schema(description = "블로그 이름", example = "우아한형제들 기술블로그", requiredMode = REQUIRED)
    private String name;

    @NotBlank(message = "회사명은 필수입니다")
    @Schema(description = "회사명", example = "우아한형제들", requiredMode = REQUIRED)
    private String company;

    @NotBlank(message = "RSS URL은 필수입니다")
    @Pattern(regexp = "^https?://.*", message = "유효한 RSS URL을 입력해주세요")
    @Schema(description = "RSS URL", example = "https://techblog.woowahan.com/feed", requiredMode = REQUIRED)
    private String rssUrl;

    @NotBlank(message = "사이트 URL은 필수입니다")
    @Pattern(regexp = "^https?://.*", message = "유효한 사이트 URL을 입력해주세요")
    @Schema(description = "사이트 URL", example = "https://techblog.woowahan.com", requiredMode = REQUIRED)
    private String siteUrl;

    @Pattern(regexp = "^https?://.*", message = "유효한 로고 URL을 입력해주세요")
    @Schema(description = "로고 URL", example = "https://techblog.woowahan.com/logo.png")
    private String logoUrl;

    @Schema(description = "블로그 설명", example = "우아한형제들의 기술 이야기")
    private String description;
}