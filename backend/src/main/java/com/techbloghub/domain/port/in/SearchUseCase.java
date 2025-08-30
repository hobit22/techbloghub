package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 검색 관련 비즈니스 유스케이스 인터페이스
 */
public interface SearchUseCase {
    
    /**
     * 키워드로 포스트 검색
     */
    Page<Post> searchByKeyword(String keyword, int page, int size, String sortBy, String sortDirection);
    
    /**
     * 복합 조건으로 포스트 검색
     */
    Page<Post> searchWithFilters(String keyword, 
                                 List<String> companies, 
                                 List<String> tags, 
                                 List<String> categories,
                                 int page, int size, 
                                 String sortBy, String sortDirection);
    
    /**
     * 태그로 포스트 검색
     */
    Page<Post> searchByTags(List<String> tagNames, int page, int size);
    
    /**
     * 카테고리로 포스트 검색
     */
    Page<Post> searchByCategories(List<String> categoryNames, int page, int size);
}