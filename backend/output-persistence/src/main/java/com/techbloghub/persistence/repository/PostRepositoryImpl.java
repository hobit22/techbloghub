package com.techbloghub.persistence.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.techbloghub.domain.post.model.SearchCondition;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import com.techbloghub.persistence.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QPostEntity post = QPostEntity.postEntity;
    private static final QPostCategoryEntity postCategory = QPostCategoryEntity.postCategoryEntity;
    private static final QPostTagEntity postTag = QPostTagEntity.postTagEntity;
    private static final QCategoryEntity category = QCategoryEntity.categoryEntity;
    private static final QTagEntity tag = QTagEntity.tagEntity;

    @Override
    public Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(searchCondition, pageable);

        boolean needTagJoin = searchCondition.getTags() != null && !searchCondition.getTags().isEmpty();
        boolean needCategoryJoin = searchCondition.getCategories() != null && !searchCondition.getCategories().isEmpty();

        var query = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.blog).fetchJoin();

        // Only join tags if needed for filtering
        if (needTagJoin) {
            query.leftJoin(post.postTags, postTag).fetchJoin()
                 .leftJoin(postTag.tag, tag).fetchJoin();
        }

        // Only join categories if needed for filtering
        if (needCategoryJoin) {
            query.leftJoin(post.postCategories, postCategory).fetchJoin()
                 .leftJoin(postCategory.category, category).fetchJoin();
        }

        List<PostEntity> content = query
                .where(
                        keywordCondition(searchCondition.getKeyword()),
                        blogIdCondition(searchCondition.getBlogIds()),
                        categoriesContainsAll(searchCondition.getCategories()),
                        tagsContainsAll(searchCondition.getTags()),
                        dateAfterCondition(searchCondition.getPublishedAfter()),
                        dateBeforeCondition(searchCondition.getPublishedBefore())
                )
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        keywordCondition(searchCondition.getKeyword()),
                        blogIdCondition(searchCondition.getBlogIds()),
                        categoriesContainsAll(searchCondition.getCategories()),
                        tagsContainsAll(searchCondition.getTags()),
                        dateAfterCondition(searchCondition.getPublishedAfter()),
                        dateBeforeCondition(searchCondition.getPublishedBefore())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<PostEntity> findByTaggingStatus(TaggingProcessStatus status, int limit) {
        return queryFactory.selectFrom(post)
                .where(post.taggingProcessStatus.eq(status))
                .orderBy(post.publishedAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Map<TaggingProcessStatus, Long> getTaggingStatusStatistics() {
        List<Tuple> results = queryFactory
                .select(post.taggingProcessStatus, post.count())
                .from(post)
                .groupBy(post.taggingProcessStatus)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(post.taggingProcessStatus),
                        tuple -> tuple.get(post.count())
                ));
    }

    @Override
    public List<PostEntity> findPostsWithTotalContent(int limit) {
        return queryFactory.selectFrom(post)
                .where(
                        post.totalContent.isNotNull()
                )
                .limit(limit)
                .fetch();
    }


    private BooleanExpression keywordCondition(String keyword) {
        return hasText(keyword) ?
                post.title.containsIgnoreCase(keyword)
                        .or(post.content.containsIgnoreCase(keyword)) : null;
    }

    private BooleanExpression blogIdCondition(List<Long> blogIds) {
        return (blogIds != null && !blogIds.isEmpty()) ?
                post.blog.id.in(blogIds) : null;
    }

    private BooleanExpression categoriesContainsAll(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }

        return JPAExpressions
                .select(postCategory.countDistinct())
                .from(postCategory)
                .join(postCategory.category, category)
                .where(postCategory.post.eq(post),
                        category.name.in(categories))
                .eq((long) categories.size());
    }

    private BooleanExpression tagsContainsAll(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        return JPAExpressions
                .select(postTag.countDistinct())
                .from(postTag)
                .join(postTag.tag, tag)
                .where(postTag.post.eq(post),
                        tag.name.in(tags))
                .eq((long) tags.size());
    }

    private BooleanExpression dateAfterCondition(LocalDate publishedAfter) {
        return publishedAfter != null ? post.publishedAt.goe(publishedAfter.atStartOfDay()) : null;
    }

    private BooleanExpression dateBeforeCondition(LocalDate publishedBefore) {
        return publishedBefore != null ? post.publishedAt.loe(publishedBefore.atStartOfDay()) : null;
    }

    private OrderSpecifier<?> buildOrderSpecifier(SearchCondition searchCondition, Pageable pageable) {
        String sortBy = searchCondition.getSortBy() != null ? searchCondition.getSortBy() : "publishedAt";
        Sort.Direction direction = searchCondition.getSortDirection() != null ?
                searchCondition.getSortDirection() : Sort.Direction.DESC;

        PathBuilder<PostEntity> pathBuilder = new PathBuilder<>(PostEntity.class, "postEntity");
        Order order = direction == Sort.Direction.ASC ? Order.ASC : Order.DESC;

        return new OrderSpecifier(order, pathBuilder.get(sortBy));
    }
}