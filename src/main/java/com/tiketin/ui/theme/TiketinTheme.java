package com.tiketin.ui.theme;

import com.tiketin.ui.components.AnimatedButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class TiketinTheme {

    public static final Color BG_APP = Color.web("#F7F8F6");
    public static final Color BG_SURFACE = Color.web("#FFFFFF");
    public static final Color BG_SUBTLE = Color.web("#F3F1ED");
    public static final Color BG_BEZEL = Color.web("#F7F8F6");
    public static final Color BG_SIDEBAR = Color.web("#F4F1EC");

    public static final Color TEXT_PRIMARY = Color.web("#20252A");
    public static final Color TEXT_SECONDARY = Color.web("#525A62");
    public static final Color TEXT_MUTED = Color.web("#687078");

    public static final Color BORDER = Color.web("#E3E6E2");
    public static final Color BORDER_STRONG = Color.web("#CFD4CE");

    public static final Color ACCENT = Color.web("#E06338");
    public static final Color ACCENT_HOVER = Color.web("#C85229");
    public static final Color ACCENT_SOFT = Color.web("#FFF0EB");
    public static final Color ACCENT_TEXT = Color.web("#FFFFFF");

    public static final Color SUCCESS_FG = Color.web("#15803D");
    public static final Color SUCCESS_BG = Color.web("#DCFCE7");
    public static final Color WARNING_FG = Color.web("#B45309");
    public static final Color WARNING_BG = Color.web("#FEF3C7");
    public static final Color DANGER_FG = Color.web("#B91C1C");
    public static final Color DANGER_BG = Color.web("#FEE2E2");
    public static final Color NEUTRAL_FG = Color.web("#475569");
    public static final Color NEUTRAL_BG = Color.web("#F1F5F9");

    private static String primaryFont = "System";
    private static String monoFont = "Monospaced";

    static {
        try {
            var families = Font.getFamilies();
            // Always use JavaFX logical "System" so CSS (-fx-font-family: "System")
            // and Java Font instances resolve identically without hover font shifts.
            primaryFont = "System";

            if (families.contains("JetBrains Mono")) {
                monoFont = "JetBrains Mono";
            } else if (families.contains("SF Mono")) {
                monoFont = "SF Mono";
            } else if (families.contains("Consolas")) {
                monoFont = "Consolas";
            }
        } catch (Exception e) {
            // Fallback to defaults
        }
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static Font fontRegular(double size) {
        return Font.font(primaryFont, size);
    }

    public static Font fontBold(double size) {
        return Font.font(primaryFont, javafx.scene.text.FontWeight.BOLD, size);
    }

    public static Font fontMono(double size, boolean bold) {
        return Font.font(monoFont, bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, size);
    }

    public static Node getIcon(String name, double size, Color color) {
        return TiketinIcons.getIcon(name, size, color);
    }

    public static VBox createCardPanel() {
        return createCardPanel(18, 18);
    }

    public static VBox createCardPanel(double hPad, double vPad) {
        VBox box = new VBox();
        box.getStyleClass().add("card-surface");
        box.setPadding(new Insets(vPad, hPad, vPad, hPad));
        return box;
    }

    public static StackPane createDoubleBezelCard(Region innerContent, double outerPad, double innerPad) {
        StackPane card = new StackPane(innerContent);
        card.getStyleClass().add("bezel-inner");
        card.setPadding(new Insets(innerPad));
        return card;
    }

    public static AnimatedButton createPrimaryButton(String text) {
        return new AnimatedButton(text, "arrow-right");
    }

    public static AnimatedButton createPrimaryButton(String text, String iconName) {
        return new AnimatedButton(text, iconName);
    }

    public static AnimatedButton createSecondaryButton(String text) {
        AnimatedButton btn = new AnimatedButton(text, "");
        btn.getStyleClass().setAll("btn-secondary");
        return btn;
    }

    public static AnimatedButton createSecondaryButton(String text, String iconName) {
        AnimatedButton btn = new AnimatedButton(text, iconName);
        btn.getStyleClass().setAll("btn-secondary");
        return btn;
    }

    public static AnimatedButton createDangerButton(String text) {
        AnimatedButton btn = new AnimatedButton(text, "trash");
        btn.getStyleClass().setAll("btn-danger");
        return btn;
    }

    public static AnimatedButton createDangerButton(String text, String iconName) {
        AnimatedButton btn = new AnimatedButton(text, iconName);
        btn.getStyleClass().setAll("btn-danger");
        return btn;
    }

    public static Button createSquareIconButton(String iconName, double size, String tooltipText) {
        Button btn = new Button();
        btn.setMinSize(size, size);
        btn.setPrefSize(size, size);
        btn.setMaxSize(size, size);
        btn.setAlignment(Pos.CENTER);
        btn.setGraphic(TiketinIcons.getIcon(iconName, Math.max(14, size * 0.4), TEXT_PRIMARY));
        btn.getStyleClass().setAll("icon-btn-square");
        if (tooltipText != null && !tooltipText.isEmpty()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
    }

    public static TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #20252A; -fx-prompt-text-fill: #788088; -fx-border-color: #E3E6E2; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;");
        return tf;
    }

    public static PasswordField createPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #20252A; -fx-prompt-text-fill: #788088; -fx-border-color: #E3E6E2; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;");
        return pf;
    }

    public static VBox createFieldGroup(String labelText, Region field) {
        Label lbl = new Label(labelText);
        lbl.setFont(fontBold(12));
        lbl.setTextFill(TEXT_SECONDARY);
        lbl.setStyle("-fx-text-fill: " + toHex(TEXT_SECONDARY) + ";");
        VBox group = new VBox(6, lbl, field);
        return group;
    }

    public static Label createBadge(String text, Color fg, Color bg) {
        Label badge = new Label(text);
        badge.setFont(fontBold(11));
        badge.setTextFill(fg);
        badge.setStyle("-fx-background-color: " + toHex(bg) + "; -fx-background-radius: 6px; -fx-padding: 3 8;");
        return badge;
    }

    public static Label createStatusBadge(String status) {
        if (status == null) status = "UNKNOWN";
        String upper = status.trim().toUpperCase();
        switch (upper) {
            case "APPROVED":
            case "SUCCESS":
            case "ACTIVE":
            case "COMPLETED":
            case "DISETUJUI":
            case "BERHASIL":
            case "AKTIF":
            case "CONFIRMED":
            case "VERIFIED":
                return createBadge(status, SUCCESS_FG, SUCCESS_BG);

            case "PENDING":
            case "WAITING":
            case "PROCESSING":
            case "MENUNGGU":
            case "MENUNGGU_VERIFIKASI":
                return createBadge(status, WARNING_FG, WARNING_BG);

            case "USED":
            case "TERPAKAI":
                return createBadge("terpakai", NEUTRAL_FG, NEUTRAL_BG);

            case "REJECTED":
            case "CANCELLED":
            case "FAILED":
            case "DANGER":
            case "DITOLAK":
            case "DIBATALKAN":
            case "HABIS":
                return createBadge(status, DANGER_FG, DANGER_BG);

            default:
                return createBadge(status, NEUTRAL_FG, NEUTRAL_BG);
        }
    }

    public static Label createCategoryBadge(String category) {
        return createBadge(category != null ? category : "General", TEXT_SECONDARY, BG_SUBTLE);
    }

    public static javafx.stage.Window resolveWindow(Object owner) {
        if (owner instanceof javafx.stage.Window) {
            return (javafx.stage.Window) owner;
        }
        if (owner instanceof javafx.scene.Node) {
            javafx.scene.Scene s = ((javafx.scene.Node) owner).getScene();
            if (s != null) return s.getWindow();
        }
        if (owner instanceof Scene) {
            return ((Scene) owner).getWindow();
        }
        return null;
    }

    public static void applyStylesheet(Scene scene) {
        if (scene == null) return;
        try {
            var cssUrl = TiketinTheme.class.getResource("/styles/tiketin.css");
            if (cssUrl != null) {
                String url = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(url)) {
                    scene.getStylesheets().add(url);
                }
            }
        } catch (Exception ignored) {}
    }

    public static <T> void runAsync(Callable<T> taskCallable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return taskCallable.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            if (onError != null) {
                onError.accept(task.getException());
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
