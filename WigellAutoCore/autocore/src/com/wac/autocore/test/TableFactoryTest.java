package com.wac.autocore.test;

import com.wac.autocore.model.Customer;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import javafx.embed.swing.JFXPanel;

import java.util.Arrays;
import java.util.List;

/**
 * Enhetstester för TableFactory och dess flerkolumnssökning (applySearch).
 */
public class TableFactoryTest {

    static {
        // Initierar JavaFX Toolkit så att TableView/TableColumn kan instansieras
        new JFXPanel();
    }

    public void testSearchFilterMatchesSingleQuery() {
        List<Customer> data = Arrays.asList(
                new Customer(1, "Anna Andersson", "070-111111", "anna@example.com"),
                new Customer(2, "Bengt Berg", "070-222222", "bengt@example.com"),
                new Customer(3, "Cecilia Carlsson", "070-333333", "cecilia@example.com")
        );

        FilterableTable<Customer> table = TableFactory.create(data);
        table.getTableView().getColumns().addAll(
                TableFactory.col("Name", 150, Customer::getName),
                TableFactory.col("Email", 200, Customer::getEmail)
        );

        table.applySearch("bengt");
        assertTrue(table.getFilteredList().size() == 1, "Förväntade 1 träff för 'bengt'");
        assertTrue(table.getFilteredList().get(0).getName().equals("Bengt Berg"), "Förväntade Bengt Berg som träff");
    }

    public void testSearchFilterFindsItemsRegardlessOfBaseListIndex() {
        // Detta testar specifikt buggen i SCRUM-70 där col.getCellData(idx)
        // slog upp fel element när filteredList krympte.
        List<Customer> data = Arrays.asList(
                new Customer(1, "Kund Ett", "111", "ett@test.se"),
                new Customer(2, "Kund Tva", "222", "tva@test.se"),
                new Customer(3, "Kund Tre", "333", "tre@test.se"),
                new Customer(4, "Kund Fyra", "444", "fyra@test.se"),
                new Customer(5, "Kund Fem", "555", "fem@test.se")
        );

        FilterableTable<Customer> table = TableFactory.create(data);
        table.getTableView().getColumns().add(
                TableFactory.col("Name", 150, Customer::getName)
        );

        // Sök på sista elementet direkt
        table.applySearch("fem");
        assertTrue(table.getFilteredList().size() == 1, "Sökning på 'fem' ska ge 1 träff");
        assertTrue(table.getFilteredList().get(0).getName().equals("Kund Fem"), "Ska hitta sista elementet");

        // Töm sökning
        table.applySearch("");
        assertTrue(table.getFilteredList().size() == 5, "Tom sökning ska visa alla 5 element");

        // Sök på något som inte finns
        table.applySearch("okänd");
        assertTrue(table.getFilteredList().isEmpty(), "Sökning på okänd ska ge 0 träffar");
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
