//package com.example.loan_service.test;
//
//import com.example.loan_service.entity.Repayment;
//import com.example.loan_service.service.RepaymentService;
//import com.example.loan_service.repository.RepaymentRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//import java.util.Scanner;
//
//@Component
//@RequiredArgsConstructor
//public class RepaymentTestRunner implements CommandLineRunner {
//
//    private final RepaymentService repaymentService;
//    private final RepaymentRepository repaymentRepository;
//
//    @Override
//    public void run(String... args) {
//        Scanner scanner = new Scanner(System.in);
//
//        while (true) {
//            try {
//                System.out.print("Nhập repaymentId: ");
//                Long repaymentId = Long.parseLong(scanner.nextLine());
//
//                Optional<Repayment> optionalRepayment = repaymentRepository.findById(repaymentId);
//                Repayment repayment = optionalRepayment.get();
//                boolean isLast = repaymentService.checkLastMonthRepayment(repayment);
//
//                System.out.println("Kết quả: " + (isLast ? "Là kỳ trả cuối" : "Không phải kỳ trả cuối"));
//                System.out.println("----------------------------------------");
//
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//        }
//    }
//}
