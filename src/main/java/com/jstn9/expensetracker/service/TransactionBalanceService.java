package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.exception.BalanceNegativeException;
import com.jstn9.expensetracker.model.Profile;
import com.jstn9.expensetracker.model.Transaction;
import com.jstn9.expensetracker.model.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionBalanceService {

    public void rollbackOldTransactionEffect(Profile profile, Transaction oldTransaction) {
        BigDecimal amount = oldTransaction.getAmount();

        if (oldTransaction.getType() == TransactionType.INCOME) {
            profile.subtractBalance(amount);
        } else {
            profile.addBalance(amount);
        }
    }

    public void applyNewBalance(Profile profile, Transaction transaction) {
        BigDecimal amount = transaction.getAmount();

        if (transaction.getType() == TransactionType.INCOME) {
            profile.addBalance(amount);
        } else {
            if (profile.getBalance().compareTo(amount) < 0) {
                throw new BalanceNegativeException();
            }
            profile.subtractBalance(amount);
        }
    }
}
