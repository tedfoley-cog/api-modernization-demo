-- Loan-origination seed data ONLY. This service owns its own H2 schema; it does
-- not seed or read account/payment/dealer tables. These rows exist so the read
-- endpoints return data on a fresh start.

INSERT INTO loan_applications
    (application_number, applicant_name, applicant_ssn, credit_score, vehicle_vin,
     vehicle_year, vehicle_make, vehicle_model, requested_amount, approved_amount,
     interest_rate, term_months, monthly_payment, status, dealer_id,
     application_date, approval_date, funding_date)
VALUES
    ('LN-SEED0001', 'Jordan Rivera', '***-**-1234', 742, '1FTFW1E50NFA10001',
     2022, 'Ford', 'F-150', 30000.00, 30000.00, 3.99, 72, 469.29, 'APPROVED', 1,
     CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), NULL),
    ('LN-SEED0002', 'Casey Nguyen', '***-**-5678', 655, '3FA6P0H70NR100002',
     2021, 'Ford', 'Fusion', 18000.00, 15300.00, 8.99, 60, 317.49, 'APPROVED', 2,
     CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), NULL),
    ('LN-SEED0003', 'Morgan Patel', '***-**-9012', 540, '1FMCU9GD5NUA10003',
     2020, 'Ford', 'Escape', 22000.00, NULL, NULL, NULL, NULL, 'DECLINED', NULL,
     CURRENT_TIMESTAMP(), NULL, NULL);
