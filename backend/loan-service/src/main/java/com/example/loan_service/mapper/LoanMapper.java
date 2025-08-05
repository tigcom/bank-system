package com.example.loan_service.mapper;

import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import com.example.loan_service.entity.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    LoanMapper INSTANCE = Mappers.getMapper(LoanMapper.class);

    /** Request DTO → Entity **/
    @Mappings({
            @Mapping(target = "loanId",                      source = "loanId"),
            @Mapping(target = "customerId",                  source = "customerId"),
            @Mapping(target = "disbursementAccountNumber",   source = "disbursementAccountNumber"),
            @Mapping(target = "repaymentAccountNumber",      source = "repaymentAccountNumber"),
            @Mapping(target = "amount",                      source = "amount"),
            @Mapping(target = "interestRate",                source = "interestRate"),
            @Mapping(target = "termMonths",                  source = "termMonths"),
            @Mapping(target = "status",                      source = "status"),
            @Mapping(target = "createdAt",                   source = "createdAt"),
            @Mapping(target = "approvedAt",                  source = "approvedAt"),
            @Mapping(target = "loanType",                    source = "loanType")
    })
    Loan toEntity(LoanRequestDTO dto);

    /** Entity → Response DTO **/
    @Mappings({
            @Mapping(target = "loanId",         source = "loanId"),
            @Mapping(target = "customerId",     source = "customerId"),
            // LoanResponseDTO chỉ có 1 field accountNumber, mình map từ disbursementAccountNumber
            @Mapping(target = "accountNumber",  source = "disbursementAccountNumber"),
            @Mapping(target = "amount",         source = "amount"),
            @Mapping(target = "interestRate",   source = "interestRate"),
            @Mapping(target = "termMonths",     source = "termMonths"),
            // startDate là LocalDate, tự động map từ createdAt nếu cấu hình MapStruct javaTimeConversion
            @Mapping(target = "startDate",      source = "createdAt"),
            @Mapping(target = "status",         source = "status"),
            @Mapping(target = "createdAt",      source = "createdAt"),
            @Mapping(target = "approvedAt",     source = "approvedAt")
    })
    LoanResponseDTO toDTO(Loan entity);

    /** Entity → Request DTO **/
    @Mappings({
            @Mapping(target = "loanId",                    source = "loanId"),
            @Mapping(target = "disbursementAccountNumber", source = "disbursementAccountNumber"),
            @Mapping(target = "repaymentAccountNumber",    source = "repaymentAccountNumber"),
            @Mapping(target = "amount",                    source = "amount"),
            @Mapping(target = "interestRate",              source = "interestRate"),
            @Mapping(target = "termMonths",                source = "termMonths"),
            @Mapping(target = "customerId",                source = "customerId"),
            @Mapping(target = "createdAt",                 source = "createdAt"),
            @Mapping(target = "approvedAt",                source = "approvedAt"),
            @Mapping(target = "status",                    source = "status"),
            @Mapping(target = "loanType",                  source = "loanType")
    })
    LoanRequestDTO toRequestDTO(Loan loan);

    /** Request DTO → Response DTO **/
    @Mappings({
            @Mapping(target = "loanId",         source = "loanId"),
            @Mapping(target = "customerId",     source = "customerId"),
            @Mapping(target = "accountNumber",  source = "disbursementAccountNumber"),
            @Mapping(target = "amount",         source = "amount"),
            @Mapping(target = "interestRate",   source = "interestRate"),
            @Mapping(target = "termMonths",     source = "termMonths"),
            @Mapping(target = "startDate",      source = "createdAt"),
            @Mapping(target = "status",         source = "status"),
            @Mapping(target = "createdAt",      source = "createdAt"),
            @Mapping(target = "approvedAt",     source = "approvedAt")
    })
    LoanResponseDTO toResponseDTO(LoanRequestDTO dto);
}
