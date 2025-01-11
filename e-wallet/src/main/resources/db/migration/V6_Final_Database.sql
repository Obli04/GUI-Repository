DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    second_name VARCHAR(255),   
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(255),
    token_expiry TIMESTAMP,
    two_factor_secret VARCHAR(255),
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    balance DOUBLE PRECISION DEFAULT 0.0,
    budget DOUBLE PRECISION DEFAULT 0.0,
    piggy_bank DOUBLE PRECISION DEFAULT 0.0,
    iban VARCHAR(34),
    variable_symbol VARCHAR(10) UNIQUE
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    id_sender INTEGER, --- NULL if depositing through API, otherwise this MUST be inserted.
    name_of_sender VARCHAR(255), --- If id is NULL then this MUST be inserted otherwise NULL.
    id_receiver INTEGER NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    type VARCHAR(50) NOT NULL, -- e.g., Withdraw, Deposit, Send, Receive
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(100), -- e.g., Payments, Utilities, Food, Travel, Shopping, Deposit
    FOREIGN KEY (id_sender) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (id_receiver) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE budgets (
    id SERIAL PRIMARY KEY,
    id_user INTEGER,
    budget DOUBLE PRECISION DEFAULT 0.0,
    budget_spent DOUBLE PRECISION DEFAULT 0.0,
    budget_category VARCHAR(100),
    FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE request_money (
    id SERIAL PRIMARY KEY,
    id_sender INTEGER NOT NULL,
    id_receiver INTEGER NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    description VARCHAR(255) DEFAULT 'Request for money',
    FOREIGN KEY (id_sender) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (id_receiver) REFERENCES users(id) ON DELETE CASCADE
);