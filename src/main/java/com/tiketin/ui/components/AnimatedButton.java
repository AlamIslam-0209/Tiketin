package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinIcons;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class AnimatedButton extends Button {
    private Label textLabel;
    private StackPane iconHolder;
    private String iconName;

    public AnimatedButton(String text) {
        this(text, "arrow-right");
    }

    public AnimatedButton(String text, String iconName) {
        super(text);
        this.iconName = iconName;
        getStyleClass().add("btn-primary");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        textLabel = new Label(text);
        textLabel.textProperty().bind(textProperty());
        textLabel.textFillProperty().bind(textFillProperty());
        textLabel.fontProperty().bind(fontProperty());

        iconHolder = new StackPane();
        iconHolder.setAlignment(Pos.CENTER);
        iconHolder.setMinSize(16, 16);
        iconHolder.setPrefSize(16, 16);
        iconHolder.setMaxSize(16, 16);

        updateIconAndColor();

        HBox graphicBox = new HBox(8, textLabel, iconHolder);
        graphicBox.setAlignment(Pos.CENTER);
        setGraphic(graphicBox);

        getStyleClass().addListener((ListChangeListener<String>) c -> updateIconAndColor());
        hoverProperty().addListener((obs, oldV, newV) -> updateIconAndColor());

        setupAnimations();
    }

    private Color resolveIconColor() {
        if (getStyleClass().contains("btn-secondary")) {
            return isHover() ? Color.web("#20252A") : Color.web("#525A62");
        } else if (getStyleClass().contains("btn-danger")) {
            return isHover() ? Color.web("#991B1B") : Color.web("#B91C1C");
        } else if (getStyleClass().contains("ticket-code-chip")) {
            return isHover() ? Color.web("#E06338") : Color.web("#687078");
        } else {
            return Color.WHITE;
        }
    }

    private void updateIconAndColor() {
        iconHolder.getChildren().clear();
        if (iconName != null && !iconName.isEmpty()) {
            Node icon = TiketinIcons.getIcon(iconName, 14, resolveIconColor());
            iconHolder.getChildren().add(icon);
            iconHolder.setVisible(true);
            iconHolder.setManaged(true);
        } else {
            iconHolder.setVisible(false);
            iconHolder.setManaged(false);
        }
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
        updateIconAndColor();
    }

    private void setupAnimations() {
        setOnMouseEntered(e -> {
            if (!getStyleClass().contains("btn-primary") && !getStyleClass().contains("btn-success")) {
                return;
            }
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(180),
                    new KeyValue(iconHolder.translateXProperty(), 3.0, Interpolator.SPLINE(0.22, 1, 0.36, 1))
                )
            );
            tl.play();
        });

        setOnMouseExited(e -> {
            if (iconHolder.getTranslateX() != 0) {
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(180),
                        new KeyValue(iconHolder.translateXProperty(), 0, Interpolator.SPLINE(0.22, 1, 0.36, 1))
                    )
                );
                tl.play();
            }
        });

        setOnMousePressed(e -> {
            if (getStyleClass().contains("btn-primary")) {
                setScaleX(0.985);
                setScaleY(0.985);
            }
        });
        setOnMouseReleased(e -> {
            if (getScaleX() != 1.0) {
                setScaleX(1.0);
                setScaleY(1.0);
            }
        });
    }
}
