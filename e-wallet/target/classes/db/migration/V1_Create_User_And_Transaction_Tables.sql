CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    second_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL,
    piggy_bank DOUBLE PRECISION NOT NULL,
    balance DOUBLE PRECISION NOT NULL,
    budget DOUBLE PRECISION NOT NULL
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    id_sender INTEGER NOT NULL,
    id_receiver INTEGER NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    type VARCHAR(50) NOT NULL, -- e.g., Withdraw, Deposit, Send, Receive
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(100), -- e.g., Payments, Utilities, Food, Travel, Shopping
    FOREIGN KEY (id_sender) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (id_receiver) REFERENCES users(id) ON DELETE CASCADE
);