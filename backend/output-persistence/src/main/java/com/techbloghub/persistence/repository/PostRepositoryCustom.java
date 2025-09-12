package com.techbloghub.persistence.repository;

import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.persistence.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable);
}