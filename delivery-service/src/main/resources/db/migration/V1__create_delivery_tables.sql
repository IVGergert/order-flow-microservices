CREATE SEQUENCE couriers_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE couriers (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    transport_type VARCHAR(255),
    courier_status VARCHAR(255),
    CONSTRAINT couriers_pkey PRIMARY KEY (id),
    CONSTRAINT uk_couriers_user_id UNIQUE (user_id)
);

ALTER TABLE couriers
    ADD CONSTRAINT couriers_transport_type_check
    CHECK (
        transport_type IN (
            'BICYCLE',
            'CAR',
            'ON_FOOT'
        )
    );

ALTER TABLE couriers
    ADD CONSTRAINT couriers_courier_status_check
    CHECK (
        courier_status IN (
            'OFFLINE',
            'AVAILABLE',
            'ON_THE_WAY_TO_RESTAURANT',
            'ON_THE_WAY_TO_CUSTOMER'
        )
    );


CREATE SEQUENCE deliveries_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE deliveries (
    id BIGINT NOT NULL,
    delivery_status VARCHAR(255),
    order_id BIGINT,
    courier_id BIGINT,
    address VARCHAR(255),
    eta_minutes INTEGER,
    completed_at TIMESTAMP(6),
    CONSTRAINT deliveries_pkey PRIMARY KEY (id),
    CONSTRAINT uk_deliveries_order_id UNIQUE (order_id)
);

ALTER TABLE deliveries
    ADD CONSTRAINT deliveries_delivery_status_check
    CHECK (
        delivery_status IN (
            'WAITING_FOR_COURIER',
            'COURIER_ASSIGNED',
            'PICKED_UP',
            'DELIVERED'
        )
    );

ALTER TABLE deliveries
    ADD CONSTRAINT fk_deliveries_courier
    FOREIGN KEY (courier_id)
    REFERENCES couriers(id);

CREATE INDEX idx_delivery_status
    ON deliveries(delivery_status);