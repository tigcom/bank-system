package com.example.loan_service.mapper;

import com.example.loan_service.entity.Repayment;
import com.example.loan_service.dto.response.RepaymentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RepaymentMapper {
    RepaymentMapper INSTANCE = Mappers.getMapper(RepaymentMapper.class);
    RepaymentResponseDTO toDTO(Repayment entity);
}