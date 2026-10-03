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
     * Luftmarginalen. En kolumn med fast format får sin uppmätta bredd plus den här marginalen, så
     * att värdet inte ligger dikt an mot grannen. Den nedre gränsen hindrar kolumnen från att
     * klippas på en liten skärm, den övre från att svälla på en stor.
     */
    public static final double AIR = 40;

    /** ID-kolumnen ser likadan ut i varje tabell. "1042" behöver 47. */
    public static final double W_ID = 55;

    /** Registreringsnummer. 90 och inte 80, för reservtexten "Vehicle #12" behöver 86. */
    public static final double W_REG_NR = 90;

    /** Årsmodell, fyra siffror. "2012" behöver 47. */
    public static final double W_YEAR = 55;

    /** Kronbelopp. "24,995 kr" behöver 95. */
    public static final double W_MONEY = 95;

    /** Minuter. "270 min" är 52, men rubriken "Beräknad tid" är 73, så behovet är 90. */
    public static final double W_MINUTES = 90;

    /** Klockslag i ett spann. "07:00 - 10:30" behöver 101. */
    public static final double W_TIME = 105;

    /** Datum. "2026-10-05" behöver 88. */
    public static final double W_DATE = 100;

    /** Datum och tid tillsammans. "2026-10-05 14:30" behöver 127. */
    public static final double W_DATETIME = 135;

    /** Hänvisning till en annan post, "#1042". Rubriken "Arbetsorder" är den breda delen och behöver 86. */
    public static final double W_REF = 95;

    /** Statuschip. Det bredaste ordet, "Arbetsorder skapad", behöver 137. */
    public static final double W_STATUS = 140;

    /** Ja och nej-chip. Rubriken "Tillgänglig" behöver 80. */
    public static final double W_FLAG = 90;

    /** Telefonnummer. "070-123 45 67" behöver 101. */
    public static final double W_PHONE = 110;

    /** Bilmärke. Det längsta i startdatan, "Volkswagen", behöver 94. */
    public static final double W_BRAND = 100;

    /** Modellnamn. "V70" behöver 64. */
    public static final double W_MODEL = 80;

    /** Betalningstyp. "Kortbetalning" behöver 100. */
    public static final double W_TYPE = 120;

    /**
     * Personnamn. Samma mått för Namn, Mekaniker och Kund, för de visar samma sorts värde.
     * "Johan Karlsson" behöver 114, den övre gränsen håller ett namn från att ta hela tabellen.
     */
    public static final double W_PERSON_MIN = 130;
    public static final double W_PERSON_MAX = 260;

    /** E-post. "anna.andersson@example.se" behöver 179. */
    public static final double W_EMAIL_MIN = 185;
    public static final double W_EMAIL_MAX = 320;

    /** Beskrivning. Den längsta raden i startdatan behöver 161. */
    public static final double W_TEXT_MIN = 150;
    public static final double W_TEXT_MAX = 400;

    /** Tjänstelista med priser. "Bromsservice (2,495 kr), Årsservice (3,495 kr)" behöver 293. */
    public static final double W_SERVICES_MIN = 240;
    public static final double W_SERVICES_MAX = 480;

    /** Specialisering. "Bromsar och hjulupphängning" behöver 116. */
    public static final double W_SPEC_MIN = 120;
    public static final double W_SPEC_MAX = 320;

    /**
     * En kolumn för ett värde med fast format, till exempel ett id, ett belopp eller ett datum.
     * Minsta bredd är vad värdet behöver för att inte klippas, den övre gränsen är samma bredd plus
     * luftmarginalen. Mellan de två får kolumnen röra sig, så att tabellen kan fylla sin ruta utan
     * att en kort kolumn sväller på en stor skärm.
     */
    public static <S> TableColumn<S, String> sizeCol(String title, double need,
                                                    Function<S, String> mapper) {
        return textCol(title, need, need + AIR, mapper);
    }

    /** Statuschip med samma mått som sizeCol. */
    public static <S> TableColumn<S, String> sizeBadge(String title, double need,
                                                      Function<S, String> mapper) {
        TableColumn<S, String> c = badgeCol(title, need, mapper);
        c.setMinWidth(need);
        c.setMaxWidth(Math.max(need + AIR, need * 1.2));
        return c;
    }

    /**
     * En textkolumn som får växa, men inte obegränsat. Minsta bredden är vad texten behöver för att
     * vara läsbar, den övre gränsen håller en lång rad från att äta hela tabellen på en stor skärm.
     * På en smal skärm krymper kolumnen till sin minsta bredd i stället för att klippas.
     */
    public static <S> TableColumn<S, String> textCol(String title, double minWidth, double maxWidth,
                                                     Function<S, String> mapper) {
        TableColumn<S, String> c = col(title, minWidth, mapper);
        c.setMinWidth(minWidth);
        c.setMaxWidth(maxWidth);
        return c;
    }

    /** ID-kolumnen, samma i alla tabeller. */
    public static <S> TableColumn<S, String> idCol(Function<S, String> mapper) {
        return sizeCol(com.wac.autocore.ui.i18n.I18n.get("table.col.id"), W_ID, mapper);
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
