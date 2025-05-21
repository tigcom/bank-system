create database customerdb;
use customerdb;

CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cif_code VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
    identity_number VARCHAR(20) UNIQUE NOT NULL,
    address TEXT NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(15) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'closed')),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (kyc_status IN ('pending', 'verified', 'rejected')),
    kyc_response JSON,
    kyc_verified_at DATETIME,
    password_hash VARCHAR(255) NOT NULL,
    reset_token VARCHAR(100),
    reset_token_expiry DATETIME,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE INDEX idx_customers_cif_code ON customers(cif_code);
CREATE INDEX idx_customers_identity_number ON customers(identity_number);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone_number ON customers(phone_number);

INSERT INTO customers (
    cif_code, full_name, date_of_birth, gender, identity_number, address, email, phone_number, 
    status, kyc_status, kyc_response, kyc_verified_at, password_hash
) VALUES (
    'CIF001', 'Nguyen Van A', '1990-01-15', 'male', '123456789', '123 Le Loi, Hanoi', 
    'a.nguyen@example.com', '0901234567', 'active', 'verified', 
    '{"verified": true, "score": 0.95, "details": "Identity matched"}', 
    '2025-05-20 14:41:00', 
    '$2a$10$your.bcrypt.encoded.password'
);

SELECT * FROM customers WHERE email = 'a.nguyen@example.com';