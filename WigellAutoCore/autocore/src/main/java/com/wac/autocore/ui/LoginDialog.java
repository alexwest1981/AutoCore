package com.wac.autocore.ui;

import com.wac.autocore.security.Identity;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.i18n.I18n;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * The window that comes before the application: a name, a password, and nothing else.
 *
 * It is a stage of its own rather than a dialog, because there is no main window yet to own a
 * dialog, and an ownerless one closes at once. A wrong password leaves the window open with a
 * sentence instead of closing it with a silent nothing.
 */
public final class LoginDialog {

    private LoginDialog() {}

    /** Signs in, or comes back empty when the window was closed without a working sign-in. */
    public static Optional<Identity> showAndWait(final LoginService login) {
        final Stage stage = new Stage();
        stage.setTitle(I18n.get("login.title"));
        stage.setResizable(false);

        final TextField username = new TextField();
        username.setPromptText(I18n.get("login.username"));
        username.setPrefColumnCount(22);

        final PasswordField password = new PasswordField();
        password.setPromptText(I18n.get("login.password"));
        password.setPrefColumnCount(22);

        final Label error = new Label();
        error.getStyleClass().add("login-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        final GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(12);
        fields.addRow(0, new Label(I18n.get("login.username") + ":"), username);
        fields.addRow(1, new Label(I18n.get("login.password") + ":"), password);

        final Label appName = new Label(I18n.get("nav.brand.title"));
        appName.getStyleClass().add("login-brand");

        final Button signIn = new Button(I18n.get("login.sign_in"));
        signIn.setDefaultButton(true);
        final Button cancel = new Button(I18n.get("common.close"));
        cancel.setCancelButton(true);

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox buttons = new HBox(10, spacer, cancel, signIn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        final VBox root = new VBox(16, appName, fields, error, buttons);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("root");

        final Identity[] signedIn = new Identity[1];

        signIn.setOnAction(clicked -> {
            Optional<Identity> identity = login.signIn(username.getText(), password.getText());
            if (identity.isEmpty()) {
                // Stay where we are and say why. Closing the window would look like a crash.
                error.setText(I18n.get("login.error"));
                error.setVisible(true);
                error.setManaged(true);
                // The sentence is a new row in the window, so the window grows with it.
                stage.sizeToScene();
                password.clear();
                password.requestFocus();
                return;
            }
            signedIn[0] = identity.get();
            stage.close();
        });
        cancel.setOnAction(clicked -> stage.close());

        final Scene scene = new Scene(root);
        stage.setScene(scene);
        // The theme decides the padding and the font, so the window is measured after it is on.
        ThemeManager.apply(scene, ThemeCatalog.DEFAULT_SLUG);
        com.wac.autocore.ui.motion.PressDepth.install(scene);

        stage.setOnShown(shown -> {
            stage.sizeToScene();
            username.requestFocus();
        });
        stage.showAndWait();

        return Optional.ofNullable(signedIn[0]);
    }
}
