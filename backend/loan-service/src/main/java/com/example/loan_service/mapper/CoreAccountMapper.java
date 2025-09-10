package com.example.loan_service.mapper;

import com.example.common_service.dto.CoreAccountRequest;
import com.example.loan_service.dto.response.LoanResponseDTO;
import com.example.loan_service.entity.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CoreAccountMapper {
    CoreAccountMapper INSTANCE = Mappers.getMapper(CoreAccountMapper.class);

    @Mappings({
            @Mapping(target = "accountNumber", source = "accountNumber"),
            @Mapping(target = "balance", source = "amount"),
            @Mapping(target = "status", ignore = true), // status sẽ set ở handler
            @Mapping(target = "cifCode", ignore = true), // set ở handler nếu cần
            @Mapping(target = "accountType", ignore = true) // set ở handler nếu cần
    })
    CoreAccountRequest fromLoanResponseDTO(LoanResponseDTO dto);

    @Mappings({
            @Mapping(target = "accountNumber", source = "disbursementAccountNumber"),
            @Mapping(target = "balance", source = "amount"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "cifCode", ignore = true),
            @Mapping(target = "accountType", ignore = true)
    })
    CoreAccountRequest fromLoan(Loan loan);
}
