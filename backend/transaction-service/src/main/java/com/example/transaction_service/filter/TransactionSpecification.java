package com.example.transaction_service.filter;

import com.example.transaction_service.dto.request.TransactionFilterRequest;
import com.example.transaction_service.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {
    public static Specification<Transaction> filter(TransactionFilterRequest request){
       return ((root, query, cb) -> {
           List<Predicate> predicates = new ArrayList<>();

//           Mệnh đề Where filter sql
           if (request.getAccountNumber() != null) {
               Predicate from = cb.equal(root.get("fromAccountNumber"), request.getAccountNumber());
               Predicate to = cb.equal(root.get("toAccountNumber"), request.getAccountNumber());
               predicates.add(cb.or(from, to));
           }

//           Mệnh đề end filter sql
           if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
               Predicate descriptionLike = cb.like(cb.lower(root.get("description")), "%" + request.getKeyword().toLowerCase() + "%");
               Predicate typeLike = cb.like(cb.lower(root.get("type").as(String.class)), "%" + request.getKeyword().toLowerCase() + "%");
               predicates.add(cb.or(descriptionLike, typeLike));
           }
           if (request.getType() != null) {
               predicates.add(cb.equal(root.get("type"), request.getType()));
           }

           if (request.getStatus() != null) {
               predicates.add(cb.equal(root.get("status"), request.getStatus()));
           }

           if (request.getCurrency() != null) {
               predicates.add(cb.equal(root.get("currency"), request.getCurrency()));
           }

           if (request.getBankType() != null) {
               predicates.add(cb.equal(root.get("bankType"), request.getBankType()));
           }


           if (request.getStartDate() != null && request.getEndDate() != null) {
               predicates.add(cb.between(root.get("timestamp"), request.getStartDate(), request.getEndDate()));
           }
           if (request.getFromAmount() != null && request.getToAmount() != null) {
               predicates.add(cb.between(root.get("amount"), request.getFromAmount(), request.getToAmount()));
           } else if (request.getFromAmount() != null) {
               predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), request.getFromAmount()));
           } else if (request.getToAmount() != null) {
               predicates.add(cb.lessThanOrEqualTo(root.get("amount"), request.getToAmount()));
           }
           return cb.and(predicates.toArray(new Predicate[0]));
       });
    }
}
