package com.example.loan_service.mapper;

import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import com.example.loan_service.entity.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = InfoIncomeMapper.class)
public interface LoanMapper {

    /** Request DTO → Entity **/
    @Mappings({
            @Mapping(target = "loanId",        source = "loanId"),
            @Mapping(target = "customerId",    source = "customerId"),
            @Mapping(target = "accountNumber", source = "accountNumber"),
            @Mapping(target = "amount",        source = "amount"),
            @Mapping(target = "interestRate",  source = "interestRate"),
            @Mapping(target = "termMonths",    source = "termMonths"),
            @Mapping(target = "status",        source = "status"),
            @Mapping(target = "createdAt",     source = "createdAt"),
            @Mapping(target = "approvedAt",    source = "approvedAt"),
            @Mapping(target = "infoIncome",    source = "infoIncome")
    })
    Loan toEntity(LoanRequestDTO dto);

    /** Entity → Response DTO **/
    @Mappings({
            @Mapping(target = "loanId",         source = "loanId"),
            @Mapping(target = "customerId",     source = "customerId"),
            @Mapping(target = "accountNumber",  source = "accountNumber"),
            @Mapping(target = "amount",         source = "amount"),
            @Mapping(target = "interestRate",   source = "interestRate"),
            @Mapping(target = "termMonths",     source = "termMonths"),
            @Mapping(target = "startDate",      source = "createdAt"),                       // LocalDateTime → LocalDate
            @Mapping(target = "infoIncome", source = "infoIncome"),      // từ nested InfoIncome
            @Mapping(target = "status",         source = "status"),
            @Mapping(target = "createdAt",      source = "createdAt"),
            @Mapping(target = "approvedAt",     source = "approvedAt")
    })
    LoanResponseDTO toDTO(Loan entity);

    /** Entity → Request DTO **/
    @Mappings({
            @Mapping(target = "loanId",        source = "loanId"),
            @Mapping(target = "accountNumber", source = "accountNumber"),
            @Mapping(target = "amount",        source = "amount"),
            @Mapping(target = "interestRate",  source = "interestRate"),
            @Mapping(target = "termMonths",    source = "termMonths"),
            @Mapping(target = "customerId",    source = "customerId"),
            @Mapping(target = "createdAt",     source = "createdAt"),
            @Mapping(target = "approvedAt",    source = "approvedAt"),
            @Mapping(target = "status",        source = "status"),
            @Mapping(target = "infoIncome",    source = "infoIncome")
    })
    LoanRequestDTO toRequestDTO(Loan loan);

    /** Request DTO → Response DTO **/
    @Mappings({
            @Mapping(target = "loanId",         source = "loanId"),
            @Mapping(target = "customerId",     source = "customerId"),
            @Mapping(target = "accountNumber",  source = "accountNumber"),
            @Mapping(target = "amount",         source = "amount"),
            @Mapping(target = "interestRate",   source = "interestRate"),
            @Mapping(target = "termMonths",     source = "termMonths"),
            @Mapping(target = "startDate",      source = "createdAt"),
            @Mapping(target = "status",         source = "status"),
            @Mapping(target = "createdAt",      source = "createdAt"),
            @Mapping(target = "approvedAt",     source = "approvedAt"),
            @Mapping(target = "infoIncome", source = "infoIncome")
    })
    LoanResponseDTO toResponseDTO(LoanRequestDTO dto);
}
