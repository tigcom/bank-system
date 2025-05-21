package com.example.loan_service.mapper;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LoanMapper {
    LoanMapper INSTANCE = Mappers.getMapper(LoanMapper.class);

    @Mapping(target = "loanId", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Loan toEntity(LoanRequestDTO dto);

    LoanResponseDTO toDTO(Loan entity);
}