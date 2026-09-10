CREATE SEQUENCE payments_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE payments (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    amount NUMERIC(38, 2),
    payment_status VARCHAR(255) NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    CONSTRAINT payments_pkey PRIMARY KEY (id),
    CONSTRAINT uk_payments_order_id UNIQUE (order_id)
);

ALTER TABLE payments
    ADD CONSTRAINT payments_payment_status_check
    CHECK (
        payment_status IN (
            'PAYMENT_SUCCEEDED',
            'PAYMENT_FAILED',
            'REFUNDED'
            )
        );

ALTER TABLE payments
    ADD CONSTRAINT payments_payment_method_check
    CHECK (
        payment_method IN (
            'CARD',
            'CASH'
        )
    );