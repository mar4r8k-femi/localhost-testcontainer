CREATE TABLE users (
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    email    VARCHAR(150) NOT NULL,
    active   BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO users (username, email, active) VALUES
    ('alice', 'alice@example.com', TRUE),
    ('bob',   'bob@example.com',   TRUE),
    ('carol', 'carol@example.com', TRUE);
