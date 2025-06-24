package com.example.loan_service.mapper;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {
    @Mapping(target = "loanId", source = "loanId")
    @Mapping(target = "status",source = "status" )
    @Mapping(target = "createdAt",source = "createdAt")
    @Mapping(target="approvedAt", source="approvedAt")
    @Mapping(target="customerId", source="customerId")
    Loan toEntity(LoanRequestDTO dto);
    LoanResponseDTO toDTO(Loan entity);
    LoanRequestDTO toRequestDTO(Loan loan);
    LoanResponseDTO toResponseDTO(LoanRequestDTO loan);
}
