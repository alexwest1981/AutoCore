import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DesignSelectorApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Wigell AutoCore - Select Design Option (1-5)");

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 30; -fx-alignment: center; -fx-background-color: #1e1e1e;");

        Label title = new Label("Select which design proposal to display:");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button btn1 = createDesignButton("1. Industrial Dark Terminal", "dark-theme.css");
        Button btn2 = createDesignButton("2. Corporate Clean Light", "light-theme.css");
        Button btn3 = createDesignButton("3. Modern Dashboard (Cards)", "dashboard-theme.css");
        Button btn4 = createDesignButton("4. Compact Power User", "compact-theme.css");
        Button btn5 = createDesignButton("5. Touch / Kiosk Mode", "kiosk-theme.css");

        root.getChildren().addAll(title, btn1, btn2, btn3, btn4, btn5);

        Scene scene = new Scene(root, 400, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button createDesignButton(String name, String cssFile) {
        Button btn = new Button(name);
        btn.setStyle("-fx-font-size: 14px; -fx-pref-width: 330px; -fx-pref-height: 40px; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            System.out.println("Selected theme: " + cssFile);
            // Switch to the respective view once they have been created
        });
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}