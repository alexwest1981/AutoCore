package com.wac.autocore.ui.components;

import com.wac.autocore.ui.util.UiFormatters;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Function;

/**
 * Fabriksmetoder för att skapa konsekvent stylade och sökbara JavaFX-tabeller.
 */
public final class TableFactory {

    private TableFactory() {}

    /**
     * Behållare för en TableView kopplad till en ObservableList och en FilteredList.
     */
    public static class FilterableTable<S> {
        private final TableView<S> tableView;
        private final ObservableList<S> baseList;
        private final FilteredList<S> filteredList;

        public FilterableTable(List<S> data) {
            this.baseList = FXCollections.observableArrayList(data);
            this.filteredList = new FilteredList<S>(this.baseList);
            this.tableView = new TableView<S>(this.filteredList);
            this.tableView.getStyleClass().add("orders-table");
            this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            Label placeholder = new Label(com.wac.autocore.ui.i18n.I18n.get("table.empty"));
            placeholder.getStyleClass().add("text-muted");
            this.tableView.setPlaceholder(placeholder);
        }

        public TableView<S> getTableView() {
            return tableView;
        }

        public ObservableList<S> getBaseList() {
            return baseList;
        }

        public FilteredList<S> getFilteredList() {
            return filteredList;
        }

        /**
         * Applicerar ett sökfilter över alla tabellens kolumner.
         */
        public void applySearch(String query) {
            final String q = query == null ? "" : query.trim().toLowerCase();
            filteredList.setPredicate(row -> {
                if (q.isEmpty()) {
                    return true;
                }
                if (row == null) {
                    return false;
                }
                for (TableColumn<S, ?> col : tableView.getColumns()) {
                    if (col == null) {
                        continue;
                    }
                    Object val = col.getCellData(row);
                    if (val != null && val.toString().toLowerCase().contains(q)) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    public static <S> FilterableTable<S> create(List<S> data) {
        return new FilterableTable<S>(data);
    }

    /**
     * Skapar en standardtextkolumn.
     */
    public static <S> TableColumn<S, String> col(String title, double width,
                                                Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        // Golvet är 70px, inte 40: en kolumn som pressas ihop till 40 klipper "2013" till "20…", och
        // det är min-bredden (inte önskad bredd) som avgör hur långt tabellens
        // CONSTRAINED_RESIZE_POLICY får krympa en kolumn. 70 räcker för fyrsiffriga id:n och korta
        // värden; de långa textkolumnerna får ta resten.
        c.setMinWidth(Math.min(width, 70));
        if (width <= 70) {
            c.setMaxWidth(100);
        }
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(mapper.apply(cd.getValue())));
        return c;
    }

    /**
     * Skapar en statuskolumn med färgkodade badge-chips.
     */
    public static <S> TableColumn<S, String> badgeCol(String title, double width,
                                                     Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        // Samma golv som textkolumnerna, men högre: en statuschip är bredare än sitt värde
        // ("Genomförd" klipptes till "Geno…" när kolumnen pressades ihop).
        c.setMinWidth(Math.min(width, 95));
        c.setMaxWidth(Math.max(width * 1.5, 240));
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(mapper.apply(cd.getValue())));
        c.setCellFactory(column -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                chip.getStyleClass().add("badge");
                String extra = UiFormatters.badgeClass(item);
                if (!extra.isEmpty()) {
                    chip.getStyleClass().add(extra);
                }
                setGraphic(chip);
                setText(null);
            }
        });
        return c;
    }
}
