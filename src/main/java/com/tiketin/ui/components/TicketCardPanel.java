package com.tiketin.ui.components;

import com.tiketin.model.Order;
import com.tiketin.model.Ticket;
import javafx.scene.layout.VBox;
import java.util.List;

public class TicketCardPanel extends VBox {
    public TicketCardPanel(Order order, List<Ticket> tickets, Runnable onCancelled, Runnable onProofUploaded) {}
}
