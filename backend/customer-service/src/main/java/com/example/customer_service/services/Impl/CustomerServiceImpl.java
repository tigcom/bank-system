package com.example.customer_service.services.impl;

import com.example.customer_service.dtos.*;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
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

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (!validateRequest(request)) {
            return new RegisterResponse(false, "Invalid request data");
        }

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            return new RegisterResponse(false, "Email already exists");
        }

        Customer customer = new Customer();
        customer.setCifCode(generateCifCode());
        customer.setFullName(request.getFullName());
        customer.setAddress(request.getAddress());
        customer.setIdentityNumber(request.getIdentityNumber());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setStatus("active");
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setKycStatus("pending");

        customerRepository.save(customer);
        return new RegisterResponse(true, "Registered successfully");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        if (customerOpt.isEmpty()) {
            return new LoginResponse(null);
        }

        Customer customer = customerOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            return new LoginResponse(null);
        }

        String token = "jwt-token-" + customer.getEmail();
        return new LoginResponse(token);
    }

    @Override
    public ForgotPasswordResponse forgotPassword(String email) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        if (customerOpt.isEmpty()) {
            return new ForgotPasswordResponse(false, "Email not found");
        }

        Customer customer = customerOpt.get();
        String resetToken = UUID.randomUUID().toString();
        customer.setResetToken(resetToken);
        customer.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        customerRepository.save(customer);

        return new ForgotPasswordResponse(true, "Reset link sent");
    }

    @Override
    public List<CustomerDTO> getCustomerList() {
        return customerRepository.findAll()
                .stream()
                .map(this::toCustomerDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO getCustomerDetail(String cifCode) {
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        return customerOpt.map(this::toCustomerDTO).orElse(null);
    }

    @Override
    public UpdateCustomerResponse updateCustomer(UpdateCustomerRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new UpdateCustomerResponse(false, "Customer not found");
        }

        Customer customer = customerOpt.get();
        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getPhoneNumber() != null) {
            if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent() &&
                    !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                return new UpdateCustomerResponse(false, "Phone number already exists");
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        customerRepository.save(customer);
        return new UpdateCustomerResponse(true, "Updated successfully");
    }

    @Override
    public UpdateStatusResponse updateCustomerStatus(UpdateStatusRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new UpdateStatusResponse(false, "Customer not found");
        }

        Customer customer = customerOpt.get();
        if (!customer.getStatus().equals(request.getStatus())) {
            customer.setStatus(request.getStatus());
            customerRepository.save(customer);
        }
        return new UpdateStatusResponse(true, "Status updated");
    }

    private boolean validateRequest(RegisterRequest request) {
        return request.getFullName() != null &&
                request.getAddress() != null &&
                request.getIdentityNumber() != null &&
                request.getEmail() != null &&
                request.getPhoneNumber() != null &&
                request.getPassword() != null;
    }

    private String generateCifCode() {
        return "CIF" + System.currentTimeMillis();
    }

    private CustomerDTO toCustomerDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getCustomerId());
        dto.setCifCode(customer.getCifCode());
        dto.setFullName(customer.getFullName());
        dto.setAddress(customer.getAddress());
        dto.setEmail(customer.getEmail());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setStatus(customer.getStatus());
        dto.setKycStatus(customer.getKycStatus());
        return dto;
    }
}