package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.TaggingResult;

public interface AutoTaggingUseCase {

    /**
     * 단일 포스트를 자동으로 태깅
     *
     * @param postId 태깅할 포스트 id
     * @return 태깅 결과
     */
    TaggingResult autoTag(Long postId);
}
