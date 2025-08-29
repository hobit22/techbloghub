package com.techbloghub.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.techbloghub.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.techbloghub.entity.QCategory.category;
import static com.techbloghub.entity.QPost.post;
import static com.techbloghub.entity.QPostCategory.postCategory;
import static com.techbloghub.entity.QPostTags.postTags;
import static com.techbloghub.entity.QTags.tags;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.blog).fetchJoin()
                .orderBy(post.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Post> findRecentPosts(LocalDateTime since) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.blog).fetchJoin()
                .where(post.publishedAt.goe(since))
                .orderBy(post.publishedAt.desc())
                .fetch();
    }

    @Override
    public Page<Post> findByTagNames(List<String> tagNames, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.blog).fetchJoin()
                .leftJoin(post.postTags, postTags)
                .leftJoin(postTags.tags, tags)
                .where(tags.name.in(tagNames))
                .orderBy(post.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.postTags, postTags)
                .leftJoin(postTags.tags, tags)
                .where(tags.name.in(tagNames))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Post> findByCategoryNames(List<String> categoryNames, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.blog).fetchJoin()
                .leftJoin(post.postCategories, postCategory)
                .leftJoin(postCategory.category, category)
                .where(category.name.in(categoryNames))
                .orderBy(post.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.postCategories, postCategory)
                .leftJoin(postCategory.category, category)
                .where(category.name.in(categoryNames))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }
}