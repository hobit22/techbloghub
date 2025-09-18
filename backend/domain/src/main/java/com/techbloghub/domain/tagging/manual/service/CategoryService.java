package com.techbloghub.domain.tagging.manual.service;

import com.techbloghub.domain.tagging.manual.model.Category;
import com.techbloghub.domain.tagging.manual.usecase.CategoryUseCase;
import com.techbloghub.domain.tagging.manual.port.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepositoryPort.findAll();
    }
}