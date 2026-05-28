-- =============================================================================
-- Seed Data — Auto Finance Platform
-- Realistic sample data across all domains (shared database)
-- =============================================================================

-- Dealers
INSERT INTO dealers (dealer_code, dealer_name, contact_name, contact_email, region, reserve_rate, holdback_pct, active, tier, ytd_volume) VALUES
('DLR-001', 'Metro Auto Group', 'James Wilson', 'jwilson@metraauto.example.com', 'Northeast', 1.75, 2.00, true, 'PLATINUM', 245),
('DLR-002', 'Sunrise Motors', 'Maria Garcia', 'mgarcia@sunrisemotors.example.com', 'Southeast', 1.50, 2.00, true, 'GOLD', 128),
('DLR-003', 'Pacific Coast Auto', 'David Chen', 'dchen@pacificcoast.example.com', 'West', 1.25, 1.50, true, 'GOLD', 97),
('DLR-004', 'Heartland Vehicles', 'Sarah Johnson', 'sjohnson@heartland.example.com', 'Midwest', 1.50, 2.00, true, 'SILVER', 54),
('DLR-005', 'Legacy Auto Sales', 'Robert Brown', 'rbrown@legacyauto.example.com', 'South', 1.00, 1.50, false, 'SILVER', 12);

-- Loan Applications
INSERT INTO loan_applications (application_number, applicant_name, applicant_ssn, credit_score, vehicle_vin, vehicle_year, vehicle_make, vehicle_model, requested_amount, approved_amount, interest_rate, term_months, monthly_payment, status, dealer_id, application_date, approval_date, funding_date) VALUES
('LN-A1B2C3D4', 'John Smith', '***-**-1234', 745, '1HGCM82633A004352', 2023, 'Honda', 'Accord', 28500.00, 28500.00, 3.99, 60, 525.89, 'ACTIVE', 1, '2024-01-15 10:30:00', '2024-01-15 10:35:00', '2024-01-20 14:00:00'),
('LN-E5F6G7H8', 'Jane Doe', '***-**-5678', 698, '2T1BURHE6JC123456', 2022, 'Toyota', 'Camry', 24000.00, 24000.00, 5.49, 72, 392.45, 'ACTIVE', 1, '2024-02-01 09:00:00', '2024-02-01 09:10:00', '2024-02-05 11:00:00'),
('LN-I9J0K1L2', 'Michael Johnson', '***-**-9012', 635, '3VWDP7AJ5DM123456', 2021, 'Volkswagen', 'Jetta', 18500.00, 15725.00, 8.99, 60, 326.12, 'ACTIVE', 2, '2024-03-10 14:15:00', '2024-03-10 14:25:00', '2024-03-15 09:30:00'),
('LN-M3N4O5P6', 'Emily Williams', '***-**-3456', 710, 'WBA3B1C51EK123456', 2024, 'BMW', '3 Series', 42000.00, 42000.00, 5.49, 72, 685.17, 'FUNDED', 3, '2024-04-20 11:00:00', '2024-04-20 11:15:00', NULL),
('LN-Q7R8S9T0', 'Robert Davis', '***-**-7890', 520, 'KNAGM4AD4G5123456', 2020, 'Kia', 'Optima', 16000.00, NULL, NULL, 60, NULL, 'DECLINED', 4, '2024-05-05 16:30:00', NULL, NULL),
('LN-U1V2W3X4', 'Lisa Martinez', '***-**-2345', 690, '5NPE34AF6HH123456', 2023, 'Hyundai', 'Sonata', 26000.00, 26000.00, 5.49, 60, 497.87, 'ACTIVE', 2, '2024-06-12 08:45:00', '2024-06-12 08:55:00', '2024-06-18 10:00:00'),
('LN-Y5Z6A7B8', 'Thomas Anderson', '***-**-6789', 755, '1G1YY22G965123456', 2024, 'Chevrolet', 'Corvette', 65000.00, 65000.00, 3.99, 72, 1016.23, 'ACTIVE', 1, '2024-07-01 13:20:00', '2024-07-01 13:25:00', '2024-07-08 15:00:00'),
('LN-C9D0E1F2', 'Sandra Wilson', '***-**-0123', 575, 'JM1BK32F781234567', 2019, 'Mazda', '3', 14000.00, 9800.00, 14.99, 48, 275.63, 'DELINQUENT', 4, '2024-01-25 10:00:00', '2024-01-25 10:20:00', '2024-02-01 09:00:00');

