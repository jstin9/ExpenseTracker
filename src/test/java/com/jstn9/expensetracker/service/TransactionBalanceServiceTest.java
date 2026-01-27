package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.exception.BalanceNegativeException;
import com.jstn9.expensetracker.model.Profile;
import com.jstn9.expensetracker.model.Transaction;
import com.jstn9.expensetracker.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionBalanceServiceTest {

    private TransactionBalanceService transactionBalanceService;

    @BeforeEach
    void setUp() {
        transactionBalanceService = new TransactionBalanceService();
    }

    @Test
    void rollbackOldTransactionEffect_WhenIncome_ThenSubtractBalance() {
        // Given
        Profile profile = new Profile();
        profile.setBalance(BigDecimal.valueOf(1000));

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.INCOME)
                .build();

        // When
        transactionBalanceService.rollbackOldTransactionEffect(profile, transaction);

        // Then
        assertEquals(BigDecimal.valueOf(800), profile.getBalance());
    }

    @Test
    void rollbackOldTransactionEffect_WhenExpense_ThenAddBalance() {
        // Given
        Profile profile = new Profile();
        profile.setBalance(BigDecimal.valueOf(1000));

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.EXPENSE)
                .build();

        // When
        transactionBalanceService.rollbackOldTransactionEffect(profile, transaction);

        // Then
        assertEquals(BigDecimal.valueOf(1200), profile.getBalance());
    }

    @Test
    void applyNewBalance_WhenIncome_ThenAddBalance() {
        // Given
        Profile profile = new Profile();
        profile.setBalance(BigDecimal.valueOf(1000));

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.INCOME)
                .build();

        // When
        transactionBalanceService.applyNewBalance(profile, transaction);

        // Then
        assertEquals(BigDecimal.valueOf(1200), profile.getBalance());
    }

    @Test
    void applyNewBalance_WhenExpenseAndBalancePositive_ThenSubtractBalance() {
        // Given
        Profile profile = new Profile();
        profile.setBalance(BigDecimal.valueOf(1000));

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.EXPENSE)
                .build();

        // When
        transactionBalanceService.applyNewBalance(profile, transaction);

        // Then
        assertEquals(BigDecimal.valueOf(800), profile.getBalance());
    }

    @Test
    void applyNewBalance_WhenExpenseAndBalanceNegative_ThenThrowsException() {
        // Given
        Profile profile = new Profile();
        profile.setBalance(BigDecimal.valueOf(100));

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.EXPENSE)
                .build();

        // When & Then
        assertThrows(BalanceNegativeException.class,
                () -> transactionBalanceService.applyNewBalance(profile, transaction));
    }
}