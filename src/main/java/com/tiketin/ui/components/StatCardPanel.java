package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class StatCardPanel extends StackPane {
    private Label valueLabel;
    private Label labelLabel;

    public StatCardPanel(String value, String label) {
        this(value, label, null);
    }

    public StatCardPanel(String value, String label, String iconName) {
        getStyleClass().add("stat-card");
        setPadding(new Insets(14, 16, 14, 16));

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        if (iconName != null && !iconName.isEmpty()) {
            Node icon = TiketinTheme.getIcon(iconName, 18, TiketinTheme.ACCENT);
            StackPane iconContainer = new StackPane(icon);
            iconContainer.setPrefSize(32, 32);
            iconContainer.setMinSize(32, 32);
            iconContainer.setMaxSize(32, 32);
            iconContainer.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.ACCENT_SOFT) + "; -fx-background-radius: 8px;");
            topRow.getChildren().add(iconContainer);
        }

        valueLabel = new Label();
        valueLabel.setTextFill(TiketinTheme.TEXT_PRIMARY);
        valueLabel.setStyle("-fx-text-fill: #20252A;");
        setValue(value);
        topRow.getChildren().add(valueLabel);

        labelLabel = new Label(label != null ? label : "");
        labelLabel.setFont(TiketinTheme.fontRegular(12.5));
        labelLabel.setTextFill(TiketinTheme.TEXT_SECONDARY);
        labelLabel.setStyle("-fx-text-fill: #525A62;");

        content.getChildren().addAll(topRow, labelLabel);
        getChildren().add(content);
    }

    public void setValue(String value) {
        String text = value != null ? value : "0";
        int len = text.length();
        double fontSize = len >= 11 ? 18.0 : (len >= 7 ? 20.5 : 24.0);
        valueLabel.setFont(TiketinTheme.fontBold(fontSize));
        valueLabel.setText(text);
    }

    public void setLabel(String label) {
        labelLabel.setText(label != null ? label : "");
    }
}
