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
import com.jstn9.expensetracker.repository.TransactionRepository;
import com.jstn9.expensetracker.specification.TransactionSpecification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final ProfileService profileService;
    private final TransactionMapper transactionMapper;
    private final TransactionBalanceService transactionBalanceService;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository,
                              UserService userService,
                              ProfileService profileService,
                              TransactionMapper transactionMapper,
                              TransactionBalanceService transactionBalanceService,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.profileService = profileService;
        this.transactionMapper = transactionMapper;
        this.transactionBalanceService = transactionBalanceService;
        this.categoryService = categoryService;
    }

    public Page<TransactionResponse> getFiltered(TransactionFilter filter, Pageable pageable) {
        User user = userService.getCurrentUser();

        return transactionRepository.findAll(TransactionSpecification
                        .filter(filter, user), pageable)
                .map(transactionMapper::toTransactionResponse);
    }

    public TransactionResponse getTransactionById(Long id) {
        User user = userService.getCurrentUser();

        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(TransactionNotFoundException::new);

        return transactionMapper.toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = userService.getCurrentUser();
        Profile profile = profileService.getCurrentUserProfile(user);

        Transaction transaction = new Transaction();

        Category category = categoryService.getCategoryByIdForUser(request.getCategoryId(), user);
        transactionMapper.updateEntityFromRequest(request, user, category, transaction);

        transactionBalanceService.applyNewBalance(profile, transaction);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        User user = userService.getCurrentUser();
        Profile profile = profileService.getCurrentUserProfile(user);

        Transaction transaction = findOldTransactionByIdAndUser(id, user);

        boolean isAmountOrTypeChanged = isAmountOrTypeChanged(transaction, request);

        if (isAmountOrTypeChanged) {
            transactionBalanceService.rollbackOldTransactionEffect(profile, transaction);
        }

        Category category = categoryService.getCategoryByIdForUser(request.getCategoryId(), user);
        transactionMapper.updateEntityFromRequest(request, user, category, transaction);

        if (isAmountOrTypeChanged) {
            transactionBalanceService.applyNewBalance(profile, transaction);
            profileService.saveProfile(profile);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional
    public void deleteById(Long id) {
        User user = userService.getCurrentUser();
        Profile profile = profileService.getCurrentUserProfile(user);

        Transaction transaction = findOldTransactionByIdAndUser(id, user);

        transactionBalanceService.rollbackOldTransactionEffect(profile, transaction);

        profileService.saveProfile(profile);

        transactionRepository.delete(transaction);
    }

    private Transaction findOldTransactionByIdAndUser(Long id, User user) {
        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private boolean isAmountOrTypeChanged(Transaction oldTransaction, TransactionRequest request) {
        return oldTransaction.getAmount().compareTo(request.getAmount()) != 0 || oldTransaction.getType() != request.getType();
    }
}
