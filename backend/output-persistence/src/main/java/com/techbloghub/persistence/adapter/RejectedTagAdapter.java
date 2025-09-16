package com.techbloghub.persistence.adapter;

import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.entity.RejectedCategoryEntity;
import com.techbloghub.persistence.entity.RejectedTagEntity;
import com.techbloghub.persistence.repository.RejectedCategoryRepository;
import com.techbloghub.persistence.repository.RejectedTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거부된 태그/카테고리 저장 및 관리 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RejectedTagAdapter {
    
    private final RejectedTagRepository rejectedTagRepository;
    private final RejectedCategoryRepository rejectedCategoryRepository;
    
    /**
     * 거부된 태그 저장 또는 빈도 증가
     */
    @Transactional
    public void saveRejectedTag(String tagName, PostEntity post) {
        try {
            var existing = rejectedTagRepository.findByTagNameAndPost(tagName, post);
            
            if (existing.isPresent()) {
                // 이미 존재하면 빈도 증가
                existing.get().incrementFrequency();
                log.debug("Incremented frequency for rejected tag '{}' to {}", 
                    tagName, existing.get().getFrequencyCount());
            } else {
                // 새로 생성
                var rejectedTag = new RejectedTagEntity(tagName, post);
                rejectedTagRepository.save(rejectedTag);
                log.debug("Saved new rejected tag '{}' for post {}", tagName, post.getId());
            }
            
        } catch (Exception e) {
            log.error("Error saving rejected tag '{}' for post {}: {}", tagName, post.getId(), e.getMessage());
        }
    }
    
    /**
     * 거부된 카테고리 저장 또는 빈도 증가
     */
    @Transactional
    public void saveRejectedCategory(String categoryName, PostEntity post) {
        try {
            var existing = rejectedCategoryRepository.findByCategoryNameAndPost(categoryName, post);
            
            if (existing.isPresent()) {
                // 이미 존재하면 빈도 증가
                existing.get().incrementFrequency();
                log.debug("Incremented frequency for rejected category '{}' to {}", 
                    categoryName, existing.get().getFrequencyCount());
            } else {
                // 새로 생성
                var rejectedCategory = new RejectedCategoryEntity(categoryName, post);
                rejectedCategoryRepository.save(rejectedCategory);
                log.debug("Saved new rejected category '{}' for post {}", categoryName, post.getId());
            }
            
        } catch (Exception e) {
            log.error("Error saving rejected category '{}' for post {}: {}", categoryName, post.getId(), e.getMessage());
        }
    }
    
    /**
     * 여러 거부된 태그들을 배치로 저장
     */
    @Transactional
    public void saveRejectedTags(List<String> tagNames, PostEntity post) {
        for (String tagName : tagNames) {
            saveRejectedTag(tagName, post);
        }
    }
    
    /**
     * 여러 거부된 카테고리들을 배치로 저장
     */
    @Transactional
    public void saveRejectedCategories(List<String> categoryNames, PostEntity post) {
        for (String categoryName : categoryNames) {
            saveRejectedCategory(categoryName, post);
        }
    }
    
    /**
     * 승인 후보 태그들 조회 (최소 빈도 이상)
     */
    @Transactional(readOnly = true)
    public List<RejectedTagEntity> getApprovalCandidateTags(int minFrequency) {
        return rejectedTagRepository.findApprovalCandidates(
            RejectedTagEntity.RejectedStatus.PENDING, minFrequency);
    }
    
    /**
     * 승인 후보 카테고리들 조회 (최소 빈도 이상)
     */
    @Transactional(readOnly = true)
    public List<RejectedCategoryEntity> getApprovalCandidateCategories(int minFrequency) {
        return rejectedCategoryRepository.findApprovalCandidates(
            RejectedTagEntity.RejectedStatus.PENDING, minFrequency);
    }
    
    /**
     * 최근 거부된 태그들 조회 (트렌드 분석용)
     */
    @Transactional(readOnly = true)
    public List<RejectedTagEntity> getRecentRejectedTags(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return rejectedTagRepository.findRecentRejected(
            RejectedTagEntity.RejectedStatus.PENDING, since);
    }
    
    /**
     * 태그 승인 처리
     */
    @Transactional
    public void approveTag(String tagName) {
        var rejectedTags = rejectedTagRepository.findByStatusOrderByFrequencyCountDesc(
            RejectedTagEntity.RejectedStatus.PENDING);
        
        rejectedTags.stream()
            .filter(tag -> tag.getTagName().equals(tagName))
            .forEach(RejectedTagEntity::approve);
        
        log.info("Approved rejected tag: {}", tagName);
    }
    
    /**
     * 카테고리 승인 처리
     */
    @Transactional
    public void approveCategory(String categoryName) {
        var rejectedCategories = rejectedCategoryRepository.findByStatusOrderByFrequencyCountDesc(
            RejectedTagEntity.RejectedStatus.PENDING);
        
        rejectedCategories.stream()
            .filter(category -> category.getCategoryName().equals(categoryName))
            .forEach(RejectedCategoryEntity::approve);
        
        log.info("Approved rejected category: {}", categoryName);
    }
}