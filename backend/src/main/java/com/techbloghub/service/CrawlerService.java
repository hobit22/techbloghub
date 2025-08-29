package com.techbloghub.service;

import com.techbloghub.crawler.RssFeedCrawler;
import com.techbloghub.entity.*;
import com.techbloghub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final RssFeedCrawler rssFeedCrawler;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final PostDocumentRepository postDocumentRepository;
    private final PostTagsRepository postTagsRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final TagsService tagsService;
    private final CategoryService categoryService;

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void crawlAllFeeds() {
        log.info("Starting scheduled RSS feed crawling");

        List<Blog> activeBlogs = blogRepository.findActiveBlogs();

        for (Blog blog : activeBlogs) {
            try {
                crawlBlogFeed(blog);
            } catch (Exception e) {
                log.error("Error crawling feed for blog {}: {}", blog.getName(), e.getMessage());
            }
        }

        log.info("Completed scheduled RSS feed crawling");
    }

    @Transactional
    public void crawlBlogFeed(Blog blog) {
        log.info("Crawling feed for blog: {}", blog.getName());

        List<Post> newPosts = rssFeedCrawler.crawlFeed(blog);
        int savedCount = 0;

        for (Post post : newPosts) {
            if (!postRepository.existsByOriginalUrl(post.getOriginalUrl())) {
                try {
                    Post savedPost = postRepository.save(post);

                    // Create PostTags intermediate entities
                    Set<Tags> tags = tagsService.extractAndCreateTags(post.getTitle() + " " + post.getContent());
                    for (Tags tag : tags) {
                        PostTags postTags = PostTags.builder()
                                .post(savedPost)
                                .tags(tag)
                                .build();
                        postTagsRepository.save(postTags);
                    }

                    // Create PostCategory intermediate entities  
                    Set<Category> categories = categoryService.extractAndCreateCategories(post.getTitle() + " " + post.getContent());
                    for (Category category : categories) {
                        PostCategory postCategory = PostCategory.builder()
                                .post(savedPost)
                                .category(category)
                                .build();
                        postCategoryRepository.save(postCategory);
                    }

                    PostDocument postDocument = PostDocument.fromPost(savedPost);
                    postDocumentRepository.save(postDocument);

                    savedCount++;
                } catch (Exception e) {
                    log.error("Error saving post {}: {}", post.getTitle(), e.getMessage());
                }
            }
        }

        blog.setLastCrawledAt(LocalDateTime.now());
        blogRepository.save(blog);

        log.info("Saved {} new posts from {}", savedCount, blog.getName());
    }

    @Transactional
    public void crawlSpecificBlog(Long blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + blogId));

        crawlBlogFeed(blog);
    }

    @Transactional
    public void reindexAllPosts() {
        log.info("Starting reindexing of all posts to Elasticsearch");

        postDocumentRepository.deleteAll();

        List<Post> allPosts = postRepository.findAll();

        for (Post post : allPosts) {
            try {
                PostDocument postDocument = PostDocument.fromPost(post);
                postDocumentRepository.save(postDocument);
            } catch (Exception e) {
                log.error("Error reindexing post {}: {}", post.getId(), e.getMessage());
            }
        }

        log.info("Completed reindexing {} posts", allPosts.size());
    }
}