package com.tiketin.ui.views;

import com.tiketin.model.Event;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class OrderCheckoutView extends VBox {
    private final MainFrame mainFrame;
    private final Label detailLbl = new Label("Tugasnya Alip");

    public OrderCheckoutView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(48));
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(28));
        card.getStyleClass().add("card-editorial");
        Label t = new Label("Coming Soon");
        t.setFont(TiketinTheme.fontBold(18));
        t.setStyle("-fx-text-fill: #20252A;");
        detailLbl.setFont(TiketinTheme.fontRegular(13.5));
        detailLbl.setStyle("-fx-text-fill: #525A62;");
        detailLbl.setWrapText(true);
        Button back = TiketinTheme.createSecondaryButton("Kembali", "arrow-left");
        back.setOnAction(e -> this.mainFrame.navigateTo(MainFrame.VIEW_HOME));
        card.getChildren().addAll(t, detailLbl, back);
        getChildren().add(card);
    }

    public void setEvent(Event event, Runnable onOrderSuccess) {
        detailLbl.setText("Tugasnya Alip");
    }
}
