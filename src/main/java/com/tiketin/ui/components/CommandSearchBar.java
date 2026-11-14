package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinIcons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

public class CommandSearchBar extends HBox {
    private TextField textField;
    private Button clearBtn;
    private Consumer<String> onSearchChange;

    public CommandSearchBar(String placeholder) {
        getStyleClass().add("cmd-search");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setMinHeight(46);
        setPrefHeight(46);

        Node searchIcon = TiketinIcons.getIcon("search", 16, Color.web("#687078"));

        textField = new TextField();
        textField.setPromptText(placeholder != null ? placeholder : "Cari acara, kota, atau penyelenggara");
        textField.getStyleClass().add("cmd-search-input");
        HBox.setHgrow(textField, Priority.ALWAYS);

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                getStyleClass().add("cmd-search-focused");
            } else {
                getStyleClass().remove("cmd-search-focused");
            }
        });

        clearBtn = new Button();
        clearBtn.setGraphic(TiketinIcons.getIcon("close", 12, Color.web("#687078")));
        clearBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        clearBtn.setVisible(false);
        clearBtn.setManaged(false);
        clearBtn.setOnAction(e -> setText(""));

        String os = System.getProperty("os.name");
        String shortcutText = (os != null && os.toLowerCase().contains("mac")) ? "⌘ K" : "Ctrl K";
        Label shortcutBadge = new Label(shortcutText);
        shortcutBadge.getStyleClass().add("cmd-shortcut-badge");

        getChildren().addAll(searchIcon, textField, clearBtn, shortcutBadge);

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = newVal != null && !newVal.isEmpty();
            clearBtn.setVisible(hasText);
            clearBtn.setManaged(hasText);
            if (onSearchChange != null) {
                onSearchChange.accept(newVal);
            }
        });
    }

    public TextField getTextField() {
        return textField;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public void setOnSearchChange(Consumer<String> consumer) {
        this.onSearchChange = consumer;
    }

    public void focusInput() {
        textField.requestFocus();
        textField.selectAll();
    }
}
