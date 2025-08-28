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
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    
    private final PostDocumentRepository postDocumentRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;
    
    public Page<PostDocument> search(SearchRequest request) {
        try {
            Pageable pageable = createPageable(request);
            
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return searchWithFilters(request, pageable);
            } else {
                return searchWithQueryAndFilters(request, pageable);
            }
        } catch (Exception e) {
            log.error("Error performing search: {}", e.getMessage());
            return Page.empty();
        }
    }
    
    private Page<PostDocument> searchWithFilters(SearchRequest request, Pageable pageable) {
        if (hasFilters(request)) {
            BoolQuery.Builder boolQuery = QueryBuilders.bool();
            
            addFilters(boolQuery, request);
            
            Query searchQuery = org.springframework.data.elasticsearch.core.query.NativeQuery.builder()
                    .withQuery(boolQuery.build()._toQuery())
                    .withPageable(pageable)
                    .build();
            
            return elasticsearchTemplate.search(searchQuery, PostDocument.class).map(searchHit -> searchHit.getContent());
        }
        
        return postDocumentRepository.findAll(pageable);
    }
    
    private Page<PostDocument> searchWithQueryAndFilters(SearchRequest request, Pageable pageable) {
        BoolQuery.Builder boolQuery = QueryBuilders.bool();
        
        var multiMatchQuery = QueryBuilders.multiMatch()
                .query(request.getQuery())
                .fields("title^2", "content", "author")
                .fuzziness("AUTO");
        
        boolQuery.must(multiMatchQuery.build()._toQuery());
        
        addFilters(boolQuery, request);
        
        Query searchQuery = org.springframework.data.elasticsearch.core.query.NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(pageable)
                .build();
        
        return elasticsearchTemplate.search(searchQuery, PostDocument.class).map(searchHit -> searchHit.getContent());
    }
    
    private void addFilters(BoolQuery.Builder boolQuery, SearchRequest request) {
        if (request.getCompanies() != null && !request.getCompanies().isEmpty()) {
            var companyQuery = QueryBuilders.terms()
                    .field("company")
                    .terms(t -> t.value(request.getCompanies().stream()
                            .map(c -> co.elastic.clients.elasticsearch._types.FieldValue.of(c))
                            .toList()));
            boolQuery.filter(companyQuery.build()._toQuery());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            var tagQuery = QueryBuilders.terms()
                    .field("tags")
                    .terms(t -> t.value(request.getTags().stream()
                            .map(tag -> co.elastic.clients.elasticsearch._types.FieldValue.of(tag))
                            .toList()));
            boolQuery.filter(tagQuery.build()._toQuery());
        }
        
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            var categoryQuery = QueryBuilders.terms()
                    .field("categories")
                    .terms(t -> t.value(request.getCategories().stream()
                            .map(cat -> co.elastic.clients.elasticsearch._types.FieldValue.of(cat))
                            .toList()));
            boolQuery.filter(categoryQuery.build()._toQuery());
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