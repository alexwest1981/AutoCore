-- Nycklar i schemat, så att en rad inte kan peka på något som inte finns.
--
-- SQLite kan inte sätta en nyckel på en tabell som redan finns, så varje tabell som
-- bär en referens byggs om: skapa ny, kopiera rader, släpp den gamla, döp om.
-- Köraren har nycklarna avstängda under tiden och kontrollerar varje rad efteråt.
--
-- Delarna följer med sin förälder. Historiken — arbetsorder, fakturor, betalningar —
-- får inte försvinna, så den nekar i stället för att kaskadera.

-- Först bort rader som redan pekar fel. Noll betydde "ingen" i de här kolumnerna.

UPDATE vehicles SET customer_id = NULL
WHERE customer_id = 0
   OR (customer_id IS NOT NULL AND customer_id NOT IN (SELECT id FROM customers));

UPDATE bookings SET vehicle_id = NULL
WHERE vehicle_id = 0
   OR (vehicle_id IS NOT NULL AND vehicle_id NOT IN (SELECT id FROM vehicles));

UPDATE bookings SET mechanic_id = NULL
WHERE mechanic_id = 0
   OR (mechanic_id IS NOT NULL AND mechanic_id NOT IN (SELECT id FROM mechanics));

UPDATE bookings SET service_item_id = NULL
WHERE service_item_id = 0
   OR (service_item_id IS NOT NULL AND service_item_id NOT IN (SELECT id FROM service_items));

UPDATE work_orders SET booking_id = NULL
WHERE booking_id = 0
   OR (booking_id IS NOT NULL AND booking_id NOT IN (SELECT id FROM bookings));

UPDATE work_orders SET mechanic_id = NULL
WHERE mechanic_id = 0
   OR (mechanic_id IS NOT NULL AND mechanic_id NOT IN (SELECT id FROM mechanics));

UPDATE work_orders SET vehicle_id = NULL
WHERE vehicle_id = 0
   OR (vehicle_id IS NOT NULL AND vehicle_id NOT IN (SELECT id FROM vehicles));

UPDATE work_orders SET original_work_order_id = NULL
WHERE original_work_order_id = 0
   OR (original_work_order_id IS NOT NULL
       AND original_work_order_id NOT IN (SELECT id FROM work_orders));

UPDATE invoices SET work_order_id = NULL
WHERE work_order_id = 0
   OR (work_order_id IS NOT NULL AND work_order_id NOT IN (SELECT id FROM work_orders));

UPDATE invoice_lines SET service_item_id = NULL
WHERE service_item_id = 0
   OR (service_item_id IS NOT NULL AND service_item_id NOT IN (SELECT id FROM service_items));

UPDATE payments SET invoice_id = NULL
WHERE invoice_id = 0
   OR (invoice_id IS NOT NULL AND invoice_id NOT IN (SELECT id FROM invoices));

-- Kopplingstabellerna kan inte ha tomma nycklar, så en rad utan förälder försvinner.
-- Den går inte att nå ändå.

DELETE FROM booking_service_items
WHERE booking_id NOT IN (SELECT id FROM bookings)
   OR service_item_id NOT IN (SELECT id FROM service_items);

DELETE FROM booking_mechanics
WHERE booking_id NOT IN (SELECT id FROM bookings)
   OR mechanic_id NOT IN (SELECT id FROM mechanics);

DELETE FROM service_package_items
WHERE package_id NOT IN (SELECT id FROM service_packages)
   OR service_item_id NOT IN (SELECT id FROM service_items);

DELETE FROM work_order_service_items
WHERE work_order_id NOT IN (SELECT id FROM work_orders)
   OR service_item_id NOT IN (SELECT id FROM service_items);

DELETE FROM invoice_lines WHERE invoice_id NOT IN (SELECT id FROM invoices);

-- vehicles pekar på sin ägare.

CREATE TABLE vehicles_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    registration_number TEXT NOT NULL,
    brand TEXT,
    model TEXT,
    year INTEGER,
    customer_id INTEGER REFERENCES customers (id) ON DELETE RESTRICT);

INSERT INTO vehicles_ny (id, registration_number, brand, model, year, customer_id)
SELECT id, registration_number, brand, model, year, customer_id FROM vehicles;

DROP TABLE vehicles;

ALTER TABLE vehicles_ny RENAME TO vehicles;

-- bookings pekar på fordon, mekaniker och tjänst.

CREATE TABLE bookings_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id INTEGER REFERENCES vehicles (id) ON DELETE RESTRICT,
    start_time TEXT,
    end_time TEXT,
    mechanic_id INTEGER REFERENCES mechanics (id) ON DELETE RESTRICT,
    service_item_id INTEGER REFERENCES service_items (id) ON DELETE RESTRICT,
    date TEXT,
    description TEXT,
    status TEXT);

INSERT INTO bookings_ny (id, vehicle_id, start_time, end_time, mechanic_id, service_item_id,
                         date, description, status)
SELECT id, vehicle_id, start_time, end_time, mechanic_id, service_item_id,
       date, description, status
FROM bookings;

DROP TABLE bookings;

ALTER TABLE bookings_ny RENAME TO bookings;

-- work_orders pekar på bokningen den kom ur, och på fordon och mekaniker.

