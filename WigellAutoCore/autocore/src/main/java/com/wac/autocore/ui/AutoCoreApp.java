package com.wac.autocore.ui;

import com.wac.autocore.AutoCoreApplication;
import com.wac.autocore.data.Db;
import com.wac.autocore.data.Settings;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.service.PasswordRotation;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.i18n.I18n;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** The JavaFX interface for Wigell AutoCore. */
public class AutoCoreApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(AutoCoreApp.class);

    private ConfigurableApplicationContext context;

    private GarageSystem garage;

    private StackPane root;

    private IdentityHolder identityHolder;

    private LoginService loginService;

    /**
     * The context is opened here, on the JavaFX launcher thread, because JavaFX creates this
     * object itself: a bean it made would never be the one shown. The storage comes from
     * DatabaseConfig, and the window only ever sees the factory the context built.
     */
    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        context = SpringApplication.run(AutoCoreApplication.class, args);

        Db db = context.getBean(Db.class);
        try {
            db.ensureReady();
        } catch (RuntimeException e) {
            // Without a finished database the window opens empty and still looks like
            // it works. Say what is wrong and open nothing, instead of half an app.
            log.error("Databasen är inte körbar. Åtgärda felet och starta om.", e);
            context.close();
            System.exit(1);
        }

        applyConfiguredPasswords();

        garage = new GuardedGarageSystem(context.getBean(RepositoryFactory.class), context.getBean(IdentityHolder.class));
        identityHolder = context.getBean(IdentityHolder.class);
        loginService = context.getBean(LoginService.class);

        // Before the sign-in window: it is the first thing a person sees, and it should speak the
        // language they chose last time.
        restoreLanguage();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        installErrorBoundary();
        persistLanguageChanges();

        root = new StackPane();
        root.getStyleClass().addAll("root", "stage");

        Scene scene = new Scene(root, 1280, 800);
        ThemeManager.apply(scene, com.wac.autocore.theme.ThemeCatalog.DEFAULT_SLUG);

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();

        showSignIn();
    }

    /** The window stands open, with the sign-in card in the middle of it. */
    private void showSignIn() {
        root.getChildren().setAll(LoginView.build(loginService, this::signedIn));
        root.setAlignment(Pos.CENTER);
    }

    private void signedIn(Identity identity) {
        identityHolder.set(identity);
        showApplication();
    }

    /** The card gives way to the shell, and the first page is the overview. */
    private void showApplication() {
        AppShell shell = new AppShell(garage);
        root.getChildren().setAll(shell.getView());
        shell.open("overview");
    }

    /**
     * Shows our own failures as a sentence instead of a stack trace. One place covers every screen.
     */
    public static void installErrorBoundary() {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            // Our own refusals are logged by kind; anything else is a real fault and needs the trace.
            if (failure instanceof AppException) {
                ActionDialogs.logAppError((AppException) failure);
            } else {
                log.error("Oväntat fel i tråden {}", thread.getName(), failure);
            }
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), errorMessageFor(failure));
        });
    }

    /** The one place that turns an error into a text, so the same error reads the same everywhere. */
    public static String errorMessageFor(Throwable failure) {
        if (failure instanceof AppException) {
            String key = ((AppException) failure).getMessageKey();
            if (key != null) {
                // A refusal the service has already worded: the sentence alone is the answer.
                return I18n.get(key);
            }
            return I18n.get("view.error.action_failed") + "\n\n" + failure.getMessage();
        }
        // Not one of ours: the class and its text are for the log, the reader gets the sentence.
        return I18n.get("view.error.action_failed");
    }

    /**
     * Turns configured passwords into hashes once the schema is there. A bad value stops the start
     * with the account it belongs to, never with the value itself.
     */
    private void applyConfiguredPasswords() {
        try {
            java.util.List<String> changed = context.getBean(PasswordRotation.class).apply();
            if (!changed.isEmpty()) {
                log.info("Lösenord satta från konfigurationen för: {}", changed);
            }
        } catch (IllegalArgumentException bad) {
            log.error("Programmet startar inte med en ogiltig säkerhetskonfiguration: {}", bad.getMessage());
            context.close();
            System.exit(1);
        }
    }

    /**
     * Reads the last chosen language from the settings. The sign-in window is the first screen, so
     * the language has to be known before anyone is signed in, and the guarded facade refuses every
     * read until then. This one row is therefore read from the table directly.
     */
    public void restoreLanguage() {
        I18n.setLanguage(Settings.get(context.getBean(Db.class), GarageSystem.LANGUAGE_KEY, I18n.DEFAULT_LANG));
    }

    /** Saves language switches so the choice survives a restart. Every role may do this. */
    public void persistLanguageChanges() {
        I18n.addListener(garage::saveLanguage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
