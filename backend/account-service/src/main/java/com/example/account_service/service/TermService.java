package com.example.account_service.service;

import com.example.account_service.entity.Term;
import com.example.common_service.dto.response.CoreTermDTO;

import java.util.List;

public interface TermService {
    
    List<CoreTermDTO> getAllActiveTerms();
    
    Term getTermByMonths(Integer termValueMonths);
    
    Term createTerm(Term term);
    
    Term updateTerm(Long termId, Term term);
    
    void deleteTerm(Long termId);
    
    List<Term> getAllTerms();
}
