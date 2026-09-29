CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_accounts_username UNIQUE (username)
);

CREATE TABLE account_roles (
    account_id UUID NOT NULL REFERENCES accounts (id),
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (account_id, role)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    address VARCHAR(100),
    is_active BOOLEAN NOT NULL,
    age INTEGER NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6),
    CONSTRAINT uk_users_name UNIQUE (name),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_logs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    log_message VARCHAR(255),
    user_id UUID NOT NULL REFERENCES users (id)
);

CREATE INDEX idx_user_logs_user_id ON user_logs (user_id);
