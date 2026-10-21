package com.wac.autocore.ui;

import com.wac.autocore.security.Identity;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The sign-in card that stands in the middle of the window before anyone is signed in. It is not a
 * window of its own: the application window is already open, and the card gives way to the
 * application when the name and the password belong together.
 */
public final class LoginView {

    private static final double CARD_WIDTH = 360;

    private LoginView() {}

    /** The card. {@code onSignedIn} is called once, with the identity the sign-in produced. */
    public static Parent build(final LoginService login, final Consumer<Identity> onSignedIn) {
        final TextField username = new TextField();

        final PasswordField password = new PasswordField();

        final Label error = new Label();
        error.getStyleClass().add("login-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        final Button signIn = new Button(I18n.get("login.sign_in"));
        signIn.getStyleClass().add("primary-button");
        signIn.setDefaultButton(true);
        signIn.setMaxWidth(Double.MAX_VALUE);

        Runnable attempt = () -> {
            Optional<Identity> identity = login.signIn(username.getText(), password.getText());
            if (identity.isEmpty()) {
                // Stay on the card and say why. The window is already open, so there is nowhere
                // else to go and nothing to close.
                error.setText(I18n.get("login.error"));
                error.setVisible(true);
                error.setManaged(true);
                password.clear();
                password.requestFocus();
                return;
            }
            onSignedIn.accept(identity.get());
        };
        signIn.setOnAction(clicked -> attempt.run());
        username.setOnAction(entered -> attempt.run());
        password.setOnAction(entered -> attempt.run());

        VBox card = new VBox(14,
                brand(),
                productName(),
                labelled("login.username", username),
                labelled("login.password", password),
                error,
                signIn);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(CARD_WIDTH);
        // The stack that holds the card would stretch it top to bottom without this.
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private static Node brand() {
        Image logo = UiComponents.loadLogoImage();
        if (logo == null || logo.isError()) {
            Label fallback = new Label(I18n.get("nav.brand.title"));
            fallback.getStyleClass().add("login-brand");
            return fallback;
        }
        ImageView view = new ImageView(logo);
        view.setPreserveRatio(true);
        view.setFitWidth(190);
        view.setSmooth(true);
        return view;
    }

    private static Node productName() {
        Label name = new Label(I18n.get("login.secure"));
        name.getStyleClass().add("login-secure");
        return name;
    }

    private static Node labelled(String key, Region field) {
        Label label = new Label(I18n.get(key));
        label.getStyleClass().add("login-field-label");
        field.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, label, field);
        box.setFillWidth(true);
        return box;
    }
}
