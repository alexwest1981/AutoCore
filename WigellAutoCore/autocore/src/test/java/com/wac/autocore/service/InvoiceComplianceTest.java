package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.repository.InvoiceLineRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.SqliteRepositoryFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

/**
 * The invoice as bookkeeping law wants it: a number from an unbroken series, amounts that are not
 * rewritten once the invoice has been issued, a VAT that gives back exactly what it took when the
 * invoice is credited, and a credit note that points at what it takes back.
 *
 * The rules are checked against the database and not against the window, because the window is the
 * part that changes.
 */
class InvoiceComplianceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    private File file;

    private Db db;

    private InvoiceRepository invoices;

    private GarageSystem garage;

    @BeforeEach
    void openADatabase() throws Exception {
        file = File.createTempFile("faktura-", ".db");
        file.deleteOnExit();
        file.delete();

        db = new Db(file.getAbsolutePath());
        db.ensureReady();
        invoices = new InvoiceRepository(db, new InvoiceLineRepository(db));
        garage = new GarageSystem(new SqliteRepositoryFactory(db));
    }

    @AfterEach
    void closeTheDatabase() {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    @Test
    void theNumberComesFromTheYearsSeries() {

        Invoice first = issued(DAY, 10_000);
        Invoice second = issued(DAY, 5_000);

        assertEquals("2026-0001", first.getNumber());
        assertEquals("2026-0002", second.getNumber());
        assertNotEquals(first.getNumber(), second.getNumber());
    }

    @Test
    void aNewYearStartsItsOwnSeries() {

        assertEquals("2026-0001", issued(DAY, 1_000).getNumber());
        assertEquals("2027-0001", issued(LocalDate.of(2027, 1, 4), 1_000).getNumber());
        assertEquals("2027-0002", issued(LocalDate.of(2027, 1, 5), 1_000).getNumber());
    }

    @Test
    void anIssuedInvoiceKeepsItsAmountsAndOnlyThePaidFlagMoves() {

        Invoice issued = issued(DAY, 10_000);

        // The same row, written again with other money. The repository may only carry the paid flag.
        Invoice written = new Invoice(issued.getId(), issued.getWorkOrderId(), DAY, 99_999);
        written.setId(issued.getId());
        written.setPaid(true);
        invoices.save(written);

        Invoice stored = invoices.findById(issued.getId());
        assertEquals(10_000, stored.getAmount(), "beloppet på en utfärdad faktura skrivs inte över");
        assertEquals(10_000, stored.getTotalAmount());
        assertEquals("2026-0001", stored.getNumber(), "numret följer med fakturan och ändras inte");
        assertTrue(stored.isPaid(), "betalningen är det enda som får sättas efteråt");
    }

    @Test
    void aCreditNoteMirrorsTheOriginalAndNetsToZero() {

        Invoice original = issued(DAY, 10_000, 1_000);

        Invoice credit = garage.createCreditInvoice(original.getId(), "Fel pris");

        assertEquals(original.getId(), credit.getCreditOf());
        assertTrue(credit.isCredit());
        assertEquals("Fel pris", credit.getCreditReason());
        assertEquals(-original.getAmount(), credit.getAmount());
        assertEquals(-original.getDiscount(), credit.getDiscount());
        assertEquals(-original.getTotalAmount(), credit.getTotalAmount());
        // The VAT that was charged comes back to the öre, and the pair together is nothing.
        assertEquals(original.getVatAmount(), -credit.getVatAmount());
        assertEquals(0, original.getTotalIncludingVat() + credit.getTotalIncludingVat());
        assertEquals("2026-0002", credit.getNumber(), "kreditfakturan har ett eget nummer i serien");

        assertEquals(original.getLines().size(), credit.getLines().size());
        for (int i = 0; i < credit.getLines().size(); i++) {
            InvoiceLine line = credit.getLines().get(i);
            assertEquals(-original.getLines().get(i).getPrice(), line.getPrice());
            assertEquals(-original.getLines().get(i).getFinalPrice(), line.getFinalPrice(),
                    "en kreditrad behåller sitt negativa belopp");
        }
    }

    @Test
    void anInvoiceIsCreditedOnceAndACreditNoteIsNotCredited() {

        Invoice original = issued(DAY, 10_000);
        Invoice credit = garage.createCreditInvoice(original.getId(), "Fel pris");

        assertThrows(RuleViolationException.class,
                () -> garage.createCreditInvoice(original.getId(), "Igen"),
                "en faktura tas tillbaka en gång");
        assertThrows(RuleViolationException.class,
                () -> garage.createCreditInvoice(credit.getId(), "Kreditera krediten"),
                "en kreditfaktura är ingen skuld att ta tillbaka");
    }

    @Test
    void theVatRoundsTheSameWayOnBothSides() {

        assertEquals(250, new Invoice(0, 0, DAY, 1_000).getVatAmount());
        assertEquals(-250, new Invoice(0, 0, DAY, -1_000).getVatAmount());
        assertEquals(25, new Invoice(0, 0, DAY, 99).getVatAmount());
        assertEquals(-25, new Invoice(0, 0, DAY, -99).getVatAmount());
    }

    @Test
    void theSellerDetailsStandOnTheInvoiceAndAreTheOnesTheWorkshopSaved() {

        assertEquals("SE556000000001", garage.seller().vatNumber(),
                "en ny installation börjar med demouppgifterna");

        garage.saveSeller(new com.wac.autocore.model.Seller("Verkstan AB", "Storgatan 2", "512 00",
                "Svenljunga", "070-1234567", "verkstan@example.se", "559123-4567", "SE559123456701", 20));

        assertEquals("SE559123456701", garage.seller().vatNumber());
        assertEquals(20, garage.seller().paymentTermsDays(),
                "betalningsvillkoren gäller förfallodatumet på fakturan");
    }

    private Invoice issued(LocalDate date, int amount) {
        return issued(date, amount, 0);
    }

    private Invoice issued(LocalDate date, int amount, int discount) {
        Invoice invoice = new Invoice(0, 0, date, amount);
        invoice.setDiscount(discount);
        invoice.addLine(new InvoiceLine(0, 0, 1, "Oljebyte", amount, discount));
        invoices.save(invoice);
        return invoice;
    }
}
