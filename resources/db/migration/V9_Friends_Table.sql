CREATE TABLE friends (
    id SERIAL PRIMARY KEY,
    id_user_1 INTEGER NOT NULL,
    id_user_2 INTEGER NOT NULL,
    FOREIGN KEY (id_user_1) REFERENCES users(id),
    FOREIGN KEY (id_user_2) REFERENCES users(id)
);