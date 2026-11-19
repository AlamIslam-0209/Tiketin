package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Penampil 1 foto acara dalam bingkai 4:3 non-cropped (otomatis fit) dengan klik untuk perbesar (lightbox).
 */
public class EventImageViewerPanel extends VBox {

    private final StackPane stageFrame;
    private final ImageView mainView;
    private final Canvas fallbackCanvas;
    private final HBox zoomBadge;

    private String currentPath;
    private String fallbackCategory = "Umum";
    private Runnable onOpenLightbox;

    public EventImageViewerPanel() {
        setSpacing(0);
        setMaxWidth(Double.MAX_VALUE);

        stageFrame = new StackPane();
        stageFrame.setMinWidth(0);
        stageFrame.setPrefWidth(0);
        stageFrame.setMaxWidth(Double.MAX_VALUE);
        EventImageSupport.bindFourThreeHeight(stageFrame, 300.0, 220.0, 440.0);
        stageFrame.setStyle(
                "-fx-background-color: " + EventImageSupport.MATTE_BG_HEX + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #E3E6E2;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;"
        );
        EventImageSupport.applyRoundedClip(stageFrame, 13.5);

        fallbackCanvas = new Canvas(400, 300);
        fallbackCanvas.setManaged(false);

        mainView = EventImageSupport.createFittedImageView(stageFrame, 0);

        zoomBadge = new HBox(6);
        zoomBadge.setAlignment(Pos.CENTER);
        zoomBadge.setPadding(new Insets(5, 11, 5, 11));
        zoomBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zoomBadge.setStyle("-fx-background-color: rgba(24,24,27,0.72); -fx-background-radius: 999;");
        zoomBadge.setMouseTransparent(true);
        Label zoomLbl = new Label("Klik untuk perbesar");
        zoomLbl.setFont(TiketinTheme.fontBold(11));
        zoomLbl.setStyle("-fx-text-fill: #FFFFFF;");
        zoomBadge.getChildren().addAll(TiketinIcons.getIcon("eye", 12, Color.WHITE), zoomLbl);
        StackPane.setAlignment(zoomBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(zoomBadge, new Insets(0, 12, 12, 0));

        stageFrame.getChildren().addAll(fallbackCanvas, mainView, zoomBadge);

        Runnable redrawFallback = () -> {
            if (!fallbackCanvas.isVisible()) return;
            double w = stageFrame.getWidth();
            double h = stageFrame.getHeight();
            if (w > 0 && h > 0) {
                fallbackCanvas.setWidth(w);
                fallbackCanvas.setHeight(h);
                EventCardPanel.drawCategoryVectorArt(fallbackCanvas.getGraphicsContext2D(), w, h, fallbackCategory);
            }
        };
        stageFrame.widthProperty().addListener((obs, o, n) -> redrawFallback.run());
        stageFrame.heightProperty().addListener((obs, o, n) -> redrawFallback.run());

        stageFrame.setOnMouseClicked(e -> {
            if (onOpenLightbox != null && currentPath != null && mainView.getImage() != null) {
                onOpenLightbox.run();
            }
        });

        getChildren().add(stageFrame);
    }

    public void setImage(String storedPath, String category) {
        this.currentPath = (storedPath != null && !storedPath.trim().isEmpty()) ? storedPath.trim() : null;
        this.fallbackCategory = category != null && !category.isBlank() ? category.trim() : "Umum";
        render();
    }

    public void setOnOpenLightbox(Runnable onOpenLightbox) {
        this.onOpenLightbox = onOpenLightbox;
    }

    private void render() {
        Image img = currentPath != null ? EventImageSupport.loadStoredEventImage(currentPath, 1280, 960) : null;
        boolean hasPhoto = img != null && !img.isError();

        fallbackCanvas.setVisible(!hasPhoto);
        mainView.setVisible(hasPhoto);
        mainView.setImage(hasPhoto ? img : null);
        zoomBadge.setVisible(hasPhoto);
        stageFrame.setCursor(hasPhoto ? Cursor.HAND : Cursor.DEFAULT);

        if (!hasPhoto) {
            String catLower = fallbackCategory.toLowerCase(Locale.ROOT);
            stageFrame.setStyle(
                    "-fx-background-color: " + EventCardPanel.resolveBannerBg(catLower) + ";" +
                    "-fx-background-radius: 14; -fx-border-color: #E3E6E2; -fx-border-radius: 14; -fx-border-width: 1;"
            );
            double w = stageFrame.getWidth() > 0 ? stageFrame.getWidth() : 400;
            double h = stageFrame.getHeight() > 0 ? stageFrame.getHeight() : 300;
            fallbackCanvas.setWidth(w);
            fallbackCanvas.setHeight(h);
            EventCardPanel.drawCategoryVectorArt(fallbackCanvas.getGraphicsContext2D(), w, h, catLower);
        } else {
            stageFrame.setStyle(
                    "-fx-background-color: " + EventImageSupport.MATTE_BG_HEX + ";" +
                    "-fx-background-radius: 14; -fx-border-color: #E3E6E2; -fx-border-radius: 14; -fx-border-width: 1;"
            );
        }
    }
}
