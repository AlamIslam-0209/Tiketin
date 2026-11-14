package com.tiketin.ui.components;

import com.tiketin.ui.MainFrame;
import com.tiketin.ui.theme.TiketinTheme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

public class ToastNotification {

    private static final String TOAST_TRAY_KEY = "tiketin-toast-tray";

    public static void info(Object owner, String message) {
        show(owner, message, "info", TiketinTheme.ACCENT, Color.web("#FFF0EB"));
    }

    public static void success(Object owner, String message) {
        show(owner, message, "check", TiketinTheme.SUCCESS_FG, TiketinTheme.SUCCESS_BG);
    }

    public static void error(Object owner, String message) {
        show(owner, message, "alert", TiketinTheme.DANGER_FG, TiketinTheme.DANGER_BG);
    }

    private static void show(Object owner, String message, String iconName, Color accentFg, Color badgeBg) {
        javafx.application.Platform.runLater(() -> {
            Pane overlayPane = resolveOverlayPane(owner);
            if (overlayPane == null) {
                System.out.println("Toast [" + iconName + "]: " + message);
                return;
            }

            VBox tray = getOrCreateTray(overlayPane);

            HBox toastCard = new HBox(10);
            toastCard.setAlignment(Pos.CENTER_LEFT);
            toastCard.setPadding(new Insets(10, 14, 10, 12));
            toastCard.setMinWidth(220);
            toastCard.setMaxWidth(340);
            // Lock height strictly to content so StackPane never stretches the toast vertically
            toastCard.setMinHeight(Region.USE_PREF_SIZE);
            toastCard.setPrefHeight(Region.USE_COMPUTED_SIZE);
            toastCard.setMaxHeight(Region.USE_PREF_SIZE);
            toastCard.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-border-color: #CFD4CE;" +
                "-fx-border-width: 1px;" +
                "-fx-background-radius: 10px;" +
                "-fx-border-radius: 10px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(32,37,42,0.14), 16, 0, 0, 6);"
            );

            Node icon = TiketinTheme.getIcon(iconName, 14, accentFg);
            StackPane iconPill = new StackPane(icon);
            iconPill.setMinSize(26, 26);
            iconPill.setPrefSize(26, 26);
            iconPill.setMaxSize(26, 26);
            iconPill.setStyle("-fx-background-color: " + TiketinTheme.toHex(badgeBg) + "; -fx-background-radius: 7px;");

            Label msgLabel = new Label(message != null ? message : "");
            msgLabel.setFont(TiketinTheme.fontBold(12.5));
            msgLabel.setStyle("-fx-text-fill: #20252A;");
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(276);

            toastCard.getChildren().addAll(iconPill, msgLabel);
            tray.getChildren().add(toastCard);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toastCard);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            PauseTransition stay = new PauseTransition(Duration.seconds(3.0));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toastCard);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> tray.getChildren().remove(toastCard));

            SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
            seq.play();
        });
    }

    private static Pane resolveOverlayPane(Object owner) {
        if (owner instanceof MainFrame) {
            return ((MainFrame) owner).getOverlayPane();
        }
        if (owner instanceof Node) {
            Node n = (Node) owner;
            while (n != null) {
                if (n instanceof MainFrame) {
                    return ((MainFrame) n).getOverlayPane();
                }
                n = n.getParent();
            }
            if (((Node) owner).getScene() != null && ((Node) owner).getScene().getRoot() instanceof Pane) {
                return (Pane) ((Node) owner).getScene().getRoot();
            }
        }
        if (owner instanceof Pane) {
            return (Pane) owner;
        }
        if (owner instanceof Window) {
            Window w = (Window) owner;
            if (w.getScene() != null && w.getScene().getRoot() instanceof Pane) {
                return (Pane) w.getScene().getRoot();
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.getScene() != null && window.getScene().getRoot() instanceof StackPane) {
                return (Pane) window.getScene().getRoot();
            }
        }
        return null;
    }

    private static VBox getOrCreateTray(Pane overlayPane) {
        for (Node child : overlayPane.getChildren()) {
            if (TOAST_TRAY_KEY.equals(child.getUserData()) && child instanceof VBox) {
                return (VBox) child;
            }
        }
        VBox tray = new VBox(8);
        tray.setUserData(TOAST_TRAY_KEY);
        tray.setAlignment(Pos.BOTTOM_RIGHT);
        tray.setMouseTransparent(true);
        tray.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        tray.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (overlayPane instanceof StackPane) {
            StackPane.setAlignment(tray, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(tray, new Insets(0, 22, 22, 0));
        }
        overlayPane.getChildren().add(tray);
        return tray;
    }
}
