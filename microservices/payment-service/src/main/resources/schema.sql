-- =============================================================================
-- Payment Microservice Schema
-- Owned exclusively by the Payment bounded context
-- =============================================================================

CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id         BIGINT NOT NULL,
    payment_amount  DECIMAL(10, 2) NOT NULL,
    principal_amount DECIMAL(10, 2),
    interest_amount  DECIMAL(10, 2),
    fee_amount       DECIMAL(10, 2),
    late_fee         DECIMAL(10, 2),
    payment_method   VARCHAR(30),
    status           VARCHAR(20),
    confirmation_number VARCHAR(50),
    ach_routing_number  VARCHAR(9),
    ach_account_number  VARCHAR(20),
    payment_date     TIMESTAMP,
    processed_date   TIMESTAMP,
    due_date         DATE
);

-- CQRS read model: materialized payment summary per loan
CREATE TABLE payment_summary (
    loan_id                BIGINT PRIMARY KEY,
    total_paid             DECIMAL(12, 2) DEFAULT 0,
    total_principal_paid   DECIMAL(12, 2) DEFAULT 0,
    total_interest_paid    DECIMAL(12, 2) DEFAULT 0,
    total_fees_paid        DECIMAL(12, 2) DEFAULT 0,
    outstanding_late_fees  DECIMAL(12, 2) DEFAULT 0,
    payment_count          INT DEFAULT 0,
    last_payment_date      TIMESTAMP,
    last_payment_amount    DECIMAL(10, 2)
);
