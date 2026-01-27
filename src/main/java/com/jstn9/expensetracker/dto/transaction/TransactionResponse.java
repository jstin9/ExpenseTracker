package com.jstn9.expensetracker.dto.transaction;

import com.jstn9.expensetracker.dto.category.CategoryResponse;
import com.jstn9.expensetracker.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private Long id;

    private BigDecimal amount;

    private TransactionType type;

    private String description;

    private LocalDate date;

    private CategoryResponse category;
}
