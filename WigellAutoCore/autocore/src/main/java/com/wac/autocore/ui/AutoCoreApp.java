package com.wac.autocore.ui;

import com.wac.autocore.AutoCoreApplication;
import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.service.PasswordRotation;
import com.wac.autocore.theme.ThemeManager;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;


/** The JavaFX interface for Wigell AutoCore. */
public class AutoCoreApp extends Application {

    /** Key the language choice is stored under in the settings table. */
    public static final String LANGUAGE_KEY = "language";

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
            System.out.println(e.getMessage());
            System.out.println("Databasen är inte körbar. Åtgärda felet och starta om.");
            context.close();
            System.exit(1);
        }

        applyConfiguredPasswords();

        garage = new GarageSystem(context.getBean(RepositoryFactory.class));
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
        com.wac.autocore.ui.motion.PressDepth.install(scene);

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
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("shell");

        VBox pageBox = new VBox(18);
        pageBox.getStyleClass().add("pages");

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        PageRouter router = new PageRouter(garage, pageBox);
        SidebarView sidebar = new SidebarView(router::navigate);
        router.setSidebar(sidebar);

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");
        mainCol.setCenter(scroll);

        shell.setLeft(sidebar.getView());
        shell.setCenter(mainCol);

        root.getChildren().setAll(shell);
        router.navigate("overview");
    }

    /**
     * Shows our own failures as a sentence instead of a stack trace. One place covers every screen.
     */
    public static void installErrorBoundary() {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            // A refusal with a sentence is the program working; only real faults need the trace.
            if (!(failure instanceof AppException) || ((AppException) failure).getMessageKey() == null) {
                failure.printStackTrace();
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
        return I18n.get("view.error.action_failed") + "\n\n" + failure.toString();
    }

    /**
     * Turns configured passwords into hashes once the schema is there. A bad value stops the start
     * with the account it belongs to, never with the value itself.
     */
    private void applyConfiguredPasswords() {
        try {
            java.util.List<String> changed = context.getBean(PasswordRotation.class).apply();
            if (!changed.isEmpty()) {
                System.out.println("Lösenord satta från konfigurationen för: " + changed);
            }
        } catch (IllegalArgumentException bad) {
            System.out.println(bad.getMessage());
            System.out.println("Programmet startar inte med en ogiltig säkerhetskonfiguration.");
            context.close();
            System.exit(1);
        }
    }

    /** Reads the last chosen language from the settings. */
    public void restoreLanguage() {
        I18n.setLanguage(garage.getSetting(LANGUAGE_KEY, I18n.DEFAULT_LANG));
    }

    /** Saves language switches so the choice survives a restart. */
    public void persistLanguageChanges() {
        I18n.addListener(lang -> garage.saveSetting(LANGUAGE_KEY, lang));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
