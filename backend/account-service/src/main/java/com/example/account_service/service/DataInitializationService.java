package com.example.account_service.service;//package com.example.account_service.service;
//
//import com.example.account_service.entity.CreditCardType;
//import com.example.account_service.entity.Term;
//import com.example.account_service.repository.CreditCardTypeRepository;
//import com.example.account_service.repository.TermRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//import java.util.List;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class DataInitializationService implements CommandLineRunner {
//
//    private final TermRepository termRepository;
//    private final CreditCardTypeRepository creditCardTypeRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        initializeTerms();
//        initializeCreditCardTypes();
//    }
//
//    private void initializeTerms() {
//        if (termRepository.count() == 0) {
//            log.info("Initializing Terms data...");
//
//            List<Term> terms = Arrays.asList(
//                Term.builder()
//                    .termValueMonths(0)
//                    .interestRate(new BigDecimal("1.500"))
//                    .isActive(true)
//                    .build(),
//                Term.builder()
//                    .termValueMonths(3)
//                    .interestRate(new BigDecimal("3.000"))
//                    .isActive(true)
//                    .build(),
//                Term.builder()
//                    .termValueMonths(6)
//                    .interestRate(new BigDecimal("4.500"))
//                    .isActive(true)
//                    .build(),
//                Term.builder()
//                    .termValueMonths(12)
//                    .interestRate(new BigDecimal("6.000"))
//                    .isActive(true)
//                    .build(),
//                Term.builder()
//                    .termValueMonths(24)
//                    .interestRate(new BigDecimal("7.500"))
//                    .isActive(true)
//                    .build()
//            );
//
//            termRepository.saveAll(terms);
//            log.info("Terms data initialized successfully!");
//        }
//    }
//
//    private void initializeCreditCardTypes() {
//        if (creditCardTypeRepository.count() == 0) {
//            log.info("Initializing Credit Card Types data...");
//
//            List<CreditCardType> creditCardTypes = Arrays.asList(
//                CreditCardType.builder()
//                    .typeName("VIB Classic")
//                    .defaultCreditLimit(new BigDecimal("20000000"))
//                    .interestRate(new BigDecimal("2.500"))
//                    .annualFee(new BigDecimal("200000"))
//                    .minimumIncome(new BigDecimal("8000000"))
//                    .imageUrl("https://example.com/vib-classic.jpg")
//                    .description("Thẻ tín dụng cơ bản với nhiều ưu đãi")
//                    .build(),
//                CreditCardType.builder()
//                    .typeName("VIB Gold")
//                    .defaultCreditLimit(new BigDecimal("50000000"))
//                    .interestRate(new BigDecimal("2.200"))
//                    .annualFee(new BigDecimal("500000"))
//                    .minimumIncome(new BigDecimal("15000000"))
//                    .imageUrl("https://example.com/vib-gold.jpg")
//                    .description("Thẻ tín dụng cao cấp với nhiều đặc quyền")
//                    .build(),
//                CreditCardType.builder()
//                    .typeName("VIB Platinum")
//                    .defaultCreditLimit(new BigDecimal("100000000"))
//                    .interestRate(new BigDecimal("2.000"))
//                    .annualFee(new BigDecimal("1000000"))
//                    .minimumIncome(new BigDecimal("30000000"))
//                    .imageUrl("https://example.com/vib-platinum.jpg")
//                    .description("Thẻ tín dụng hạng sang với đặc quyền VIP")
//                    .build()
//            );
//
//            creditCardTypeRepository.saveAll(creditCardTypes);
//            log.info("Credit Card Types data initialized successfully!");
//        }
//    }
//}