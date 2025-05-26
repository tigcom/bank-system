package com.example.account_service.service;

import com.example.common_service.constant.AccountType;

public interface BaseAccountCreateDTO {
    String getCifCode();
    com.example.common_service.constant.AccountType getAccountType();
}
