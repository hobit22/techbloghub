package com.techbloghub.domain.post.port;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.manual.model.Tag;

import java.util.List;

/**
 * Post-Tag relationship repository outbound port
 */
public interface PostTagRepositoryPort {
    
    /**
     * Save post-tag relationship
     * @param postId ID of the post
     * @param tagId ID of the tag
     */
    void save(Long postId, Long tagId);
    
    /**
     * Assign multiple tags to a post
     *
     * @param post 포스트
     * @param tags 연결할 태그 목록
     */
    void assignTagsToPost(Post post, List<Tag> tags);

    /**
     * Remove all tag connections from post
     *
     * @param postId 포스트 ID
     */
    void removeAllTagsFromPost(Long postId);

    /**
     * Find tags connected to post
     *
     * @param postId 포스트 ID
     * @return 연결된 태그 목록
     */
    List<Tag> findTagsByPostId(Long postId);
}