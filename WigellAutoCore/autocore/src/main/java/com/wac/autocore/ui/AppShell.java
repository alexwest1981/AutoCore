package com.wac.autocore.ui;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.navigation.SidebarView;

import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * The frame the application runs in: the menu on the left and the pages on the right. Built after
 * someone has signed in, and put where the sign-in card stood.
 */
public final class AppShell {

    private final BorderPane view;

    private final PageRouter router;

    public AppShell(GarageSystem garage, Runnable onSignOut) {
        VBox pageBox = new VBox(18);
        pageBox.getStyleClass().add("pages");

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        router = new PageRouter(garage, pageBox);
        SidebarView sidebar = new SidebarView(router::navigate, onSignOut, garage);
        router.setSidebar(sidebar);

        BorderPane mainCol = new BorderPane();
        mainCol.getStyleClass().add("col");
        mainCol.setCenter(scroll);

        view = new BorderPane();
        view.getStyleClass().add("shell");
        view.setLeft(sidebar.getView());
        view.setCenter(mainCol);
    }

    public Parent getView() {
        return view;
    }

    /** Opens a page. The first one is the overview. */
    public void open(String pageKey) {
        router.navigate(pageKey);
    }
}
