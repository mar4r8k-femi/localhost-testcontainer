CREATE TABLE products (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    sku   VARCHAR(50)    NOT NULL UNIQUE,
    name  VARCHAR(100)   NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT            NOT NULL DEFAULT 0
);

INSERT INTO products (sku, name, price, stock) VALUES
    ('SKU-001', 'Widget A',  9.99, 100),
    ('SKU-002', 'Gadget B', 19.99,  50),
    ('SKU-003', 'Thing C',   4.99, 200);
