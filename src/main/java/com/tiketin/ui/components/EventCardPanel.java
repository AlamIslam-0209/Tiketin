package com.tiketin.ui.components;

import com.tiketin.model.Event;
import com.tiketin.ui.MainFrame;
import javafx.scene.layout.VBox;

public class EventCardPanel extends VBox {
    public EventCardPanel(Event event, Runnable onBooked) {}
    public EventCardPanel(MainFrame mainFrame, Event event, Runnable onBooked) {}
}
