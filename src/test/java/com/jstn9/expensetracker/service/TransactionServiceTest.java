package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.transaction.TransactionFilter;
import com.jstn9.expensetracker.dto.transaction.TransactionRequest;
import com.jstn9.expensetracker.dto.transaction.TransactionResponse;
import com.jstn9.expensetracker.exception.TransactionNotFoundException;
import com.jstn9.expensetracker.mapper.TransactionMapper;
import com.jstn9.expensetracker.model.Category;
import com.jstn9.expensetracker.model.Profile;
import com.jstn9.expensetracker.model.Transaction;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.model.enums.TransactionType;
import com.jstn9.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProfileService profileService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionBalanceService transactionBalanceService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    /**
     * Вспомогательный метод для настройки поведения мока маппера.
     * Имитирует работу MapStruct: берет данные из аргументов и перекладывает в Transaction.
     */
    private void setupMapperUpdateMock() {
        doAnswer(invocation -> {
            TransactionRequest req = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            Category category = invocation.getArgument(2);
            Transaction t = invocation.getArgument(3);

            t.setAmount(req.getAmount());
            t.setType(req.getType());
            t.setDescription(req.getDescription());
            t.setDate(req.getDate());
            t.setUser(user);
            t.setCategory(category);
            return null;
        }).when(transactionMapper).updateEntityFromRequest(any(), any(), any(), any());
    }

    @Test
    void getFiltered() {
        // Given
        TransactionFilter filter = new TransactionFilter();
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();

        Transaction transaction = new Transaction();
        TransactionResponse transactionResponse = new TransactionResponse();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction));

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findAll(
                Mockito.<Specification<Transaction>>any(),
                eq(pageable))
        ).thenReturn(transactionPage);
        when(transactionMapper.toTransactionResponse(transaction))
                .thenReturn(transactionResponse);

        // When
        Page<TransactionResponse> result = transactionService.getFiltered(filter, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(transactionResponse, result.getContent().get(0));

        // Verify
        verify(userService).getCurrentUser();
        verify(transactionRepository).findAll(Mockito.<Specification<Transaction>>any(), eq(pageable));
        verify(transactionMapper).toTransactionResponse(transaction);
    }

    @Test
    void getTransactionById_WhenTransactionIdExists_ThenReturnTransactionResponse() {
        // Given
        User user = new User();
        Long transactionId = 1L;

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();
        TransactionResponse transactionResponse = TransactionResponse.builder()
                .id(transactionId)
                .build();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUser(transactionId, user)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toTransactionResponse(transaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.getTransactionById(transactionId);

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getId());

        // Verify
        verify(userService).getCurrentUser();
        verify(transactionRepository).findByIdAndUser(transactionId, user);
        verify(transactionMapper).toTransactionResponse(transaction);
    }

    @Test
    void getTransactionById_WhenTransactionIdNotExists_ThenThrowException() {
        // Given
        User user = new User();
        Long badTransactionId = 1L;

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUser(badTransactionId, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(badTransactionId));

        // Verify
        verify(userService).getCurrentUser();
        verify(transactionRepository).findByIdAndUser(badTransactionId, user);
        verifyNoInteractions(transactionMapper);
    }

    @Test
    void createTransaction_ShouldSaveTransactionAndReturnTransactionResponse() {
        // Given
        User user = new User();
        Profile profile = new Profile();

        TransactionRequest request = TransactionRequest.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.INCOME)
                .description("test")
                .date(LocalDate.now())
                .categoryId(1L)
                .build();

        Transaction savedTransaction = Transaction.builder().id(1L).build();
        TransactionResponse transactionResponse = TransactionResponse.builder().id(1L).build();

        setupMapperUpdateMock(); // Настраиваем маппер

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(categoryService.getCategoryByIdForUser(request.getCategoryId(), user)).thenReturn(new Category());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toTransactionResponse(savedTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.createTransaction(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(categoryService).getCategoryByIdForUser(request.getCategoryId(), user);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionMapper).toTransactionResponse(savedTransaction);
    }

    @Test
    void updateTransaction_WhenTransactionIdExistsAndAmountOrTypeNotChanged_ThenReturnTransactionResponse() {
        // Given
        User user = new User();
        Profile profile = new Profile();

        Long transactionId = 1L;
        TransactionRequest request = TransactionRequest.builder()
                .amount(BigDecimal.valueOf(200)) // same amount and type with request so boolean will be false
                .type(TransactionType.INCOME)
                .build();

        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.INCOME)
                .build();

        TransactionResponse transactionResponse = TransactionResponse.builder()
                .id(transactionId)
                .amount(request.getAmount())
                .type(request.getType())
                .build();

        setupMapperUpdateMock(); // ВАЖНО: Маппер нужен и тут!

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(transactionRepository.findByIdAndUser(transactionId, user))
                .thenReturn(Optional.of(transaction));
        when(categoryService.getCategoryByIdForUser(request.getCategoryId(), user)).thenReturn(new Category());
        when(transactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(any(Transaction.class))).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        assertEquals(request.getAmount(), result.getAmount());
        assertEquals(request.getType(), result.getType());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(transactionRepository).findByIdAndUser(transactionId, user);
        verify(categoryService).getCategoryByIdForUser(request.getCategoryId(), user);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionMapper).toTransactionResponse(any(Transaction.class));

        verify(transactionBalanceService, never())
                .rollbackOldTransactionEffect(any(), any());
        verify(transactionBalanceService, never())
                .applyNewBalance(any(), any());
        verify(profileService, never())
                .saveProfile(any(Profile.class));
    }

    @Test
    void updateTransaction_WhenTransactionIdExistsAndAmountOrTypeChanged_ThenReturnTransactionResponse() {
        // Given
        Long transactionId = 1L;
        User user = new User();
        Profile profile = new Profile();

        TransactionRequest request = TransactionRequest.builder()
                .amount(BigDecimal.valueOf(400)) // not same amount and type with request so boolean will be true
                .type(TransactionType.EXPENSE)
                .description("test")
                .date(LocalDate.now())
                .categoryId(1L)
                .build();

        Transaction transactionInDb = Transaction.builder()
                .id(transactionId)
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.INCOME)
                .build();

        TransactionResponse expectedResponse = TransactionResponse.builder()
                .id(transactionId)
                .amount(request.getAmount())
                .type(request.getType())
                .build();

        setupMapperUpdateMock(); // Настраиваем маппер

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(transactionRepository.findByIdAndUser(transactionId, user))
                .thenReturn(Optional.of(transactionInDb));
        when(categoryService.getCategoryByIdForUser(eq(request.getCategoryId()), eq(user)))
                .thenReturn(new Category());
        when(transactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenReturn(expectedResponse);

        // When
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Then
        assertNotNull(result);

        assertAll("Checking the returned DTO",
                () -> assertEquals(request.getAmount(), result.getAmount()),
                () -> assertEquals(request.getType(), result.getType())
        );

        assertAll("Checking if an entity is updated before saving",
                () -> assertEquals(request.getAmount(), transactionInDb.getAmount()),
                () -> assertEquals(request.getType(), transactionInDb.getType()),
                () -> assertEquals(request.getDescription(), transactionInDb.getDescription()),
                () -> assertEquals(request.getDate(), transactionInDb.getDate()),
                () -> assertNotNull(transactionInDb.getCategory())
        );

        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(transactionRepository).findByIdAndUser(transactionId, user);
        verify(categoryService).getCategoryByIdForUser(request.getCategoryId(), user);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionMapper).toTransactionResponse(any(Transaction.class));

        verify(transactionBalanceService).rollbackOldTransactionEffect(eq(profile), any());
        verify(transactionBalanceService).applyNewBalance(eq(profile), any());
        verify(profileService).saveProfile(any(Profile.class));
    }

    @Test
    void updateTransaction_WhenTransactionIdNotExists_ThenThrowException() {
        // Given
        Long badTransactionId = 1L;
        User user = new User();
        Profile profile = new Profile();
        TransactionRequest request = new TransactionRequest();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(transactionRepository.findByIdAndUser(badTransactionId, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(badTransactionId, request));

        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(transactionRepository).findByIdAndUser(badTransactionId, user);
        verifyNoMoreInteractions(transactionRepository, transactionBalanceService, profileService);
    }

    @Test
    void deleteById_WhenTransactionIdExists_ThenDeleteTransaction() {
        // Given
        Long transactionId = 1L;
        User user = new User();
        Profile profile = new Profile();

        Transaction transactionInDb = Transaction.builder()
                .id(transactionId)
                .build();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(transactionRepository.findByIdAndUser(transactionId, user))
                .thenReturn(Optional.of(transactionInDb));

        // When
        transactionService.deleteById(transactionId);

        // Then
        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(transactionRepository).findByIdAndUser(transactionId, user);
        verify(transactionBalanceService).rollbackOldTransactionEffect(eq(profile), any());
        verify(profileService).saveProfile(any(Profile.class));
        verify(transactionRepository).delete(transactionInDb);
    }

    @Test
    void deleteById_WhenTransactionIdNotExists_ThenThrowsException() {
        // Given
        Long badTransactionId = 1L;
        User user = new User();
        Profile profile = new Profile();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileService.getCurrentUserProfile(user)).thenReturn(profile);
        when(transactionRepository.findByIdAndUser(badTransactionId, user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.deleteById(badTransactionId));

        // Verify
        verify(userService).getCurrentUser();
        verify(profileService).getCurrentUserProfile(user);
        verify(transactionRepository).findByIdAndUser(badTransactionId, user);
        verifyNoMoreInteractions(transactionBalanceService, profileService, transactionRepository);
    }
}