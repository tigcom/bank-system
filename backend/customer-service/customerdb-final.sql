CREATE DATABASE customerdb;
CREATE DATABASE transaction_service;
CREATE DATABASE accountdb;
USE customerdb;

CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    cif_code VARCHAR(20) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
    identity_number VARCHAR(20) UNIQUE NOT NULL,
    address TEXT NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(15) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE kyc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    identity_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('male', 'female', 'other')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    verified_at DATETIME,
    verified_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_kyc_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Chỉ số phục vụ truy vấn nhanh
CREATE INDEX idx_customers_cif_code ON customers(cif_code);
CREATE INDEX idx_customers_identity_number ON customers(identity_number);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone_number ON customers(phone_number);
CREATE INDEX idx_customers_user_id ON customers(user_id);


INSERT INTO customers (
    user_id, cif_code, username , full_name, date_of_birth, gender, identity_number, 
    address, email, phone_number, status
) VALUES (
    'c6a123bc-456d-78ef-90gh-ijklmnopqrst', 'CIF001', "nguyenvana" , 'Nguyen Van A', '1990-01-15', 
    'male', '123456789', '123 Le Loi, Hanoi', 'a.nguyen@example.com', '0901234567', 'ACTIVE'
);

INSERT INTO kyc (
    customer_id,
    identity_number,
    full_name,
    date_of_birth,
    gender,
    status,
    verified_at,
    verified_by
) VALUES (
    1,
    '123456789',
    'Nguyen Van A',
    '1990-01-15',
    'male',
    'VERIFIED',
    '2025-05-20 14:41:00',
    'admin_user'
);

select * from customers;
select * from kyc;
SELECT customer_id FROM customers WHERE user_id = 'c6a123bc-456d-78ef-90gh-ijklmnopqrst';