CREATE TABLE work_orders_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER REFERENCES bookings (id) ON DELETE RESTRICT,
    mechanic_id INTEGER REFERENCES mechanics (id) ON DELETE RESTRICT,
    status TEXT,
    type TEXT,
    vehicle_id INTEGER REFERENCES vehicles (id) ON DELETE RESTRICT,
    description TEXT,
    original_work_order_id INTEGER REFERENCES work_orders (id) ON DELETE RESTRICT,
    planned_date TEXT,
    planned_time TEXT,
    customer_instructions TEXT,
    other_comments TEXT);

INSERT INTO work_orders_ny (id, booking_id, mechanic_id, status, type, vehicle_id, description,
                            original_work_order_id, planned_date, planned_time,
                            customer_instructions, other_comments)
SELECT id, booking_id, mechanic_id, status, type, vehicle_id, description,
       original_work_order_id, planned_date, planned_time,
       customer_instructions, other_comments
FROM work_orders;

DROP TABLE work_orders;

ALTER TABLE work_orders_ny RENAME TO work_orders;

-- Kopplingstabellerna hänger på sin bokning, arbetsorder eller paket.

CREATE TABLE booking_service_items_ny (
    booking_id INTEGER NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    service_item_id INTEGER NOT NULL REFERENCES service_items (id) ON DELETE CASCADE,
    package_name TEXT,
    PRIMARY KEY (booking_id, service_item_id));

INSERT INTO booking_service_items_ny (booking_id, service_item_id, package_name)
SELECT booking_id, service_item_id, package_name FROM booking_service_items;

DROP TABLE booking_service_items;

ALTER TABLE booking_service_items_ny RENAME TO booking_service_items;

CREATE TABLE booking_mechanics_ny (
    booking_id INTEGER NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    mechanic_id INTEGER NOT NULL REFERENCES mechanics (id) ON DELETE CASCADE,
    PRIMARY KEY (booking_id, mechanic_id));

INSERT INTO booking_mechanics_ny (booking_id, mechanic_id)
SELECT booking_id, mechanic_id FROM booking_mechanics;

DROP TABLE booking_mechanics;

ALTER TABLE booking_mechanics_ny RENAME TO booking_mechanics;

CREATE TABLE work_order_service_items_ny (
    work_order_id INTEGER NOT NULL REFERENCES work_orders (id) ON DELETE CASCADE,
    service_item_id INTEGER NOT NULL REFERENCES service_items (id) ON DELETE RESTRICT,
    completed INTEGER NOT NULL DEFAULT 0,
    price REAL,
    package_name TEXT,
    PRIMARY KEY (work_order_id, service_item_id));

INSERT INTO work_order_service_items_ny (work_order_id, service_item_id, completed, price,
                                         package_name)
SELECT work_order_id, service_item_id, completed, price, package_name
FROM work_order_service_items;

DROP TABLE work_order_service_items;

ALTER TABLE work_order_service_items_ny RENAME TO work_order_service_items;

CREATE TABLE service_package_items_ny (
    package_id INTEGER NOT NULL REFERENCES service_packages (id) ON DELETE CASCADE,
    service_item_id INTEGER NOT NULL REFERENCES service_items (id) ON DELETE CASCADE,
    PRIMARY KEY (package_id, service_item_id));

INSERT INTO service_package_items_ny (package_id, service_item_id)
SELECT package_id, service_item_id FROM service_package_items;

DROP TABLE service_package_items;

ALTER TABLE service_package_items_ny RENAME TO service_package_items;

-- invoices pekar på arbetet den fakturerar.

CREATE TABLE invoices_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    work_order_id INTEGER REFERENCES work_orders (id) ON DELETE RESTRICT,
    invoice_date TEXT,
    invoice_number TEXT,
    credit_of INTEGER,
    credit_reason TEXT,
    amount REAL,
    discount REAL,
    total_amount REAL,
    paid INTEGER DEFAULT 0);

INSERT INTO invoices_ny (id, work_order_id, invoice_date, invoice_number, credit_of, credit_reason,
                         amount, discount, total_amount, paid)
SELECT id, work_order_id, invoice_date, invoice_number, credit_of, credit_reason,
       amount, discount, total_amount, paid
FROM invoices;

DROP TABLE invoices;

ALTER TABLE invoices_ny RENAME TO invoices;

-- Fakturaraden är ett fryst minne av vad som gjordes: namnet och priset ligger på raden,
-- så kopplingen till tjänsten får bli tom när tjänsten försvinner.

CREATE TABLE invoice_lines_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    service_item_id INTEGER REFERENCES service_items (id) ON DELETE SET NULL,
    service_name TEXT NOT NULL,
    price REAL NOT NULL,
    discount REAL DEFAULT 0,
    package_name TEXT);

INSERT INTO invoice_lines_ny (id, invoice_id, service_item_id, service_name, price, discount,
                              package_name)
SELECT id, invoice_id, service_item_id, service_name, price, discount, package_name
FROM invoice_lines;

DROP TABLE invoice_lines;

ALTER TABLE invoice_lines_ny RENAME TO invoice_lines;

-- payments pekar på fakturan den hör till.

CREATE TABLE payments_ny (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER REFERENCES invoices (id) ON DELETE RESTRICT,
    amount REAL,
    payment_type TEXT,
    payment_date TEXT,
    successful INTEGER DEFAULT 0);

INSERT INTO payments_ny (id, invoice_id, amount, payment_type, payment_date, successful)
SELECT id, invoice_id, amount, payment_type, payment_date, successful FROM payments;

DROP TABLE payments;

ALTER TABLE payments_ny RENAME TO payments;
