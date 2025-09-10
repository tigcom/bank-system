package com.example.account_service.service.impl;

import com.example.account_service.entity.Term;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.TermRepository;
import com.example.account_service.service.TermService;
import com.example.common_service.dto.response.CoreTermDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Override
    public List<CoreTermDTO> getAllActiveTerms() {
        List<Term> activeTerms = termRepository.findAllActiveTermsOrderByMonths();

        return activeTerms.stream()
                .map(this::mapToCoreTermDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Term getTermByMonths(Integer termValueMonths) {
        Term term = termRepository.findByTermValueMonths(termValueMonths);
        if (term == null) {
            throw new AppException(ErrorCode.TERM_NOT_FOUND);
        }
        return term;
    }

    @Override
    public Term createTerm(Term term) {
        return termRepository.save(term);
    }

    @Override
    public Term updateTerm(Long termId, Term term) {
        Term existingTerm = termRepository.findById(termId)
                .orElseThrow(() -> new AppException(ErrorCode.TERM_NOT_FOUND));

        existingTerm.setTermValueMonths(term.getTermValueMonths());
        existingTerm.setInterestRate(term.getInterestRate());
        existingTerm.setIsActive(term.getIsActive());

        return termRepository.save(existingTerm);
    }

    @Override
    public void deleteTerm(Long termId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new AppException(ErrorCode.TERM_NOT_FOUND));

        term.setIsActive(false); // Soft delete
        termRepository.save(term);
    }

    @Override
    public List<Term> getAllTerms() {
        return termRepository.findAll();
    }

    private CoreTermDTO mapToCoreTermDTO(Term term) {
        return CoreTermDTO.builder()
                .termId(term.getTermId())
                .termValueMonths(term.getTermValueMonths())
                .interestRate(term.getInterestRate())
                .isActive(term.getIsActive())
                .build();
    }
}
