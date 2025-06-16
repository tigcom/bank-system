-- Initialize Terms data
INSERT INTO terms (term_value_months, interest_rate, is_active) VALUES
(0, 1.500, true),
(3, 3.000, true),
(6, 4.500, true),
(12, 6.000, true),
(24, 7.500, true)
ON CONFLICT (term_value_months) DO NOTHING;

-- Initialize Credit Card Types data
INSERT INTO credit_card_types (card_type_id, type_name, default_credit_limit, interest_rate, annual_fee, minimum_income, image_url, description) VALUES
('cc1', 'VIB Classic', 20000000, 2.500, 200000, 8000000, 'https://example.com/vib-classic.jpg', 'Thẻ tín dụng cơ bản với nhiều ưu đãi'),
('cc2', 'VIB Gold', 50000000, 2.200, 500000, 15000000, 'https://example.com/vib-gold.jpg', 'Thẻ tín dụng cao cấp với nhiều đặc quyền'),
('cc3', 'VIB Platinum', 100000000, 2.000, 1000000, 30000000, 'https://example.com/vib-platinum.jpg', 'Thẻ tín dụng hạng sang với đặc quyền VIP')
ON CONFLICT (type_name) DO NOTHING; 