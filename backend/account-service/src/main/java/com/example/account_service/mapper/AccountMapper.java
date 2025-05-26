package com.example.account_service.mapper;

import com.example.account_service.dto.request.CreditCreateDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.entity.Account;
import com.example.account_service.dto.response.AccountCreateReponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
  //  @Mapping(target = "cifCode", source = "cifCode")
 //   @Mapping(target = "accountType", source = "accountType")
   // @Mapping(target = "status", source = "status")
    Account toEntityFromPayment(PaymentCreateDTO dto);
////
//    Account toEntityFromSaving(SavingCreateDTO dto);
////
//    Account toEntityFromCredit(CreditCreateDTO dto);
    AccountCreateReponse toAccountCreateResponse(Account account);
}
