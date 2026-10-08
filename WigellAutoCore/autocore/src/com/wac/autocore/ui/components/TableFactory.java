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

/** Factory methods for tables that share the same styling. */
public final class TableFactory {

    private TableFactory() {}

    /** A TableView with its data, split into pages so the list never outgrows the panel. */
    public static class FilterableTable<S> {

        /** Rows per page. More would not fit without the panel starting to scroll anyway. */
        public static final int PAGE_SIZE = 20;

        private final TableView<S> tableView;
        private final ObservableList<S> baseList;
        /** The rows on screen right now, that is the current page. */
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

        /** Everything there is, not just the page on screen. */
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

        /** First row number on the page, counted from 1. An empty list gives 0. */
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

    /** The pager row below a table. Hidden when everything fits on one page. */
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
            // An empty list has nothing to show, but the row keeps its height so the panel stays put.
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

    /** Breathing room: a column of fixed width gets its measured width plus this. */
    public static final double AIR = 40;

    /** The ID column looks the same in every table. "1042" needs 47. */
    public static final double W_ID = 55;

    /** Registration number. 90 and not 80, because the fallback text "Vehicle #12" needs 86. */
    public static final double W_REG_NR = 90;

    /** Model year, four digits. "2012" needs 47. */
    public static final double W_YEAR = 55;

    /** Amount in kronor. "24,995 kr" needs 95. */
    public static final double W_MONEY = 95;

    /** Minutes. "270 min" is 52, but the heading "Beräknad tid" is 73, so the need is 90. */
    public static final double W_MINUTES = 90;

    /** A time range. "07:00 - 10:30" needs 101. */
    public static final double W_TIME = 105;

    /** Date. "2026-10-05" needs 88. */
    public static final double W_DATE = 100;

    /** Date and time together. "2026-10-05 14:30" needs 127. */
    public static final double W_DATETIME = 135;

    /** A reference to another record, like #1042. */
    public static final double W_REF = 95;

    /** Status chip. The widest word, "Arbetsorder skapad", needs 137. */
    public static final double W_STATUS = 140;

    /** Yes/no chip. The heading "Tillgänglig" needs 80. */
    public static final double W_FLAG = 90;

    /** Phone number. "070-123 45 67" needs 101. */
    public static final double W_PHONE = 110;

    /** Car brand. The longest in the seed data, "Volkswagen", needs 94. */
    public static final double W_BRAND = 100;

    /** Model name. "V70" needs 64. */
    public static final double W_MODEL = 80;

    /** Payment type. "Kortbetalning" needs 100. */
    public static final double W_TYPE = 120;

    /** Person names, same width for Name, Mechanic and Customer. */
    public static final double W_PERSON_MIN = 130;
    public static final double W_PERSON_MAX = 260;

    /** E-mail. "anna.andersson@example.se" needs 179. */
    public static final double W_EMAIL_MIN = 185;
    public static final double W_EMAIL_MAX = 320;

    /** Description. The longest line in the seed data needs 161. */
    public static final double W_TEXT_MIN = 150;
    public static final double W_TEXT_MAX = 400;

    /** Service list with prices. "Bromsservice (2,495 kr), Årsservice (3,495 kr)" needs 293. */
    public static final double W_SERVICES_MIN = 240;
    public static final double W_SERVICES_MAX = 480;

    /** Specialization. "Bromsar och hjulupphängning" needs 116. */
    public static final double W_SPEC_MIN = 120;
    public static final double W_SPEC_MAX = 320;

    /** Column for a value of fixed shape. At least what the value needs, at most that plus air. */
    public static <S> TableColumn<S, String> sizeCol(String title, double need,
                                                    Function<S, String> mapper) {
        return textCol(title, need, need + AIR, mapper);
    }

    /** Status chip with the same measurements as sizeCol. */
    public static <S> TableColumn<S, String> sizeBadge(String title, double need,
                                                      Function<S, String> mapper) {
        TableColumn<S, String> c = badgeCol(title, need, mapper);
        c.setMinWidth(need);
        c.setMaxWidth(Math.max(need + AIR, need * 1.2));
        return c;
    }

    /** Text column that may grow, but not take the whole table. */
    public static <S> TableColumn<S, String> textCol(String title, double minWidth, double maxWidth,
                                                     Function<S, String> mapper) {
        TableColumn<S, String> c = col(title, minWidth, mapper);
        c.setMinWidth(minWidth);
        c.setMaxWidth(maxWidth);
        return c;
    }

    /** The ID column, same in every table. No breathing room. */
    public static <S> TableColumn<S, String> idCol(Function<S, String> mapper) {
        return textCol(com.wac.autocore.ui.i18n.I18n.get("table.col.id"), W_ID, W_ID, mapper);
    }

    public static <S> TableColumn<S, String> col(String title, double width,
                                                Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        // The floor is 70px, not 40: a column squeezed to 40 clips "2013" to "20…", and it is
        // the min width (not the preferred width) that decides how far the table's
        // CONSTRAINED_RESIZE_POLICY may shrink a column. 70 is enough for four-digit ids
        // and short values; the long text columns take the rest.
        c.setMinWidth(Math.min(width, 70));
        if (width <= 70) {
            c.setMaxWidth(100);
        }
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(mapper.apply(cd.getValue())));
        return c;
    }

    /** Status column with coloured badges. */
    public static <S> TableColumn<S, String> badgeCol(String title, double width,
                                                     Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        // Same floor as the text columns, but higher: a status chip is wider than its value
        // ("Genomförd" was clipped to "Geno…" when the column was squeezed).
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
