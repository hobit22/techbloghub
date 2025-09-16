package com.techbloghub.persistence.repository;

import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.model.TaggingProcessStatus;
import com.techbloghub.persistence.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PostRepositoryCustom {

    Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable);

    List<PostEntity> findByTaggingStatus(TaggingProcessStatus status, int limit);

    Map<TaggingProcessStatus, Long> getTaggingStatusStatistics();
}