-- Accounts
INSERT INTO accounts (account_number, loan_id, customer_name, customer_email, customer_phone, mailing_address, original_balance, current_balance, payoff_amount, days_past_due, status, next_due_date, maturity_date, last_payment_date) VALUES
('ACCT-1A2B3C4D', 1, 'John Smith', 'jsmith@email.example.com', '555-0101', '123 Main St, Springfield, IL 62701', 28500.00, 26450.00, 27102.50, 0, 'CURRENT', '2025-07-01', '2029-01-15', '2025-06-01 09:00:00'),
('ACCT-5E6F7G8H', 2, 'Jane Doe', 'jdoe@email.example.com', '555-0102', '456 Oak Ave, Portland, OR 97201', 24000.00, 22100.00, 22650.00, 0, 'CURRENT', '2025-07-01', '2030-02-01', '2025-06-01 10:00:00'),
('ACCT-9I0J1K2L', 3, 'Michael Johnson', 'mjohnson@email.example.com', '555-0103', '789 Pine Rd, Austin, TX 78701', 15725.00, 14200.00, 14750.00, 0, 'CURRENT', '2025-07-10', '2029-03-10', '2025-06-10 11:00:00'),
('ACCT-3M4N5O6P', 4, 'Emily Williams', 'ewilliams@email.example.com', '555-0104', '321 Elm St, Denver, CO 80201', 42000.00, 42000.00, 43500.00, 0, 'CURRENT', '2025-07-20', '2030-04-20', NULL),
('ACCT-7Q8R9S0T', 6, 'Lisa Martinez', 'lmartinez@email.example.com', '555-0106', '654 Birch Ln, Miami, FL 33101', 26000.00, 24500.00, 25100.00, 0, 'CURRENT', '2025-07-12', '2029-06-12', '2025-06-12 14:00:00'),
('ACCT-1U2V3W4X', 7, 'Thomas Anderson', 'tanderson@email.example.com', '555-0107', '987 Cedar Dr, Chicago, IL 60601', 65000.00, 62000.00, 63500.00, 0, 'CURRENT', '2025-08-01', '2030-07-01', '2025-07-01 08:00:00'),
('ACCT-5Y6Z7A8B', 8, 'Sandra Wilson', 'swilson@email.example.com', '555-0108', '246 Maple Ct, Detroit, MI 48201', 9800.00, 8900.00, 9200.00, 45, 'DELINQUENT_30', '2025-05-25', '2028-01-25', '2025-04-25 15:00:00');

-- Payments
INSERT INTO payments (loan_id, payment_amount, principal_amount, interest_amount, fee_amount, late_fee, payment_method, status, confirmation_number, ach_routing_number, ach_account_number, payment_date, processed_date, due_date) VALUES
(1, 525.89, 430.12, 95.77, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-A1B2C3D4', '021000021', '****4352', '2025-05-01 09:00:00', '2025-05-01 09:05:00', '2025-05-01'),
(1, 525.89, 431.55, 94.34, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-E5F6G7H8', '021000021', '****4352', '2025-06-01 09:00:00', '2025-06-01 09:05:00', '2025-06-01'),
(2, 392.45, 282.45, 110.00, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-I9J0K1L2', '021000021', '****3456', '2025-05-01 10:00:00', '2025-05-01 10:05:00', '2025-05-01'),
(2, 392.45, 283.74, 108.71, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-M3N4O5P6', '021000021', '****3456', '2025-06-01 10:00:00', '2025-06-01 10:05:00', '2025-06-01'),
(3, 326.12, 208.12, 118.00, 0.00, 0.00, 'CHECK', 'COMPLETED', 'PMT-Q7R8S9T0', NULL, NULL, '2025-05-10 11:00:00', '2025-05-12 14:00:00', '2025-05-10'),
(3, 326.12, 209.68, 116.44, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-U1V2W3X4', '021000021', '****6789', '2025-06-10 11:00:00', '2025-06-10 11:05:00', '2025-06-10'),
(6, 497.87, 378.87, 119.00, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-Y5Z6A7B8', '021000021', '****2345', '2025-06-12 14:00:00', '2025-06-12 14:05:00', '2025-06-12'),
(7, 1016.23, 800.23, 216.00, 0.00, 0.00, 'WIRE', 'COMPLETED', 'PMT-C9D0E1F2', NULL, NULL, '2025-07-01 08:00:00', '2025-07-01 08:10:00', '2025-07-01'),
(8, 275.63, 153.13, 122.50, 0.00, 0.00, 'ACH', 'COMPLETED', 'PMT-G3H4I5J6', '021000021', '****0123', '2025-04-25 15:00:00', '2025-04-25 15:05:00', '2025-04-25'),
(8, 0.00, 0.00, 0.00, 0.00, 25.00, 'ACH', 'PENDING', 'FEE-K7L8M9N0', NULL, NULL, '2025-06-10 00:00:00', NULL, '2025-05-25');

-- Deal Packages
INSERT INTO deal_packages (deal_number, dealer_id, loan_id, vehicle_vin, sale_price, down_payment, trade_in_value, dealer_reserve, holdback_amount, status, submission_date, settlement_date) VALUES
('DL-A1B2C3D4', 1, 1, '1HGCM82633A004352', 32000.00, 3500.00, 0.00, 498.75, 570.00, 'SETTLED', '2024-01-14 09:00:00', '2024-02-15 10:00:00'),
('DL-E5F6G7H8', 1, 2, '2T1BURHE6JC123456', 27500.00, 3500.00, 0.00, 420.00, 480.00, 'SETTLED', '2024-01-31 08:30:00', '2024-03-01 09:00:00'),
('DL-I9J0K1L2', 2, 3, '3VWDP7AJ5DM123456', 22000.00, 3500.00, 0.00, 235.88, 314.50, 'FUNDED', '2024-03-09 14:00:00', NULL),
('DL-M3N4O5P6', 3, 4, 'WBA3B1C51EK123456', 48000.00, 6000.00, 0.00, 735.00, 840.00, 'APPROVED', '2024-04-19 10:30:00', NULL);
