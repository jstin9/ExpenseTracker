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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;


    @Test
    void getAllForCurrentUser() {
        // Given
        User user = new User();

        List<Category> categories = new ArrayList<>(List.of(
                new Category(1L, "testCategory1", user),
                new Category(2L, "testCategory2", user),
                new Category(3L, "testCategory3", user)));

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByUserOrderByName(user)).thenReturn(categories);
        when(categoryMapper.toCategoryResponse(any(Category.class)))
                .thenReturn(new CategoryResponse(1L, "testCategory1"));

        // When
        List<CategoryResponse> categoryResponses = categoryService.getAllForCurrentUser();

        // Then
        assertNotNull(categoryResponses);
        assertEquals(3, categoryResponses.size());

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByUserOrderByName(user);
        verify(categoryMapper, times(3)).toCategoryResponse(any(Category.class));

    }

    @Test
    void getAllForCurrentUser_WhenNoCategories_ThenReturnsEmptyList() {
        // Given
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByUserOrderByName(user)).thenReturn(new ArrayList<>());

        // When
        List<CategoryResponse> categoryResponses = categoryService.getAllForCurrentUser();

        // Then
        assertNotNull(categoryResponses);

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByUserOrderByName(user);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void getByIdForCurrentUser_WhenCategoryFound_ThenReturnsCategory() {
        // Given
        Long id = 1L;
        User user = new User();
        Category category = new Category(id, "testCategory1", user);

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(id, user))
                .thenReturn(Optional.of(category));
        when(categoryMapper.toCategoryResponse(category))
                .thenReturn(new CategoryResponse(id, "testCategory1"));

        // When
        CategoryResponse categoryResponse = categoryService.getByIdForCurrentUser(id);

        // Then
        assertNotNull(categoryResponse);
        assertEquals(id, categoryResponse.getId());

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(id, user);
        verify(categoryMapper).toCategoryResponse(any(Category.class));
    }

    @Test
    void getByIdForCurrentUser_WhenCategoryNotFound_ThenThrowsException() {
        // Given
        Long idNotFound = 1L;
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(idNotFound, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getByIdForCurrentUser(idNotFound));

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(idNotFound, user);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void createCategory_WhenNameDoesNotExist_ThenReturnsCreatedCategory() {
        // Given
        CategoryCreateRequest request = new CategoryCreateRequest("testName");
        User user = new User();
        Category category = new Category(1L, request.getName(), user);

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.existsByNameAndUser(request.getName(), user)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toCategoryResponse(category))
                .thenReturn(new CategoryResponse(category.getId(), category.getName()));

        // When
        CategoryResponse categoryResponse = categoryService.createCategory(request);

        // Then
        assertNotNull(categoryResponse);
        assertEquals(request.getName(), categoryResponse.getName());

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).existsByNameAndUser(request.getName(), user);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryResponse(category);
    }

    @Test
    void createCategory_WhenNameExists_ThenThrowsException() {
        // Given
        CategoryCreateRequest request = new CategoryCreateRequest("testName");
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.existsByNameAndUser(request.getName(), user)).thenReturn(true);

        // When & Then
        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.createCategory(request));

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).existsByNameAndUser(request.getName(), user);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WhenValidData_ThenReturnsUpdatedCategory() {
        // Given
        Long id = 1L;
        CategoryUpdateRequest request = new CategoryUpdateRequest("testName");
        User user = new User();
        Category category = new Category(id, "testCategory1", user);
        Category updatedCategory = new Category(id, request.getName(), user);

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(id, user))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(updatedCategory);
        when(categoryMapper.toCategoryResponse(updatedCategory))
                .thenReturn(new CategoryResponse(id, updatedCategory.getName()));

        // When
        CategoryResponse categoryResponse = categoryService.updateCategory(id, request);

        // Then
        assertNotNull(categoryResponse);
        assertEquals(request.getName(), categoryResponse.getName());

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(id, user);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toCategoryResponse(updatedCategory);
    }

    @Test
    void updateCategory_WhenCategoryNotFound_ThenThrowsException() {
        // Given
        Long idNotFound = 1L;
        CategoryUpdateRequest request = new CategoryUpdateRequest("testName");
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(idNotFound, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(idNotFound, request));

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(idNotFound, user);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_WhenCategoryExistsAndNotUsed_ThenDeletesCategory() {
        // Given
        Long id = 1L;
        User user = new User();
        Category category = new Category(id, "testCategory1", user);

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory(category)).thenReturn(false);

        // When
        categoryService.deleteCategory(id);

        // Then (Verify)
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(id, user);
        verify(transactionRepository).existsByCategory(category);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_WhenCategoryNotFound_ThenThrowsException() {
        // Given
        Long idNotFound = 1L;
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(idNotFound, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(idNotFound));

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(idNotFound, user);
        verify(transactionRepository, never()).existsByCategory(any(Category.class));
    }

    @Test
    void deleteCategory_WhenCategoryIsUsed_ThenThrowsException() {
        // Given
        Long id = 1L;
        User user = new User();
        Category category = new Category(id, "testCategory1", user);

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategory(category)).thenReturn(true);

        // When & Then
        assertThrows(CategoryIsUsedInTransactionException.class, () -> categoryService.deleteCategory(id));

        // Verify
        verify(userService).getCurrentUser();
        verify(categoryRepository).findByIdAndUser(id, user);
        verify(transactionRepository).existsByCategory(category);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void getCategoryByIdForUser_WhenCategoryExists_ThenReturnsCategory() {
        // Given
        Long id = 1L;
        User user = new User();

        // Behavior stubs
        when(categoryRepository.findByIdAndUser(id, user))
                .thenReturn(Optional.of(new Category(id, "testCategory1", user)));

        // When
        Category category = categoryService.getCategoryByIdForUser(id, user);

        // Then
        assertNotNull(category);
        assertEquals(id, category.getId());

        // Verify
        verify(categoryRepository).findByIdAndUser(id, user);
    }

    @Test
    void getCategoryByIdForUser_WhenCategoryNotFound_ThenThrowsException() {
        // Given
        Long idNotFound = 1L;
        User user = new User();

        // Behavior stubs
        when(categoryRepository.findByIdAndUser(idNotFound, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryByIdForUser(idNotFound, user));

        // Verify
        verify(categoryRepository).findByIdAndUser(idNotFound, user);
    }
}