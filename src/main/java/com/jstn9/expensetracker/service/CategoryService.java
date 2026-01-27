package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.category.CategoryCreateRequest;
import com.jstn9.expensetracker.dto.category.CategoryResponse;
import com.jstn9.expensetracker.dto.category.CategoryUpdateRequest;
import com.jstn9.expensetracker.exception.CategoryAlreadyExistsException;
import com.jstn9.expensetracker.exception.CategoryIsUsedInTransactionException;
import com.jstn9.expensetracker.exception.CategoryNotFoundException;
import com.jstn9.expensetracker.mapper.CategoryMapper;
import com.jstn9.expensetracker.model.Category;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.repository.CategoryRepository;
import com.jstn9.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository, UserService userService, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAllForCurrentUser(){
        User user = userService.getCurrentUser();

        List<Category> categories = categoryRepository.findByUserOrderByName(user);

        return categories.stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    public CategoryResponse getByIdForCurrentUser(Long id){
        User user = userService.getCurrentUser();

        Category category = findCategoryOrThrowException(id, user);

        return categoryMapper.toCategoryResponse(category);
    }

    public CategoryResponse createCategory(CategoryCreateRequest request){
        User user = userService.getCurrentUser();

        if(categoryRepository.existsByNameAndUser(request.getName(), user)){
            throw new CategoryAlreadyExistsException();
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setUser(user);
        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        User user = userService.getCurrentUser();

        Category category = findCategoryOrThrowException(id, user);

        category.setName(request.getName());
        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    public void deleteCategory(Long id) {
        User user = userService.getCurrentUser();

        Category category = findCategoryOrThrowException(id, user);

        boolean isUsed = transactionRepository.existsByCategory(category);
        if(isUsed){
            throw new CategoryIsUsedInTransactionException();
        }

        categoryRepository.delete(category);
    }

    public Category getCategoryByIdForUser(Long id, User user){
        return findCategoryOrThrowException(id, user);
    }

    private Category findCategoryOrThrowException(Long id, User user){
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(CategoryNotFoundException::new);
    }
}
