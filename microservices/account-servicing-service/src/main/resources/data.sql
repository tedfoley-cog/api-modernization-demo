-- Account-servicing seed data ONLY. This service owns its own H2 schema; it does
-- not seed or read loan/payment/dealer tables. Loan facts (interest rate, term)
-- are projected onto accounts from LoanFunded events, and balances/fees are driven
-- by payment events at runtime. These rows exist so the read endpoints return data.

INSERT INTO accounts
    (account_number, loan_id, customer_name, customer_email, customer_phone, mailing_address,
     original_balance, current_balance, payoff_amount, interest_rate, term_months,
     outstanding_fees, days_past_due, status, next_due_date, maturity_date, last_payment_date)
VALUES
    ('ACCT-SEED0001', 1001, 'Jordan Rivera', 'jordan.rivera@example.com', '555-0101', '100 Main St, Detroit MI',
     20000.00, 18500.00, 21000.00, 6.000, 60, 0.00, 0, 'CURRENT',
     DATEADD('MONTH', 1, CURRENT_DATE()), DATEADD('MONTH', 60, CURRENT_DATE()), CURRENT_TIMESTAMP()),
    ('ACCT-SEED0002', 1002, 'Casey Nguyen', 'casey.nguyen@example.com', '555-0102', '200 Oak Ave, Dearborn MI',
     15000.00, 9800.00, 16200.00, 8.990, 48, 50.00, 45, 'DELINQUENT_30',
     DATEADD('MONTH', -1, CURRENT_DATE()), DATEADD('MONTH', 47, CURRENT_DATE()), CURRENT_TIMESTAMP());
