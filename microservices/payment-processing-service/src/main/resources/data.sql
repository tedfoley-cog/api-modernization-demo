-- Payment-processing seed data ONLY. This service owns its own H2 schema; it does
-- not seed or read loan/account/dealer tables. Loan and account facts arrive via
-- events into the local projection tables at runtime.

INSERT INTO payments
    (loan_id, payment_amount, principal_amount, interest_amount, fee_amount, late_fee,
     payment_method, status, confirmation_number, payment_date, processed_date)
VALUES
    (1001, 450.00, 375.10, 74.90, 0.00, NULL, 'ACH', 'COMPLETED', 'PMT-SEED0001',
     CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (1001, 450.00, 380.25, 69.75, 0.00, NULL, 'ACH', 'PENDING', 'PMT-SEED0002',
     CURRENT_TIMESTAMP(), NULL),
    (1002, 610.00, 520.00, 90.00, 0.00, NULL, 'EFT', 'PENDING', 'PMT-SEED0003',
     CURRENT_TIMESTAMP(), NULL),
    (1002, 0.00, NULL, NULL, NULL, 25.00, NULL, 'PENDING', 'FEE-SEED0001',
     CURRENT_TIMESTAMP(), NULL);
