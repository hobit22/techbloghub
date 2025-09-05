package com.techbloghub.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "어드민 일괄 삭제 요청")
public class AdminBatchDeleteRequest {

    @Schema(description = "삭제할 포스트 ID 목록", example = "[1, 2, 3, 4, 5]")
    private List<Long> ids;

    @Schema(description = "삭제 사유 (선택사항)", example = "스팸 포스트 일괄 삭제")
    private String reason;
}