CREATE SEQUENCE menu_items_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE menu_items (
        id BIGINT NOT NULL,
        name VARCHAR(255) NOT NULL,
        price NUMERIC(19, 2) NOT NULL,
        description VARCHAR(255),
        image_url VARCHAR(255),
        category VARCHAR(255) NOT NULL,
        CONSTRAINT menu_items_pkey PRIMARY KEY (id)
);

ALTER TABLE menu_items
    ADD CONSTRAINT menu_items_category_check
    CHECK (
        category IN (
            'PIZZA',
            'BURGERS',
            'SUSHI',
            'SNACKS',
            'DRINKS'
        )
    );


CREATE SEQUENCE orders_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE orders (
    id BIGINT NOT NULL,
    customer_id BIGINT,
    address VARCHAR(255),
    total_amount NUMERIC(19, 2),
    order_status VARCHAR(255) NOT NULL,
    courier_name VARCHAR(255),
    eta_minutes INTEGER,
    CONSTRAINT orders_pkey PRIMARY KEY (id)
);

ALTER TABLE orders
    ADD CONSTRAINT orders_order_status_check
    CHECK (
        order_status IN (
            'PENDING_PAYMENT',
            'PAYMENT_FAILED',
            'CASH_ON_DELIVERY',
            'PAID',
            'DELIVERY_ASSIGNED',
            'IN_DELIVERY',
            'DELIVERED',
            'CANCELLED'
        )
    );


CREATE SEQUENCE order_items_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE order_items (
    id BIGINT NOT NULL,
    order_id BIGINT,
    item_id BIGINT,
    item_name VARCHAR(255),
    price_at_purchase NUMERIC(38, 2),
    quantity INTEGER,
    CONSTRAINT order_items_pkey PRIMARY KEY (id)
);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id)
    REFERENCES orders(id);