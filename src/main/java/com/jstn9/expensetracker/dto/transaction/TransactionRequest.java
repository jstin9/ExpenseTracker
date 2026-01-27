package com.jstn9.expensetracker.dto.transaction;

import com.jstn9.expensetracker.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class TransactionRequest {

    @NotNull(message = "Amount is required!")
    @Positive(message = "Amount cannot be negative or zero!")
    private BigDecimal amount;

    @NotNull(message = "Type is required!")
    private TransactionType type;

    @Size(max = 255, message = "Description cannot exceed 255 characters!")
    private String description;

    @NotNull(message = "Data is required!")
    private LocalDate date;

    @NotNull(message = "Category id is required!")
    private Long categoryId;
}
