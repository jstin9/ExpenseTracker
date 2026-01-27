package com.jstn9.expensetracker.dto.profile;

import com.jstn9.expensetracker.model.enums.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;

    private String name;

    private BigDecimal balance;

    private BigDecimal monthSalary;

    private CurrencyType currencyType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
