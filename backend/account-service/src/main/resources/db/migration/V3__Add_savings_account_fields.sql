-- Migration to add new fields to savings_accounts table
-- V3__Add_savings_account_fields.sql

-- Add new columns to savings_accounts table
ALTER TABLE savings_accounts 
ADD COLUMN maturity_date TIMESTAMP,
ADD COLUMN interest_payment_type VARCHAR(20),
ADD COLUMN renew_option VARCHAR(20);

-- Update existing records with default values and calculate maturity_date
UPDATE savings_accounts sa
SET 
    maturity_date = sa.created_date + INTERVAL t.term_value_months MONTH,
    interest_payment_type = 'AT_MATURITY',
    renew_option = 'NO_RENEW'
FROM terms t 
WHERE sa.term_id = t.term_id;

-- Make the new columns NOT NULL after setting default values
ALTER TABLE savings_accounts 
MODIFY COLUMN maturity_date TIMESTAMP NOT NULL,
MODIFY COLUMN interest_payment_type VARCHAR(20) NOT NULL,
MODIFY COLUMN renew_option VARCHAR(20) NOT NULL; 