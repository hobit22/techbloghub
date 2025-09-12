package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.in.CategoryUseCase;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
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