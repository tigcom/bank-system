package com.example.transaction_service.mapper;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "failedReason", source = "failedReason")
    TransactionDTO toDTO(Transaction transaction);

    Transaction toEntity(TransactionDTO transactionDTO);
}
