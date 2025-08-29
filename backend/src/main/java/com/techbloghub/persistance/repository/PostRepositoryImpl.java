package com.techbloghub.persistance.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.techbloghub.persistance.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QPostEntity post = QPostEntity.postEntity;
    private static final QPostTagEntity postTags = QPostTagEntity.postTagEntity;
    private static final QPostCategoryEntity postCategory = QPostCategoryEntity.postCategoryEntity;
    private static final QTagEntity tags = QTagEntity.tagEntity;
    private static final QCategoryEntity category = QCategoryEntity.categoryEntity;

    @Override
    public Page<PostEntity> findAllOrderByPublishedAtDesc(Pageable pageable) {
        List<PostEntity> content = queryFactory
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
    public List<PostEntity> findRecentPosts(LocalDateTime since) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.blog).fetchJoin()
                .where(post.publishedAt.goe(since))
                .orderBy(post.publishedAt.desc())
                .fetch();
    }

    @Override
    public Page<PostEntity> findByTagNames(List<String> tagNames, Pageable pageable) {
        List<PostEntity> content = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.blog).fetchJoin()
                .leftJoin(post.postTags, postTags)
                .leftJoin(postTags.tag, tags)
                .where(tags.name.in(tagNames))
                .orderBy(post.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.postTags, postTags)
                .leftJoin(postTags.tag, tags)
                .where(tags.name.in(tagNames))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<PostEntity> findByCategoryNames(List<String> categoryNames, Pageable pageable) {
        List<PostEntity> content = queryFactory
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

    @Override
    public Page<PostEntity> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable) {
        List<PostEntity> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.blog).fetchJoin()
                .where(post.blog.id.eq(blogId))
                .orderBy(post.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(post.blog.id.eq(blogId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public long countByBlogId(Long blogId) {
        Long count = queryFactory
                .select(post.count())
                .from(post)
                .where(post.blog.id.eq(blogId))
                .fetchOne();
        
        return count != null ? count : 0L;
    }
}