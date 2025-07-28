package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.corebanking_service.entity.CoreCustomer;
import com.example.corebanking_service.repository.CoreCustomerRepo;
import com.example.corebanking_service.service.CoreCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreCustomerImpl implements CoreCustomerService {

    private final CoreCustomerRepo coreCustomerRepo;

    @Override
    @Transactional
    public CoreResponse syncCoreCustomer(CoreCustomerDTO coreCustomerDTO) {
        String requestId = UUID.randomUUID().toString();
        try {
            log.info("SYNC_CORE_CUSTOMER - RequestId: {}, CifCode: {}, Status: {}", requestId, coreCustomerDTO.getCifCode(), coreCustomerDTO.getStatus());

            Optional<CoreCustomer> existingCustomer = coreCustomerRepo.findByCifCode(coreCustomerDTO.getCifCode());
            if (existingCustomer.isPresent()) {
                log.info("UPDATE_EXISTING_CUSTOMER - RequestId: {}, CifCode: {}", requestId, coreCustomerDTO.getCifCode());
                CoreCustomer coreCustomer = existingCustomer.get();
                coreCustomer.setStatus(coreCustomerDTO.getStatus());
                coreCustomerRepo.save(coreCustomer);
                log.info("UPDATE_CUSTOMER_SUCCESS - RequestId: {}, CifCode: {}", requestId, coreCustomer.getCifCode());
                return new CoreResponse(true, "Customer updated successfully");
            }

            if (coreCustomerDTO.getCifCode() == null || coreCustomerDTO.getCifCode().isEmpty()) {
                log.warn("INVALID_CIF_CODE - RequestId: {}, CifCode: {}", requestId, coreCustomerDTO.getCifCode());
                return new CoreResponse(false, "Invalid CIF code");
            }
            if (coreCustomerDTO.getStatus() == null || coreCustomerDTO.getStatus().isEmpty()) {
                log.warn("INVALID_STATUS - RequestId: {}, Status: {}", requestId, coreCustomerDTO.getStatus());
                return new CoreResponse(false, "Invalid status");
            }
            if (coreCustomerRepo.findByCifCode(coreCustomerDTO.getCifCode()).isPresent()) {
                log.warn("CIF_ALREADY_EXISTS - RequestId: {}, CifCode: {}", requestId, coreCustomerDTO.getCifCode());
                return new CoreResponse(false, "CIF code already exists");
            }

            CoreCustomer coreCustomer = CoreCustomer.builder()
                    .cifCode(coreCustomerDTO.getCifCode())
                    .status(coreCustomerDTO.getStatus())
                    .build();

            log.info("SAVE_NEW_CUSTOMER - RequestId: {}, CifCode: {}", requestId, coreCustomer.getCifCode());
            coreCustomerRepo.save(coreCustomer);
            log.info("SAVE_CUSTOMER_SUCCESS - RequestId: {}, CifCode: {}", requestId, coreCustomer.getCifCode());
            return new CoreResponse(true, "Customer synchronized successfully");
        } catch (Exception e) {
            log.error("SYNC_CORE_CUSTOMER_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, coreCustomerDTO.getCifCode(), e.getMessage(), e);
            throw new RuntimeException("Customer synchronization failed: " + e.getMessage(), e);
        }
    }

}
