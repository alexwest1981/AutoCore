package com.wac.autocore.theme;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.wac.autocore.theme.ThemeCatalog.Theme;

/**
 * Theme picker / preview. Fully self-contained demo window:
 * pick a theme on the left and watch the controls restyle on the right.
 *
 * When you have decided on the permanent theme, run:
 *     themer.py lock <module-dir> <slug>
 * which keeps only that theme, removes the others and deletes this file.
 */
public class ThemePickerApp extends Application {

    @Override
    public void start(Stage stage) {
        ListView<Theme> list = new ListView<Theme>();
        for (Theme t : ThemeCatalog.all()) list.getItems().add(t);
        list.setCellFactory(param -> new javafx.scene.control.ListCell<Theme>() {
            @Override protected void updateItem(Theme t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); }
                else setText((t.dark ? "\u25CF " : "\u25CB ") + t.name);
            }
        });
        list.setPrefWidth(240);

        Label hint = new Label("Theme picker (removable). Run themer.py lock <slug> to finalize.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        BorderPane wrap = new BorderPane();
        wrap.setCenter(buildPreview());
        wrap.setPadding(new Insets(16));

        VBox left = new VBox(8, new Label("Teman"), list, hint);
        left.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(left);
        root.setCenter(wrap);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 980, 640);
        stage.setTitle("Wigell AutoCore - Temaväljare");
        stage.setScene(scene);

        if (!ThemeCatalog.all().isEmpty()) {
            Theme first = ThemeCatalog.all().get(0);
            list.getSelectionModel().select(first);
            list.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) ->
                ThemeManager.apply(scene, newT.slug));
            ThemeManager.apply(scene, first.slug);
        }
        stage.show();
    }

    private BorderPane buildPreview() {
        Label title = new Label("AutoCore - Verkstad");
        title.getStyleClass().add("title");
        Label sub = new Label("Förhandsvisning av teman i JavaFX");
        sub.getStyleClass().add("muted");

        Button primary = new Button("Ny arbetsorder");
        primary.getStyleClass().add("primary");
        Button plain = new Button("Avbryt");
        Button ghost = new Button("Inställningar");
        ghost.getStyleClass().add("ghost");
        TextField search = new TextField();
        search.setPromptText("Sök arbetsorder, kund...");

        VBox header = new VBox(4, title, sub);
        HBox actions = new HBox(8, primary, plain, ghost);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox controls = new VBox(10, header, actions, search);
        controls.setPadding(new Insets(18));
        controls.getStyleClass().add("panel");
        HBox.setHgrow(search, Priority.ALWAYS);

        Label metricLabel = new Label("Omsättning");
        metricLabel.getStyleClass().add("metric-label");
        Label metricValue = new Label("284 650 kr");
        metricValue.getStyleClass().add("metric-value");
        VBox metric = new VBox(2, metricLabel, metricValue);
        metric.getStyleClass().add("metric-card");
        metric.setPadding(new Insets(14));

        Label section = new Label("Senaste arbetsorder");
        section.getStyleClass().add("section-title");
        Label st1 = new Label("AO-2481 \u00B7 Volvo XC60");
        Label b1 = new Label("Pågår");
        b1.getStyleClass().addAll("badge", "success");
        HBox row1 = new HBox(10, st1, new javafx.scene.layout.Region(), b1);
        HBox.setHgrow(row1.getChildren().get(1), Priority.ALWAYS);
        VBox table = new VBox(8, section, row1);
        table.setPadding(new Insets(14));
        table.getStyleClass().add("panel");

        BorderPane out = new BorderPane();
        VBox col = new VBox(14, controls, metric, table);
        out.setCenter(col);
        out.setPadding(new Insets(0));
        return out;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
