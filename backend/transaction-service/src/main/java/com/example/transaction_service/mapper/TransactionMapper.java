package com.example.transaction_service.mapper;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionDTO toDTO(Transaction transaction);

    Transaction toEntity(TransactionDTO transactionDTO);
}
