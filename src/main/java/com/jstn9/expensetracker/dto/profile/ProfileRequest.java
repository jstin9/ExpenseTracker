package com.jstn9.expensetracker.dto.profile;

import com.jstn9.expensetracker.model.enums.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @NotBlank(message = "Name cannot be empty!")
    private String name;

    @NotNull(message = "Balance is required!")
    @PositiveOrZero(message = "Balance cannot be negative!")
    private BigDecimal balance;

    @NotNull(message = "Month salary is required!")
    @PositiveOrZero(message = "Salary cannot be negative!")
    private BigDecimal monthSalary;

    @NotNull(message = "Currency is required!")
    private CurrencyType currencyType;

}
