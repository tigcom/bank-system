package com.example.loan_service.mapper;

import com.example.loan_service.entity.Notification;
import com.example.loan_service.dto.response.NotificationResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);
    NotificationResponseDTO toDTO(Notification entity);
}
