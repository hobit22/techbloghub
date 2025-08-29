package com.techbloghub.service;

import com.techbloghub.dto.SearchRequest;
import com.techbloghub.entity.PostDocument;
import com.techbloghub.repository.PostDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PostDocumentRepository postDocumentRepository;

    public Page<PostDocument> search(SearchRequest request) {
        try {
            Pageable pageable = createPageable(request);

            // 간단한 검색 구현 - 복잡한 ElasticSearch 쿼리 대신 Repository 메서드 사용
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                return postDocumentRepository.findByTitleContainingOrContentContaining(
                        request.getQuery(), request.getQuery(), pageable);
            }

            // 필터가 있는 경우
            if (hasFilters(request)) {
                if (request.getCompanies() != null && !request.getCompanies().isEmpty()) {
                    // 첫 번째 회사로 검색 (단순화)
                    return postDocumentRepository.findByCompany(request.getCompanies().get(0), pageable);
                }
                if (request.getTags() != null && !request.getTags().isEmpty()) {
                    return postDocumentRepository.findByTagsContaining(request.getTags().get(0), pageable);
                }
                if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                    return postDocumentRepository.findByCategoriesContaining(request.getCategories().get(0), pageable);
                }
            }

            // 기본 전체 검색
            return postDocumentRepository.findAll(pageable);

        } catch (Exception e) {
            log.error("Error performing search: {}", e.getMessage());
            return Page.empty();
        }
    }

    private boolean hasFilters(SearchRequest request) {
        return (request.getCompanies() != null && !request.getCompanies().isEmpty()) ||
                (request.getTags() != null && !request.getTags().isEmpty()) ||
                (request.getCategories() != null && !request.getCategories().isEmpty());
    }

    private Pageable createPageable(SearchRequest request) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy()
        );

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}