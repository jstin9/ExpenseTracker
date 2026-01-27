package com.jstn9.expensetracker.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryUpdateRequest {

    @NotBlank(message = "Category name cannot be empty!")
    private String name;
}
