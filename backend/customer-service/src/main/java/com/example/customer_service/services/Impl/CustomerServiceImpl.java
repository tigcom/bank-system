package com.example.customer_service.services.Impl;

import com.example.customer_service.dtos.*;
import com.example.customer_service.models.Customer;
import com.example.customer_service.models.CustomerStatus;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.services.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final KycService kycService;

    @Override
    public RegisterResponse register(RegisterCustomerDTO request) {
        if (!validateRequest(request)) {
            return new RegisterResponse(false, "Dữ liệu yêu cầu không hợp lệ");
        }

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            return new RegisterResponse(false, "Email đã tồn tại");
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setAddress(request.getAddress());
        customer.setIdentityNumber(request.getIdentityNumber());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setKycStatus("verified");
        customer.setKycVerifiedAt(LocalDateTime.now());
        customer.setCifCode("TEMP_" + UUID.randomUUID().toString());

        customerRepository.save(customer);

        String cif = generateCifCode(customer.getCustomerId());
        customer.setCifCode(cif);
        customerRepository.save(customer);



        KycResponse kycResponse = kycService.verifyIdentity(
                request.getIdentityNumber(),
                request.getFullName()
        );

        if (!kycResponse.isVerified()) {
            return new RegisterResponse(false, "Xác minh KYC thất bại: " + kycResponse.getMessage());
        }

        customer.setKycResponse(kycResponse.getDetails());

        return new RegisterResponse(true, "Đăng ký thành công");
    }


    @Override
    public LoginResponse login(LoginCustomerDTO request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            return new LoginResponse();
        }

        Customer customer = customerOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            return new LoginResponse();
        }

        String token = "jwt-token-" + customer.getEmail();
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return response;
    }

    @Override
    public RegisterResponse forgotPassword(String email) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        if (customerOpt.isEmpty()) {
            return new RegisterResponse(false, "Không tìm thấy email");
        }

        Customer customer = customerOpt.get();
        String resetToken = UUID.randomUUID().toString();
        customer.setResetToken(resetToken);
        customer.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        customerRepository.save(customer);

        return new RegisterResponse(true, "Đã gửi liên kết đặt lại mật khẩu");
    }

    @Override
    public CustomerListResponse getCustomerList() {
        List<CustomerResponse> customers = customerRepository.findAll()
                .stream()
                .map(this::toCustomerResponse)
                .collect(Collectors.toList());
        CustomerListResponse response = new CustomerListResponse();
        response.setCustomers(customers);
        return response;
    }

    @Override
    public CustomerResponse getCustomerDetail(String cifCode) {
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        return customerOpt.map(this::toCustomerResponse).orElse(null);
    }

    @Override
    public RegisterResponse updateCustomer(UpdateCustomerDTO request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new RegisterResponse(false, "Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getPhoneNumber() != null) {
            if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent() &&
                    !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                return new RegisterResponse(false, "Số điện thoại đã tồn tại");
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        customerRepository.save(customer);
        return new RegisterResponse(true, "Cập nhật thành công");
    }

    @Override
    public RegisterResponse updateCustomerStatus(UpdateStatusRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new RegisterResponse(false, "Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == null) {
            return new RegisterResponse(false, "Trạng thái không hợp lệ");
        }

        if (customer.getStatus() == CustomerStatus.CLOSED) {
            return new RegisterResponse(false, "Không thể thay đổi trạng thái từ CLOSED");
        }

        if (!customer.getStatus().equals(newStatus)) {
            customer.setStatus(newStatus);
            customerRepository.save(customer);
        }
        return new RegisterResponse(true, "Cập nhật trạng thái thành công");
    }

    @Override
    public KycResponse verifyKyc(KycRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
        if (customerOpt.isEmpty()) {
            return new KycResponse(false, "Không tìm thấy khách hàng", null);
        }

        Customer customer = customerOpt.get();

        // Gọi service xác minh danh tính
        KycResponse kycResponse = kycService.verifyIdentity(
                request.getIdentityNumber(),
                request.getFullName()
        );

        if (kycResponse.isVerified()) {
            customer.setKycStatus("verified");
            customer.setKycVerifiedAt(LocalDateTime.now());
        } else {
            customer.setKycStatus("rejected");
        }

        customer.setKycResponse(kycResponse.getDetails());
        customerRepository.save(customer);

        return kycResponse;
    }


    private boolean validateRequest(RegisterCustomerDTO request) {
        return request.getFullName() != null &&
                request.getAddress() != null &&
                request.getIdentityNumber() != null &&
                request.getEmail() != null &&
                request.getPhoneNumber() != null &&
                request.getPassword() != null;
    }

    private String generateCifCode(Long id) {
        return String.format("CIF%08d", id);
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getCustomerId());
        response.setCifCode(customer.getCifCode());
        response.setFullName(customer.getFullName());
        response.setAddress(customer.getAddress());
        response.setEmail(customer.getEmail());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setStatus(customer.getStatus());
        response.setKycStatus(customer.getKycStatus());
        return response;
    }
}