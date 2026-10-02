package com.wac.autocore.test;

import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.GarageSystem;

/**
 * Prov för kunduppgifterna: namn med siffror, telefonnummer med bokstäver eller fel antal siffror och
 * e-postadresser utan @ ska nekas, och ett telefonnummer som skrivs in ska sparas med bindestreck.
 */
public class CustomerValidationTest {

    public void testPhoneNumberIsStoredWithASeparator() {
        TestRunner.assertEquals("070-1234567",
                new Customer(0, "Anna Andersson", "0701234567", "anna@exempel.se").getPhone(),
                "Ett telefonnummer som skrivs utan bindestreck ska sparas som 070-1234567");
        TestRunner.assertEquals("070-1234567",
                new Customer(0, "Anna Andersson", "070 12 34 567", "anna@exempel.se").getPhone(),
                "Mellanslag ska normaliseras till samma form");

        // Äldre rader och utländska nummer har inte den svenska formen och ska lämnas i fred.
        TestRunner.assertEquals("031-12345",
                new Customer(0, "Anna Andersson", "031-12345", "").getPhone(),
                "Ett nummer som inte är 10 siffror ska lämnas orört, inte bli '-'");
    }

    public void testNamesPhonesAndEmailsAreChecked() {
        TestRunner.assertEquals(Customer.PROBLEM_NAME_DIGITS,
                Customer.validationProblem("Lucas 2", "070-1234567", "lucas@exempel.se"),
                "Siffror i namnet ska nekas");
        TestRunner.assertEquals(Customer.PROBLEM_NAME_DIGITS,
                Customer.validationProblem("Lucas2024", "070-1234567", ""),
                "Siffror sist i namnet ska nekas");

        TestRunner.assertEquals(Customer.PROBLEM_PHONE_TEN_DIGITS,
                Customer.validationProblem("Lucas", "070-1234abc", ""),
                "Bokstäver i telefonnumret ska nekas");
        TestRunner.assertEquals(Customer.PROBLEM_PHONE_TEN_DIGITS,
                Customer.validationProblem("Lucas", "070-123456", ""),
                "9 siffror ska nekas");
        TestRunner.assertEquals(Customer.PROBLEM_PHONE_TEN_DIGITS,
                Customer.validationProblem("Lucas", "070-12345678", ""),
                "11 siffror ska nekas");

        TestRunner.assertEquals(Customer.PROBLEM_EMAIL_FORMAT,
                Customer.validationProblem("Lucas", "070-1234567", "lucas.exempel.se"),
                "En adress utan @ ska nekas");
        TestRunner.assertEquals(Customer.PROBLEM_EMAIL_FORMAT,
                Customer.validationProblem("Lucas", "070-1234567", "lucas@exempel"),
                "En adress utan domändel ska nekas");

        TestRunner.assertEquals(Customer.PROBLEM_REQUIRED,
                Customer.validationProblem("", "070-1234567", ""),
                "Tomt namn ska nekas");
        TestRunner.assertEquals(null,
                Customer.validationProblem("Lucas Berg", "070-1234567", ""),
                "E-post är frivillig; ett tomt fält ska gå igenom");
        TestRunner.assertEquals(null,
                Customer.validationProblem("Lucas Berg", "070 1234567", "lucas@exempel.se"),
                "Ett fullständigt namn, telefonnummer och en giltig adress ska gå igenom");
    }

    /**
     * Regeln sitter i tjänstelagret, så både skapa- och redigeravägen går genom den. Formuläret visar
     * bara samma svar som text.
     */
    public void testServiceAndUpdateRefuseACustomerWithDigitsInTheName() throws Exception {
        CustomerService service = new CustomerService();
        try {
            service.createCustomer("Lucas 2", "070-1234567", "lucas@exempel.se");
            TestRunner.assertTrue(false, "Tjänstelagret ska neka ett namn med siffror");
        } catch (IllegalArgumentException expected) {
            TestRunner.assertTrue(expected.getMessage() != null && expected.getMessage().contains(Customer.PROBLEM_NAME_DIGITS),
                    "Nekandet ska namnge regeln som bröts, men sa: " + expected.getMessage());
        }

        CustomerRepository repository = new CustomerRepository();
        GarageSystem garage = new GarageSystem();
        Customer saved = service.createCustomer("Test Person", "0701234567", "");
        TestRunner.assertNotNull(saved, "En giltig kund ska fortfarande kunna sparas");
        try {
            TestRunner.assertEquals("070-1234567", saved.getPhone(),
                    "Den sparade kunden ska ha telefonnumret i normaliserad form");

            saved.setName("Test 5");
            try {
                garage.updateCustomer(saved);
                TestRunner.assertTrue(false, "Ändringen ska nekas av samma regel som skapandet");
            } catch (IllegalArgumentException expected) {
                TestRunner.assertEquals("Test Person", repository.findById(saved.getId()).getName(),
                        "En nekad ändring får inte ha skrivits till databasen");
            }
        } finally {
            repository.delete(saved.getId());
        }
    }
}
