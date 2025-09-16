package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.TaggingResult;

import java.util.List;
import java.util.Map;

/**
 * LLM 태깅 서비스 포트
 */
public interface LlmTaggerPort {

    /**
     * 기존 태그와 카테고리를 참조하여 포스트의 제목과 내용을 분석하여 태그와 카테고리를 추출합니다.
     *
     * @param title               포스트 제목
     * @param content             포스트 내용
     * @param existingTagGroups   기존 태그 그룹 Map (tagGroup -> List<tagName>)
     * @param existingCategories  기존 카테고리 목록
     * @return 태깅 결과
     */
    TaggingResult tagContent(
            String title,
            String content,
            Map<String, List<String>> existingTagGroups,
            List<String> existingCategories);

}
