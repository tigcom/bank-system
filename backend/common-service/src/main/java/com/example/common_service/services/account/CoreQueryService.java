package com.example.common_service.services.account;

import java.math.BigDecimal;

public interface CoreQueryService {
    BigDecimal getBalance(String accountNumber);
}
