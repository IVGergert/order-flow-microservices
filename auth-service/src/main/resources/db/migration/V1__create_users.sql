CREATE SEQUENCE users_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT NOT NULL,
    email VARCHAR(255),
    hash_password VARCHAR(255),
    role VARCHAR(255),
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (
        role IN (
            'ROLE_CUSTOMER',
            'ROLE_COURIER'
        )
    );