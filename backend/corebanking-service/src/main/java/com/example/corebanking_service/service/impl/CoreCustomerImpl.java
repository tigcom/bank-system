package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.services.customer.CoreCustomerService;
import com.example.corebanking_service.entity.CoreCustomer;
import com.example.corebanking_service.repository.CoreCustomerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
//@DubboService
@RequiredArgsConstructor
@Slf4j
public class CoreCustomerImpl implements CoreCustomerService {

    private final CoreCustomerRepo coreCustomerRepo;

    @Override
    @Transactional
    public CoreResponse syncCoreCustomer(CoreCustomerDTO coreCustomerDTO) {
        try {
            log.info("Đang đồng bộ khách hàng với CIF: {}, Status: {}",
                    coreCustomerDTO.getCifCode(), coreCustomerDTO.getStatus());

            Optional<CoreCustomer> existingCustomer = coreCustomerRepo.findByCifCode(coreCustomerDTO.getCifCode());
            if (existingCustomer.isPresent()) {
                log.info("Cập nhật khách hàng hiện có với CIF: {}", coreCustomerDTO.getCifCode());
                CoreCustomer coreCustomer = existingCustomer.get();
                coreCustomer.setStatus(coreCustomerDTO.getStatus());
                coreCustomerRepo.save(coreCustomer);
                log.info("Đã cập nhật khách hàng thành công với CIF: {}", coreCustomer.getCifCode());
                return new CoreResponse(true, "Cập nhật khách hàng thành công");
            }

            if (coreCustomerDTO.getCifCode() == null || coreCustomerDTO.getCifCode().isEmpty()) {
                log.error("Mã CIF không hợp lệ: {}", coreCustomerDTO.getCifCode());
                return new CoreResponse(false, "Mã CIF không hợp lệ");
            }
            if (coreCustomerDTO.getStatus() == null || coreCustomerDTO.getStatus().isEmpty()) {
                log.error("Trạng thái không hợp lệ: {}", coreCustomerDTO.getStatus());
                return new CoreResponse(false, "Trạng thái không hợp lệ");
            }
            if (coreCustomerRepo.findByCifCode(coreCustomerDTO.getCifCode()).isPresent()) {
                log.error("Mã CIF đã tồn tại: {}", coreCustomerDTO.getCifCode());
                return new CoreResponse(false, "Mã CIF đã tồn tại");
            }

            CoreCustomer coreCustomer = CoreCustomer.builder()
                    .cifCode(coreCustomerDTO.getCifCode())
                    .status(coreCustomerDTO.getStatus())
                    .build();

            log.info("Đang lưu khách hàng vào core_customers với CIF: {}", coreCustomer.getCifCode());
            coreCustomerRepo.save(coreCustomer);
            log.info("Đã lưu khách hàng thành công với CIF: {}", coreCustomer.getCifCode());
            return new CoreResponse(true, "Đồng bộ khách hàng thành công");
        } catch (Exception e) {
            log.error("Đồng bộ khách hàng thất bại với CIF: {}. Lỗi: {}",
                    coreCustomerDTO.getCifCode(), e.getMessage(), e);
            throw new RuntimeException("Đồng bộ khách hàng thất bại: " + e.getMessage(), e);
        }
    }

}
