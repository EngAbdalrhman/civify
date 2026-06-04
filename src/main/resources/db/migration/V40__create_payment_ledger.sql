CREATE TABLE payment_ledger (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(36) NOT NULL,
    order_id        VARCHAR(255),
    user_id         BIGINT,
    gateway         VARCHAR(50),
    status          VARCHAR(30) NOT NULL,
    gateway_status  VARCHAR(100),
    source          VARCHAR(50),
    note            VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_ledger_txn ON payment_ledger(transaction_id);
CREATE INDEX idx_payment_ledger_order ON payment_ledger(order_id);
CREATE INDEX idx_payment_ledger_status ON payment_ledger(status);
CREATE INDEX idx_payment_ledger_created ON payment_ledger(created_at);
