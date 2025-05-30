package com.example.account_service.utils;

import com.example.account_service.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class AccountNumberUtils {
    public String generateAccountNumber(Account dto) {
        String cif = dto.getCifCode();
        int typeCode;
        if (dto.getAccountType().name().equals("PAYMENT")) {
            typeCode = 0;
        } else if (dto.getAccountType().name().equals("CREDIT")) {
            typeCode = 1;
        } else {
            typeCode = 2;
        }
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return cif + typeCode + randomPart;

    }

}
