package com.techbloghub.persistance.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.persistance.entity.PostEntity;
import com.techbloghub.persistance.entity.QPostEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QPostEntity post = QPostEntity.postEntity;

    @Override
    public Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(searchCondition, pageable);

        List<PostEntity> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.blog).fetchJoin()
                .where(
                        keywordCondition(searchCondition.getKeyword()),
                        blogIdCondition(searchCondition.getBlogIds()),
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
                        dateAfterCondition(searchCondition.getPublishedAfter()),
                        dateBeforeCondition(searchCondition.getPublishedBefore())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
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