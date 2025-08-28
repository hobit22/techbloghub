package com.techbloghub.repository;

import com.techbloghub.entity.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {
    
    Page<PostDocument> findByTitleContainingOrContentContaining(
            String title, String content, Pageable pageable);
    
    Page<PostDocument> findByCompany(String company, Pageable pageable);
    
    Page<PostDocument> findByTagsContaining(String tag, Pageable pageable);
    
    Page<PostDocument> findByCategoriesContaining(String category, Pageable pageable);
    
    List<PostDocument> findByBlogId(Long blogId);
}