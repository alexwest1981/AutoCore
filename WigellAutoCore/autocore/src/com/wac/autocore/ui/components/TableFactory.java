package com.wac.autocore.ui.components;

import com.wac.autocore.ui.util.UiFormatters;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Function;

/** Fabriksmetoder för tabeller med samma stil. */
public final class TableFactory {

    private TableFactory() {}

/** En TableView med sitt data, delad i sidor så listan aldrig växer ur rutan. */
    public static class FilterableTable<S> {

        /** Rader per sida. Fler får ändå inte plats utan att rutan börjar scrolla. */
        public static final int PAGE_SIZE = 20;

        private final TableView<S> tableView;
        private final ObservableList<S> baseList;
        /** Raderna som visas just nu, alltså den aktuella sidan. */
        private final ObservableList<S> pageItems;
        private int page;

        public FilterableTable(List<S> data) {
            this.baseList = FXCollections.observableArrayList(data);
            this.pageItems = FXCollections.observableArrayList();
            this.tableView = new TableView<S>(this.pageItems);
            this.tableView.getStyleClass().add("orders-table");
            this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            Label placeholder = new Label(com.wac.autocore.ui.i18n.I18n.get("table.empty"));
            placeholder.getStyleClass().add("text-muted");
            this.tableView.setPlaceholder(placeholder);
            showPage(0);
        }

        public TableView<S> getTableView() {
            return tableView;
        }

        /** Allt som finns, inte bara sidan som visas. */
        public ObservableList<S> getBaseList() {
            return baseList;
        }

        public int getTotalCount() {
            return baseList.size();
        }

        public int getPageSize() {
            return PAGE_SIZE;
        }

        public int getPage() {
            return page;
        }

        public int getPageCount() {
            int pages = (baseList.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            return pages < 1 ? 1 : pages;
        }

        /** Första radnumret på sidan, räknat från 1. Tom lista ger 0. */
        public int getFirstRowNumber() {
            return baseList.isEmpty() ? 0 : page * PAGE_SIZE + 1;
        }

        public int getLastRowNumber() {
            int last = (page + 1) * PAGE_SIZE;
            return last > baseList.size() ? baseList.size() : last;
        }

        public void showPage(int index) {
            if (index < 0) {
                index = 0;
            }
            if (index > getPageCount() - 1) {
                index = getPageCount() - 1;
            }
            this.page = index;
            int from = index * PAGE_SIZE;
            int to = from + PAGE_SIZE;
            if (to > baseList.size()) {
                to = baseList.size();
            }
            if (from >= to) {
                pageItems.clear();
                return;
            }
            pageItems.setAll(baseList.subList(from, to));
        }

        public void nextPage() {
            showPage(page + 1);
        }

        public void previousPage() {
            showPage(page - 1);
        }
    }

    public static <S> FilterableTable<S> create(List<S> data) {
        return new FilterableTable<S>(data);
    }

    /** Bläddringsraden under en tabell. Göms när allt får plats på en sida. */
    public static <S> Node buildPager(FilterableTable<S> table) {
        Label showing = new Label();
        showing.getStyleClass().addAll("srow-sub", "small");

        Button previous = new Button(com.wac.autocore.ui.i18n.I18n.get("list.previous"));
        Button next = new Button(com.wac.autocore.ui.i18n.I18n.get("list.next"));
        previous.getStyleClass().add("secondary-button");
        next.getStyleClass().add("secondary-button");

        Runnable update = () -> {
            boolean empty = table.getTotalCount() == 0;
            boolean many = table.getPageCount() > 1;
            previous.setDisable(empty || !many || table.getPage() == 0);
            next.setDisable(empty || !many || table.getPage() >= table.getPageCount() - 1);
            // Tom lista har inget att visa, men raden behåller sin höjd så panelen står still.
            showing.setText(empty
                    ? ""
                    : com.wac.autocore.ui.i18n.I18n.get("list.showing",
                            table.getFirstRowNumber(), table.getLastRowNumber(), table.getTotalCount()));
        };

        previous.setOnAction(e -> {
            table.previousPage();
            update.run();
        });
        next.setOnAction(e -> {
            table.nextPage();
            update.run();
        });

        update.run();

        HBox row = new HBox(10, showing, previous, next);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

/** Luftmarginal: en kolumn med fast format får sin uppmätta bredd plus den här. */
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

/** Hänvisning till en annan post, som #1042. */
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

/** Personnamn, samma mått för Namn, Mekaniker och Kund. */
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

/** Kolumn för ett värde med fast format. Minst vad värdet behöver, högst det plus luft. */
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

/** Textkolumn som får växa, men inte ta hela tabellen. */
    public static <S> TableColumn<S, String> textCol(String title, double minWidth, double maxWidth,
                                                     Function<S, String> mapper) {
        TableColumn<S, String> c = col(title, minWidth, mapper);
        c.setMinWidth(minWidth);
        c.setMaxWidth(maxWidth);
        return c;
    }

/** ID-kolumnen, samma i alla tabeller. Ingen luftmarginal. */
    public static <S> TableColumn<S, String> idCol(Function<S, String> mapper) {
        return textCol(com.wac.autocore.ui.i18n.I18n.get("table.col.id"), W_ID, W_ID, mapper);
    }

/** Standardtextkolumn. */
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

/** Statuskolumn med färgade märken. */
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
