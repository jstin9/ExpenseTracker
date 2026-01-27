package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.statistics.IncomeExpense;
import com.jstn9.expensetracker.dto.transaction.TransactionResponse;
import com.jstn9.expensetracker.mapper.TransactionMapper;
import com.jstn9.expensetracker.model.Transaction;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getIncomeExpenseStats() {
        //Given
        LocalDate from = LocalDate.of(2025,1,1);
        LocalDate to = LocalDate.of(2025,2,1);
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.getIncome(user, from, to)).thenReturn(BigDecimal.valueOf(1000));
        when(transactionRepository.getExpense(user, from, to)).thenReturn(BigDecimal.valueOf(1000));

        // When
        IncomeExpense incomeExpense = statisticsService.getIncomeExpenseStats(from, to);

        // Then
        assertNotNull(incomeExpense);
        assertEquals(BigDecimal.valueOf(1000), incomeExpense.getIncome());
        assertEquals(BigDecimal.valueOf(1000), incomeExpense.getExpense());

        // Verify
        verify(userService).getCurrentUser();
        verify(transactionRepository).getIncome(user, from, to);
        verify(transactionRepository).getExpense(user, from, to);
    }

    @Test
    void getLastTransactions() {
        // Given
        int count = 10;

        User user = new User();
        List<Transaction> transactions = List.of(
                Transaction.builder().id(1L).build(),
                Transaction.builder().id(2L).build(),
                Transaction.builder().id(3L).build()
        );
        TransactionResponse transactionResponse = new TransactionResponse();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByUserOrderByDateDesc(eq(user), any(Pageable.class)))
                .thenReturn(transactions);
        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenReturn(transactionResponse);

        // When
        List<TransactionResponse> result = statisticsService.getLastTransactions(count);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify
        verify(userService).getCurrentUser();
        verify(transactionRepository).findByUserOrderByDateDesc(eq(user), any(Pageable.class));
        verify(transactionMapper, times(3)).toTransactionResponse(any(Transaction.class));

    }
}