-- Beloppen blir heltal i öre. Ett flyttal kan inte hålla ett öre exakt, och summan av raderna
-- kunde därför skilja sig från fakturans belopp. Kronor räknas fram först när något visas.
--
-- Raderna som redan finns ligger i kronor med decimaler. De räknas om en gång, här.

CREATE TABLE service_items_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    price INTEGER,
    estimated_minutes INTEGER,
    specialization TEXT);

INSERT INTO service_items_ny (id, name, description, price, estimated_minutes, specialization)
SELECT id, name, description, CAST(ROUND(price * 100) AS INTEGER), estimated_minutes,
       specialization
FROM service_items;

DROP TABLE service_items;

ALTER TABLE service_items_ny RENAME TO service_items;

-- Priset som frös på arbetet när det skapades.

CREATE TABLE work_order_service_items_ny (
    work_order_id INTEGER NOT NULL REFERENCES work_orders (id) ON DELETE CASCADE,
    service_item_id INTEGER NOT NULL REFERENCES service_items (id) ON DELETE RESTRICT,
    completed INTEGER NOT NULL DEFAULT 0,
    price INTEGER,
    package_name TEXT,
    PRIMARY KEY (work_order_id, service_item_id));

INSERT INTO work_order_service_items_ny (work_order_id, service_item_id, completed, price,
                                         package_name)
SELECT work_order_id, service_item_id, completed, CAST(ROUND(price * 100) AS INTEGER),
       package_name
FROM work_order_service_items;

DROP TABLE work_order_service_items;

ALTER TABLE work_order_service_items_ny RENAME TO work_order_service_items;

-- Fakturan och raderna håller samma enhet, annars summerar de inte.

CREATE TABLE invoices_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    work_order_id INTEGER REFERENCES work_orders (id) ON DELETE RESTRICT,
    invoice_date TEXT,
    amount INTEGER,
    discount INTEGER,
    total_amount INTEGER,
    paid INTEGER DEFAULT 0);

INSERT INTO invoices_ny (id, work_order_id, invoice_date, amount, discount, total_amount, paid)
SELECT id, work_order_id, invoice_date, CAST(ROUND(amount * 100) AS INTEGER),
       CAST(ROUND(discount * 100) AS INTEGER), CAST(ROUND(total_amount * 100) AS INTEGER), paid
FROM invoices;

DROP TABLE invoices;

ALTER TABLE invoices_ny RENAME TO invoices;

-- Fakturaraden bär sitt eget pris och sin egen rabatt.

CREATE TABLE invoice_lines_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    service_item_id INTEGER REFERENCES service_items (id) ON DELETE SET NULL,
    service_name TEXT NOT NULL,
    price INTEGER NOT NULL,
    discount INTEGER DEFAULT 0,
    package_name TEXT);

INSERT INTO invoice_lines_ny (id, invoice_id, service_item_id, service_name, price, discount,
                              package_name)
SELECT id, invoice_id, service_item_id, service_name, CAST(ROUND(price * 100) AS INTEGER),
       CAST(ROUND(discount * 100) AS INTEGER), package_name
FROM invoice_lines;

DROP TABLE invoice_lines;

ALTER TABLE invoice_lines_ny RENAME TO invoice_lines;

-- Betalningen jämförs med fakturans belopp, alltså samma enhet där också.

CREATE TABLE payments_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER REFERENCES invoices (id) ON DELETE RESTRICT,
    amount INTEGER,
    payment_type TEXT,
    payment_date TEXT,
    successful INTEGER DEFAULT 0);

INSERT INTO payments_ny (id, invoice_id, amount, payment_type, payment_date, successful)
SELECT id, invoice_id, CAST(ROUND(amount * 100) AS INTEGER), payment_type, payment_date,
       successful
FROM payments;

DROP TABLE payments;

ALTER TABLE payments_ny RENAME TO payments;
