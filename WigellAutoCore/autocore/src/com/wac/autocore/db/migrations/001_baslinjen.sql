-- Schemat så som det såg ut innan nycklarna kom på.
-- Den här filen skapas för en ny bas. En bas som redan har tabellerna får
-- versionen kvitterad utan att filen körs, för schemat är detsamma.

CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    vip INTEGER DEFAULT 0);

CREATE TABLE IF NOT EXISTS vehicles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    registration_number TEXT NOT NULL,
    brand TEXT,
    model TEXT,
    year INTEGER,
    customer_id INTEGER);

CREATE TABLE IF NOT EXISTS mechanics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    specialization TEXT,
    available INTEGER DEFAULT 1);

CREATE TABLE IF NOT EXISTS service_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    price REAL,
    estimated_minutes INTEGER,
    specialization TEXT);

CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id INTEGER,
    start_time TEXT,
    end_time TEXT,
    mechanic_id INTEGER,
    service_item_id INTEGER,
    date TEXT,
    description TEXT,
    status TEXT);

CREATE TABLE IF NOT EXISTS work_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER,
    mechanic_id INTEGER,
    status TEXT,
    type TEXT,
    vehicle_id INTEGER,
    description TEXT,
    original_work_order_id INTEGER,
    planned_date TEXT,
    planned_time TEXT,
    customer_instructions TEXT,
    other_comments TEXT);

CREATE TABLE IF NOT EXISTS booking_service_items (
    booking_id INTEGER NOT NULL,
    service_item_id INTEGER NOT NULL,
    package_name TEXT,
    PRIMARY KEY (booking_id, service_item_id));

CREATE TABLE IF NOT EXISTS service_packages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT);

CREATE TABLE IF NOT EXISTS service_package_items (
    package_id INTEGER NOT NULL,
    service_item_id INTEGER NOT NULL,
    PRIMARY KEY (package_id, service_item_id));

CREATE TABLE IF NOT EXISTS booking_mechanics (
    booking_id INTEGER NOT NULL,
    mechanic_id INTEGER NOT NULL,
    PRIMARY KEY (booking_id, mechanic_id));

CREATE TABLE IF NOT EXISTS work_order_service_items (
    work_order_id INTEGER NOT NULL,
    service_item_id INTEGER NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    price REAL,
    package_name TEXT,
    PRIMARY KEY (work_order_id, service_item_id));

CREATE TABLE IF NOT EXISTS invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    work_order_id INTEGER,
    invoice_date TEXT,
    amount REAL,
    discount REAL,
    total_amount REAL,
    paid INTEGER DEFAULT 0);

CREATE TABLE IF NOT EXISTS invoice_lines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    service_item_id INTEGER,
    service_name TEXT NOT NULL,
    price REAL NOT NULL,
    discount REAL DEFAULT 0,
    package_name TEXT);

CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER,
    amount REAL,
    payment_type TEXT,
    payment_date TEXT,
    successful INTEGER DEFAULT 0);

-- Språkvalet sparas här så att det överlever en omstart.
CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT);

-- Rättningar som en äldre bas redan har gått igenom. De står här för att en ny bas ska
-- hamna i samma läge, och var och en tål att köras om.

UPDATE work_orders SET type = 'STANDARD' WHERE type IS NULL OR type = '';

UPDATE work_orders SET type = 'RECLAMATION' WHERE type = 'WARRANTY';

UPDATE work_orders
SET vehicle_id = (SELECT b.vehicle_id FROM bookings b WHERE b.id = work_orders.booking_id)
WHERE (vehicle_id IS NULL OR vehicle_id = 0)
  AND booking_id > 0
  AND (SELECT b.vehicle_id FROM bookings b WHERE b.id = work_orders.booking_id) > 0;

UPDATE service_items SET specialization = 'seed.mechanic.brakes.specialization'
WHERE name = 'seed.service.brake_service.name' AND (specialization IS NULL OR specialization = '');

UPDATE service_items SET specialization = 'seed.mechanic.diagnostics.specialization'
WHERE name = 'seed.service.diagnostics.name' AND (specialization IS NULL OR specialization = '');

INSERT OR IGNORE INTO booking_service_items (booking_id, service_item_id)
SELECT id, service_item_id FROM bookings
WHERE service_item_id IS NOT NULL AND service_item_id > 0;

INSERT OR IGNORE INTO booking_mechanics (booking_id, mechanic_id)
SELECT id, mechanic_id FROM bookings
WHERE mechanic_id IS NOT NULL AND mechanic_id > 0;

INSERT INTO invoice_lines (invoice_id, service_item_id, service_name, price, discount)
SELECT i.id, s.id, s.name, CASE WHEN w.price IS NOT NULL THEN w.price ELSE s.price END, 0
FROM invoices i
JOIN work_order_service_items w ON w.work_order_id = i.work_order_id
JOIN service_items s ON s.id = w.service_item_id
WHERE NOT EXISTS (SELECT 1 FROM invoice_lines l WHERE l.invoice_id = i.id);
