package com.tiketin.ui.views;

import com.tiketin.ui.MainFrame;
import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeView extends VBox {
    public HomeView(MainFrame mainFrame) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(48));
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(32));
        card.getStyleClass().add("card-editorial");
        Label t = new Label("Coming Soon");
        t.setFont(TiketinTheme.fontBold(18));
        t.setStyle("-fx-text-fill: #20252A;");
        Label s = new Label("Tugasnya Alip");
        s.setFont(TiketinTheme.fontRegular(13.5));
        s.setStyle("-fx-text-fill: #525A62;");
        s.setWrapText(true);
        card.getChildren().addAll(t, s);
        getChildren().add(card);
    }

    public void refreshData() {}
}
