package com.jstn9.expensetracker.mapper;

import com.jstn9.expensetracker.dto.transaction.TransactionRequest;
import com.jstn9.expensetracker.dto.transaction.TransactionResponse;
import com.jstn9.expensetracker.model.Category;
import com.jstn9.expensetracker.model.Transaction;
import com.jstn9.expensetracker.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses =  CategoryMapper.class)
public interface TransactionMapper {
    TransactionResponse toTransactionResponse(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "category", source = "category")
    void updateEntityFromRequest(TransactionRequest request,
                                 User user,
                                 Category category,
                                 @MappingTarget Transaction transaction);
}
