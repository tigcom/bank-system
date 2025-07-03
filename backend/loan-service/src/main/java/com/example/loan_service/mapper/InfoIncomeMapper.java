// InfoIncomeMapper.java
package com.example.loan_service.mapper;



import com.example.loan_service.dto.request.InfoIncomeRequestDto;
import com.example.loan_service.dto.response.InfoIncomeResponseDto;
import com.example.loan_service.entity.InfoIncome;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InfoIncomeMapper {

    @Mapping(target = "infoId", ignore = true)
    @Mapping(target = "loan", ignore = true)
    InfoIncome toEntity(InfoIncomeRequestDto dto);

    @Mapping(target = "infoId", source = "infoId")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "declaredIncome", source = "declaredIncome")
    InfoIncomeResponseDto toDto(InfoIncome entity);
}
