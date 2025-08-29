package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.port.in.SearchUseCase;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 검색 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 * SearchUseCase 인터페이스를 구현하여 검색 도메인 로직을 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService implements SearchUseCase {

    private final PostRepositoryPort postRepositoryPort;

    @Override
    public Page<Post> searchByKeyword(String keyword, int page, int size, String sortBy, String sortDirection) {
        log.debug("Searching posts by keyword: {} with pagination: page={}, size={}", keyword, page, size);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        return postRepositoryPort.searchByKeyword(keyword, pageable);
    }

    @Override
    public Page<Post> searchWithFilters(String keyword, List<String> companies, List<String> tags, 
                                       List<String> categories, int page, int size, 
                                       String sortBy, String sortDirection) {
        log.debug("Searching posts with filters - keyword: {}, companies: {}, tags: {}, categories: {}", 
                 keyword, companies, tags, categories);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        return postRepositoryPort.searchWithFilters(keyword, companies, tags, categories, pageable);
    }

    @Override
    public Page<Post> searchByTags(List<String> tagNames, int page, int size) {
        log.debug("Searching posts by tags: {}", tagNames);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return postRepositoryPort.findByTagNames(tagNames, pageable);
    }

    @Override
    public Page<Post> searchByCategories(List<String> categoryNames, int page, int size) {
        log.debug("Searching posts by categories: {}", categoryNames);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return postRepositoryPort.findByCategoryNames(categoryNames, pageable);
    }

    /**
     * 페이징 및 정렬 조건을 생성하는 헬퍼 메서드
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "publishedAt"
        );
        
        return PageRequest.of(page, size, sort);
    }
}