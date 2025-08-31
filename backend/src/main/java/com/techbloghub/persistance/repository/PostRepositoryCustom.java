package com.techbloghub.persistance.repository;

import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable);
}