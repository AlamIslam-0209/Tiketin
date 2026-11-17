package com.tiketin.ui.components;

import com.tiketin.model.Order;
import com.tiketin.model.Ticket;
import com.tiketin.ui.dialogs.ProofViewerDialog;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

public class TicketCardPanel extends VBox {
    private final Order order;
    private final List<Ticket> tickets;
    private final Runnable onStatusChanged;

    public TicketCardPanel(Order order, List<Ticket> tickets, Runnable onStatusChanged) {
        this.order = order;
        this.tickets = tickets;
        this.onStatusChanged = onStatusChanged;

        setSpacing(0);
        setMaxWidth(Double.MAX_VALUE);
        setStyle("-fx-background-color: #FFFFFF; " +
                 "-fx-border-color: #E3E6E2; " +
                 "-fx-border-width: 1; " +
                 "-fx-border-radius: 14; " +
                 "-fx-background-radius: 14;");

        VBox innerContent = new VBox(14);
        innerContent.setPadding(new Insets(18, 20, 18, 20));

        // Top Row: Order Code + Status Badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label orderCodeLbl = new Label("Pesanan #" + order.getOrderCode());
        orderCodeLbl.setFont(TiketinTheme.fontBold(15));
        orderCodeLbl.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_PRIMARY) + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        javafx.scene.Node statusBadge = TiketinTheme.createStatusBadge(order.getStatus());

        topRow.getChildren().addAll(orderCodeLbl, spacer, statusBadge);

        // Details info
        VBox infoBox = new VBox(5);
        Label eventLbl = new Label(order.getEventName() != null ? order.getEventName() : ("Event ID: " + order.getEventId()));
        eventLbl.setFont(TiketinTheme.fontBold(14));
        eventLbl.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_PRIMARY) + ";");

        Label qtyPriceLbl = new Label(order.getQuantity() + " Tiket • Total " + CurrencyUtil.formatRupiah(order.getTotalPrice())
                + (order.getPaymentMethod() != null && !order.getPaymentMethod().isBlank() ? " • " + order.getPaymentMethod() : ""));
        qtyPriceLbl.setFont(TiketinTheme.fontRegular(12.5));
        qtyPriceLbl.setStyle("-fx-text-fill: #525A62;");

        infoBox.getChildren().addAll(eventLbl, qtyPriceLbl);

        StringBuilder scheduleLoc = new StringBuilder();
        if (order.getEventDate() != null && !order.getEventDate().isBlank()) {
            scheduleLoc.append(DateUtil.formatTanggalId(order.getEventDate()));
        }
        if (order.getEventTime() != null && !order.getEventTime().isBlank()) {
            if (scheduleLoc.length() > 0) scheduleLoc.append(" • ");
            scheduleLoc.append(order.getEventTime());
        }
        if (order.getEventLocation() != null && !order.getEventLocation().isBlank()) {
            if (scheduleLoc.length() > 0) scheduleLoc.append(" • ");
            scheduleLoc.append(order.getEventLocation());
        }
        if (scheduleLoc.length() > 0) {
            Label metaLbl = new Label(scheduleLoc.toString());
            metaLbl.setFont(TiketinTheme.fontRegular(12));
            metaLbl.setStyle("-fx-text-fill: #687078;");
            infoBox.getChildren().add(metaLbl);
        }

        // Tickets Chips Container (stable font/padding across hover to eliminate bounce)
        VBox ticketsBox = new VBox(8);
        if (tickets != null && !tickets.isEmpty()) {
            Label tHeader = new Label("Kode E-Ticket (Klik untuk Salin)");
            tHeader.setFont(TiketinTheme.fontRegular(12));
            tHeader.setStyle("-fx-text-fill: #687078;");
            ticketsBox.getChildren().add(tHeader);

            FlowPane chipsRow = new FlowPane(8, 8);
            for (Ticket t : tickets) {
                Button chip = new Button(t.getTicketCode());
                chip.getStyleClass().setAll("ticket-code-chip");
                Node idleIcon = TiketinIcons.getIcon("copy", 12, Color.web("#687078"));
                Node hoverIcon = TiketinIcons.getIcon("copy", 12, Color.web("#E06338"));
                chip.setGraphic(idleIcon);
                chip.setGraphicTextGap(6);
                chip.hoverProperty().addListener((obs, oldV, hovered) -> chip.setGraphic(hovered ? hoverIcon : idleIcon));
                chip.setOnAction(e -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(t.getTicketCode());
                    Clipboard.getSystemClipboard().setContent(cc);
                    ToastNotification.info(getScene().getWindow(), "Kode tiket " + t.getTicketCode() + " disalin!");
                });
                chipsRow.getChildren().add(chip);
            }
            ticketsBox.getChildren().add(chipsRow);
        }

        // Action row: Clear visual CTA distinction between View Proof & Danger Cancel
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        if (order.getPaymentProof() != null && !order.getPaymentProof().isEmpty()) {
            AnimatedButton viewProofBtn = TiketinTheme.createSecondaryButton("Lihat Bukti", "eye");
            viewProofBtn.setOnAction(e -> ProofViewerDialog.showProof(getScene().getWindow(), order.getPaymentProof()));
            actionRow.getChildren().add(viewProofBtn);
        }

        String statusLower = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        if (statusLower.equals("menunggu") || statusLower.equals("pending")) {
            Label pendingInfo = new Label("Menunggu verifikasi pembayaran oleh penyelenggara");
            pendingInfo.setFont(TiketinTheme.fontRegular(12));
            pendingInfo.setStyle("-fx-text-fill: #B45309;");
            pendingInfo.setWrapText(true);
            actionRow.getChildren().add(pendingInfo);
        }

        innerContent.getChildren().addAll(topRow, infoBox);
        if (!ticketsBox.getChildren().isEmpty()) {
            innerContent.getChildren().add(ticketsBox);
        }
        if (!actionRow.getChildren().isEmpty()) {
            Region actionSep = new Region();
            actionSep.setPrefHeight(1);
            actionSep.setStyle("-fx-background-color: #EEF0ED;");
            innerContent.getChildren().addAll(actionSep, actionRow);
        }

        getChildren().add(innerContent);
    }

    public TicketCardPanel(Order order, List<Ticket> tickets) {
        this(order, tickets, null);
    }
}
