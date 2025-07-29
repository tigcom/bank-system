package com.example.corebanking_service.service.impl;

import com.example.common_service.constant.AccountStatus;
import com.example.corebanking_service.entity.CoreAccountNumber;
import com.example.corebanking_service.repository.CoreAccountNumberRepo;
import com.example.corebanking_service.service.CoreBankingService;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.common_service.constant.NumberStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CoreBankingServiceImpl implements CoreBankingService {
    @Autowired
    private CoreAccountNumberRepo coreAccountNumberRepo;
    @Override
    public List<CoreAccountNumber> generateAccountNumber(String bankcode) {
        List<CoreAccountNumber> accountNumbers = new ArrayList<>();
        for (CoreAccountNumber can : randomNumber(bankcode,"01")){
            accountNumbers.add(can);
        }
        for (CoreAccountNumber can : randomNumber(bankcode,"02")){
            accountNumbers.add(can);
        }
        for (CoreAccountNumber can : randomNumber(bankcode,"03")){
            accountNumbers.add(can);
        }
        for (CoreAccountNumber can : randomNumber(bankcode,"04")){
            accountNumbers.add(can);
        }
        return accountNumbers;
    }

    @Override
    public String getAccountNumber(String typeAccount) {
        System.out.println(typeAccount);
        String accountTypeCode = null;
        if(typeAccount.equalsIgnoreCase("PAYMENT")){
            accountTypeCode = "01";
        }else if(typeAccount.equalsIgnoreCase("CREDIT")){
            accountTypeCode = "02";
        }else if(typeAccount.equalsIgnoreCase("SAVING")){
            accountTypeCode = "03";
        }else if(typeAccount.equalsIgnoreCase("LOAN")){
            accountTypeCode = "04";
        }
        System.out.println(accountTypeCode);
        return  coreAccountNumberRepo.getAccountNumberByTypeAccount(accountTypeCode).getNumber();
    }

    public List<CoreAccountNumber>  randomNumber(String bankCode, String typeCode){
        List<CoreAccountNumber> accountNumbers = new ArrayList<>();
        List<String> listNumber = new ArrayList<>();
        for( int i = 0; i <= 20; i++){
            String number =null;
            String rd = null;
            do{
                rd = String.format("%04d",new Random().nextInt(1000));
                number = bankCode + typeCode + rd;
            }while (coreAccountNumberRepo.existsByNumber(number));
            listNumber.add(number);
            CoreAccountNumber can = new CoreAccountNumber();
            can.setNumber(number);
            can.setBankCode(bankCode);
            can.setAccountTypeCode(typeCode);
            can.setGeneratedDate(LocalDateTime.now());
            can.setStatus(NumberStatus.AVAILABLE);
            accountNumbers.add(coreAccountNumberRepo.save(can));
        }
        return accountNumbers;
    }
}
