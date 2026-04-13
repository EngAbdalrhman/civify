CREATE TABLE scheduled_email_batches (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content_template TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT true,
    recipient_emails TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    sent_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_batch_status ON scheduled_email_batches(status, next_retry_at);
