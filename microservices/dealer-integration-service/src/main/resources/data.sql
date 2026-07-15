-- Dealer-integration seed data ONLY. This service owns its own H2 schema; it does
-- not seed or read loan/account/payment tables. Loan facts arrive via events into
-- the local deal_loan_projection table at runtime. The deal_packages.loan_id column
-- is a plain correlation value, not a foreign key into another service's database.

INSERT INTO dealers
    (dealer_code, dealer_name, contact_name, contact_email, region, reserve_rate, holdback_pct, active, tier, ytd_volume)
VALUES
    ('DLR-001', 'Metro Auto Group', 'James Wilson', 'jwilson@metraauto.example.com', 'Northeast', 1.75, 2.00, true, 'PLATINUM', 245),
    ('DLR-002', 'Sunrise Motors', 'Maria Garcia', 'mgarcia@sunrisemotors.example.com', 'Southeast', 1.50, 2.00, true, 'GOLD', 128),
    ('DLR-003', 'Pacific Coast Auto', 'David Chen', 'dchen@pacificcoast.example.com', 'West', 1.25, 1.50, true, 'GOLD', 97),
    ('DLR-004', 'Heartland Vehicles', 'Sarah Johnson', 'sjohnson@heartland.example.com', 'Midwest', 1.50, 2.00, true, 'SILVER', 54),
    ('DLR-005', 'Legacy Auto Sales', 'Robert Brown', 'rbrown@legacyauto.example.com', 'South', 1.00, 1.50, false, 'SILVER', 12);

INSERT INTO deal_packages
    (deal_number, dealer_id, loan_id, vehicle_vin, sale_price, down_payment, trade_in_value,
     dealer_reserve, holdback_amount, status, submission_date, settlement_date)
VALUES
    ('DL-A1B2C3D4', 1, 1, '1HGCM82633A004352', 32000.00, 3500.00, 0.00, 498.75, 570.00, 'SETTLED', '2024-01-14 09:00:00', '2024-02-15 10:00:00'),
    ('DL-E5F6G7H8', 1, 2, '2T1BURHE6JC123456', 27500.00, 3500.00, 0.00, 420.00, 480.00, 'SETTLED', '2024-01-31 08:30:00', '2024-03-01 09:00:00'),
    ('DL-I9J0K1L2', 2, 3, '3VWDP7AJ5DM123456', 22000.00, 3500.00, 0.00, 235.88, 314.50, 'FUNDED', '2024-03-09 14:00:00', NULL),
    ('DL-M3N4O5P6', 3, 4, 'WBA3B1C51EK123456', 48000.00, 6000.00, 0.00, 735.00, 840.00, 'APPROVED', '2024-04-19 10:30:00', NULL);

INSERT INTO deal_loan_projection
    (application_id, application_number, dealer_id, vehicle_vin, loan_id, loan_status, approved_amount)
VALUES
    (1, 'LN-A1B2C3D4', 1, '1HGCM82633A004352', 1, 'ACTIVE', 28500.00),
    (2, 'LN-E5F6G7H8', 1, '2T1BURHE6JC123456', 2, 'ACTIVE', 24000.00),
    (3, 'LN-I9J0K1L2', 2, '3VWDP7AJ5DM123456', 3, 'FUNDED', 15725.00),
    (4, 'LN-M3N4O5P6', 3, 'WBA3B1C51EK123456', 4, 'APPROVED', 42000.00);
