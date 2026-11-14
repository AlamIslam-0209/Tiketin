package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PageHeaderPanel extends HBox {
    private Label titleLabel;
    private Label subtitleLabel;
    private Label eyebrowLabel;
    private HBox actionsBox;

    public PageHeaderPanel(String title, String subtitle) {
        this(null, title, subtitle);
    }

    public PageHeaderPanel(String eyebrow, String title, String subtitle) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        if (eyebrow != null && !eyebrow.isBlank()) {
            eyebrowLabel = new Label(eyebrow.toUpperCase());
            eyebrowLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E06338;");
            textBox.getChildren().add(eyebrowLabel);
        }

        titleLabel = new Label(title != null ? title : "");
        titleLabel.setFont(TiketinTheme.fontBold(28));
        titleLabel.setTextFill(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setStyle("-fx-text-fill: #20252A;");
        textBox.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            subtitleLabel = new Label(subtitle);
            subtitleLabel.setFont(TiketinTheme.fontRegular(14));
            subtitleLabel.setTextFill(TiketinTheme.TEXT_SECONDARY);
            subtitleLabel.setStyle("-fx-text-fill: #525A62;");
            textBox.getChildren().add(subtitleLabel);
        }

        actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(textBox, actionsBox);
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
    }

    public void setSubtitle(String subtitle) {
        if (subtitleLabel == null) {
            subtitleLabel = new Label();
            subtitleLabel.setFont(TiketinTheme.fontRegular(14));
            subtitleLabel.setTextFill(TiketinTheme.TEXT_SECONDARY);
            ((VBox) titleLabel.getParent()).getChildren().add(subtitleLabel);
        }
        subtitleLabel.setText(subtitle != null ? subtitle : "");
    }

    public void addAction(Node actionNode) {
        if (actionNode != null) {
            actionsBox.getChildren().add(actionNode);
        }
    }
}
