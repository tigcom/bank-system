package com.example.loan_service.service.impl;

import com.example.loan_service.entity.InfoIncome;
import com.example.loan_service.repository.InfoIncomeRepository;
import com.example.loan_service.service.InfoIncomeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfoIncomeServiceImpl implements InfoIncomeService {

    private final InfoIncomeRepository repo;

    @Override
    public InfoIncome createInfoIncome(InfoIncome infoIncome) {
        log.info("CREATE_INFO_INCOME_START - loanId: {}", infoIncome.getLoan().getLoanId());
        try {
            InfoIncome saved = repo.save(infoIncome);
            log.info("CREATE_INFO_INCOME_SUCCESS - infoId: {}", saved.getInfoId());
            return saved;
        } catch (Exception e) {
            log.error("CREATE_INFO_INCOME_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public InfoIncome updateInfoIncome(InfoIncome infoIncome) {
        log.info("UPDATE_INFO_INCOME_START - infoId: {}", infoIncome.getInfoId());
        if (!repo.existsById(infoIncome.getInfoId())) {
            throw new EntityNotFoundException("InfoIncome not found: " + infoIncome.getInfoId());
        }
        try {
            InfoIncome updated = repo.save(infoIncome);
            log.info("UPDATE_INFO_INCOME_SUCCESS - infoId: {}", updated.getInfoId());
            return updated;
        } catch (Exception e) {
            log.error("UPDATE_INFO_INCOME_ERROR - infoId: {}, error: {}", infoIncome.getInfoId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<InfoIncome> getById(Long infoId) {
        log.info("GET_INFO_INCOME_BY_ID - infoId: {}", infoId);
        return repo.findById(infoId);
    }

    @Override
    public List<InfoIncome> getByLoanId(Long loanId) {
        log.info("GET_INFO_INCOME_BY_LOAN_ID - loanId: {}", loanId);
        return repo.findAllByLoan_LoanId(loanId);
    }

    @Override
    public void deleteInfoIncome(Long infoId) {
        log.info("DELETE_INFO_INCOME_START - infoId: {}", infoId);
        InfoIncome info = repo.findById(infoId)
                .orElseThrow(() -> new EntityNotFoundException("InfoIncome not found: " + infoId));
        try {
            log.info("incomeINFO : {}", info.getDeclaredIncome());
            repo.deleteById(info.getInfoId());
            log.info("DELETE_INFO_INCOME_SUCCESS - infoId: {}", infoId);
        } catch (Exception e) {
            log.error("DELETE_INFO_INCOME_ERROR - infoId: {}, error: {}", infoId, e.getMessage(), e);
            throw e;
        }
    }
}
