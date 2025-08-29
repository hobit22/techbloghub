package com.techbloghub.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.techbloghub.dto.SearchRequest;
import com.techbloghub.entity.Post;
import com.techbloghub.entity.QPost;
import com.techbloghub.entity.QPostTags;
import com.techbloghub.entity.QPostCategory;
import com.techbloghub.entity.QTags;
import com.techbloghub.entity.QCategory;
import com.techbloghub.entity.QBlog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final JPAQueryFactory queryFactory;
    
    private static final QPost post = QPost.post;
    private static final QPostTags postTags = QPostTags.postTags;
    private static final QPostCategory postCategory = QPostCategory.postCategory;
    private static final QTags tags = QTags.tags;
    private static final QCategory category = QCategory.category;
    private static final QBlog blog = QBlog.blog;

    public Page<Post> search(SearchRequest request) {
        try {
            Pageable pageable = createPageable(request);
            BooleanBuilder builder = new BooleanBuilder();

            // 키워드 검색 (제목 또는 내용에 포함)
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                String keyword = request.getQuery().trim();
                builder.and(post.title.containsIgnoreCase(keyword)
                        .or(post.content.containsIgnoreCase(keyword)));
            }

            // 회사 필터
            if (request.getCompanies() != null && !request.getCompanies().isEmpty()) {
                builder.and(blog.company.in(request.getCompanies()));
            }

            // 태그 필터
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                builder.and(tags.name.in(request.getTags()));
            }

            // 카테고리 필터
            if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                builder.and(category.name.in(request.getCategories()));
            }

            // 메인 쿼리
            List<Post> content = queryFactory
                    .selectFrom(post)
                    .distinct()
                    .leftJoin(post.blog, blog).fetchJoin()
                    .leftJoin(post.postTags, postTags)
                    .leftJoin(postTags.tags, tags)
                    .leftJoin(post.postCategories, postCategory)
                    .leftJoin(postCategory.category, category)
                    .where(builder)
                    .orderBy("desc".equalsIgnoreCase(request.getSortDirection()) ? 
                            post.publishedAt.desc() : post.publishedAt.asc())
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            // 카운트 쿼리
            Long total = queryFactory
                    .select(post.countDistinct())
                    .from(post)
                    .leftJoin(post.blog, blog)
                    .leftJoin(post.postTags, postTags)
                    .leftJoin(postTags.tags, tags)
                    .leftJoin(post.postCategories, postCategory)
                    .leftJoin(postCategory.category, category)
                    .where(builder)
                    .fetchOne();

            return new PageImpl<>(content, pageable, total != null ? total : 0);

        } catch (Exception e) {
            log.error("Error performing search: {}", e.getMessage());
            return Page.empty();
        }
    }

    private Pageable createPageable(SearchRequest request) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy()
        );

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}