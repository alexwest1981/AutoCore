-- Fakturan får ett nummer i en obruten serie per år, och en kreditfaktura pekar på den faktura
-- den krediterar. Numret sätts när fakturan skrivs och ändras aldrig efteråt: en rättelse sker med
-- en kreditfaktura, inte genom att numret eller beloppet skrivs över.
--
-- Fakturorna som redan finns hade bara sitt id. De får ett nummer av årtalet och id:t, en gång.
-- Det ger en luckfri serie bakåt också, eftersom id:t är löpande.

UPDATE invoices
SET invoice_number = COALESCE(NULLIF(substr(invoice_date, 1, 4), ''), strftime('%Y', 'now'))
        || '-' || printf('%04d', id)
WHERE invoice_number IS NULL OR invoice_number = '';

-- Två fakturor kan inte ha samma nummer. Gamla rader utan nummer får vara tomma: en unik nyckel
-- tillåter flera NULL, men inte två likadana nummer.
CREATE UNIQUE INDEX IF NOT EXISTS invoices_number ON invoices (invoice_number);
