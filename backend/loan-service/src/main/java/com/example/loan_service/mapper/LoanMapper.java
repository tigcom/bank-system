package com.example.loan_service.mapper;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {
    @Mapping(target = "loanId", ignore = true)
    @Mapping(target = "status", expression = "java(com.example.loan_service.models.LoanStatus.PENDING)")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())") // thời gian hiện tại
    Loan toEntity(LoanRequestDTO dto);


    LoanResponseDTO toDTO(Loan entity);


    LoanRequestDTO toRequestDTO(Loan loan);
}
