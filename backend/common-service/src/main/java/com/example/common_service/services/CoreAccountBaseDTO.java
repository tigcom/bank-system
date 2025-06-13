package com.example.common_service.services;

import com.example.common_service.constant.AccountType;

public interface CoreAccountBaseDTO {
    String getCifCode();
    AccountType getAccountType();
}
