package com.tiketin.ui.views;

import com.tiketin.dao.OrderDAO;
import com.tiketin.dao.TicketDAO;
import com.tiketin.model.Order;
import com.tiketin.model.Ticket;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.*;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.stream.Collectors;

public class MyTicketsView extends VBox {
    private final MainFrame mainFrame;
    private final OrderDAO orderDAO = new OrderDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private VBox activeTicketsBox;
    private VBox historyOrdersBox;
    private HBox underlineTabBar;
    private CommandSearchBar searchBar;
    private StatCardPanel statTotalOrders;
    private StatCardPanel statActiveTickets;
    private StatCardPanel statPendingVerif;
    private List<Order> allOrders = List.of();
    private java.util.Map<Integer, List<Ticket>> ticketsByOrderMap = new java.util.HashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger loadSeq = new java.util.concurrent.atomic.AtomicInteger(0);
    private String selectedTabStatus = "Semua";

    public MyTicketsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(20);
        setPadding(new Insets(32, 40, 40, 40));
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        if (!SessionManager.isLoggedIn()) {
            VBox guestPrompt = new VBox(15);
            guestPrompt.setAlignment(Pos.CENTER);
            guestPrompt.setPadding(new Insets(60));
            guestPrompt.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");
            Label msg = new Label("Silakan login terlebih dahulu untuk melihat tiket dan riwayat pesanan Anda.");
            msg.setFont(TiketinTheme.fontRegular(14));
            msg.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_SECONDARY) + ";");
            AnimatedButton loginBtn = TiketinTheme.createPrimaryButton("Masuk ke Akun", "user");
            loginBtn.setOnAction(e -> mainFrame.openAuth(MainFrame.VIEW_MY_TICKETS, () -> mainFrame.navigateTo(MainFrame.VIEW_MY_TICKETS)));
            guestPrompt.getChildren().addAll(msg, loginBtn);
            getChildren().add(guestPrompt);
            return;
        }

        // Header
        PageHeaderPanel header = new PageHeaderPanel("Tiket Saya", null);
        Button refreshBtn = TiketinTheme.createSquareIconButton("refresh", 42, "Muat ulang tiket");
        refreshBtn.setOnAction(e -> refreshData());
        header.addAction(refreshBtn);

        // Stat Cards Row
        HBox statsRow = new HBox(15);
        statTotalOrders = new StatCardPanel("0", "Total Pesanan", "ticket");
        statActiveTickets = new StatCardPanel("0", "Tiket Aktif", "check");
        statPendingVerif = new StatCardPanel("0", "Menunggu", "clock");
        statTotalOrders.setMaxWidth(Double.MAX_VALUE);
        statActiveTickets.setMaxWidth(Double.MAX_VALUE);
        statPendingVerif.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statTotalOrders, Priority.ALWAYS);
        HBox.setHgrow(statActiveTickets, Priority.ALWAYS);
        HBox.setHgrow(statPendingVerif, Priority.ALWAYS);
        statsRow.getChildren().addAll(statTotalOrders, statActiveTickets, statPendingVerif);

        // Search bar
        searchBar = new CommandSearchBar("Cari kode order, acara, atau kode tiket...");
        searchBar.setOnSearchChange(q -> filterAndRender());
        mainFrame.registerSearchFocusHandler(MainFrame.VIEW_MY_TICKETS, () -> searchBar.focusInput());

        // Underline Tab Bar
        underlineTabBar = new HBox(20);
        underlineTabBar.setAlignment(Pos.CENTER_LEFT);
        underlineTabBar.getStyleClass().add("underline-tab-bar");

        // Content Area Container
        VBox contentContainer = new VBox(15);
        ScrollPane scrollContent = new ScrollPane(contentContainer);
        scrollContent.setFitToWidth(true);
        VBox.setVgrow(scrollContent, Priority.ALWAYS);

        // We will render items directly into contentContainer based on selectedTabStatus
        this.activeTicketsBox = contentContainer;

        getChildren().addAll(header, statsRow, searchBar, underlineTabBar, scrollContent);
    }

    public void refreshData() {
        if (!SessionManager.isLoggedIn() || activeTicketsBox == null) return;
        int userId = SessionManager.getCurrentUser().getId();
        final int seq = loadSeq.incrementAndGet();

        TiketinTheme.runAsync(() -> {
            List<Order> orders = orderDAO.getOrdersByUser(userId);
            java.util.Map<Integer, List<Ticket>> map = new java.util.HashMap<>();
            if (orders != null) {
                for (Order o : orders) {
                    map.put(o.getId(), ticketDAO.getTicketsByOrder(o.getId()));
                }
            }
            return new Object[]{orders != null ? orders : List.of(), map};
        }, res -> {
            if (seq != loadSeq.get()) return;
            Object[] arr = (Object[]) res;
            @SuppressWarnings("unchecked")
            List<Order> orders = (List<Order>) arr[0];
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, List<Ticket>> map = (java.util.Map<Integer, List<Ticket>>) arr[1];
            allOrders = orders;
            ticketsByOrderMap = map;
            updateStats();
            rebuildUnderlineTabs();
            filterAndRender();
        }, err -> {
            if (seq != loadSeq.get()) return;
            ToastNotification.error(mainFrame, "Gagal memuat pesanan: " + err.getMessage());
        });
    }

    private void updateStats() {
        int total = allOrders.size();
        long active = allOrders.stream().filter(o -> {
            String s = o.getStatus() != null ? o.getStatus().toLowerCase() : "";
            return s.equals("berhasil") || s.equals("disetujui") || s.equals("approved") || s.equals("confirmed");
        }).count();
        long pending = allOrders.stream().filter(o -> {
            String s = o.getStatus() != null ? o.getStatus().toLowerCase() : "";
            return s.equals("menunggu") || s.equals("pending");
        }).count();

        statTotalOrders.setValue(String.valueOf(total));
        statActiveTickets.setValue(String.valueOf(active));
        statPendingVerif.setValue(String.valueOf(pending));
    }

    private void rebuildUnderlineTabs() {
        underlineTabBar.getChildren().clear();

        long allCount = allOrders.size();
        long menungguCount = allOrders.stream().filter(o -> "menunggu".equalsIgnoreCase(o.getStatus()) || "pending".equalsIgnoreCase(o.getStatus())).count();
        long berhasilCount = allOrders.stream().filter(o -> "berhasil".equalsIgnoreCase(o.getStatus()) || "disetujui".equalsIgnoreCase(o.getStatus())).count();
        long terpakaiCount = allOrders.stream().filter(o -> "terpakai".equalsIgnoreCase(o.getStatus()) || "used".equalsIgnoreCase(o.getStatus())).count();
        long dibatalkanCount = allOrders.stream().filter(o -> "dibatalkan".equalsIgnoreCase(o.getStatus()) || "ditolak".equalsIgnoreCase(o.getStatus())).count();

        underlineTabBar.getChildren().addAll(
            createUnderlineTab("Semua (" + allCount + ")", "Semua"),
            createUnderlineTab("Menunggu (" + menungguCount + ")", "menunggu"),
            createUnderlineTab("Berhasil (" + berhasilCount + ")", "berhasil"),
            createUnderlineTab("Terpakai (" + terpakaiCount + ")", "terpakai"),
            createUnderlineTab("Dibatalkan (" + dibatalkanCount + ")", "dibatalkan")
        );
    }

    private Button createUnderlineTab(String text, String statusKey) {
        Button tab = new Button(text);
        boolean isActive = selectedTabStatus.equalsIgnoreCase(statusKey);
        tab.getStyleClass().setAll(isActive ? "underline-tab-active" : "underline-tab");

        tab.setOnAction(e -> {
            selectedTabStatus = statusKey;
            rebuildUnderlineTabs();
            filterAndRender();
        });
        return tab;
    }

    private void filterAndRender() {
        if (activeTicketsBox == null) return;
        String query = searchBar != null && searchBar.getText() != null ? searchBar.getText().toLowerCase().trim() : "";

        List<Order> filtered = allOrders.stream().filter(o -> {
            // Status tab filter
            if (!"Semua".equalsIgnoreCase(selectedTabStatus)) {
                String s = o.getStatus() != null ? o.getStatus().toLowerCase() : "";
                if ("menunggu".equalsIgnoreCase(selectedTabStatus)) {
                    if (!s.contains("menunggu") && !s.contains("pending")) return false;
                } else if ("berhasil".equalsIgnoreCase(selectedTabStatus)) {
                    if (!s.contains("berhasil") && !s.contains("disetujui") && !s.contains("approved") && !s.contains("confirmed")) return false;
                } else if ("terpakai".equalsIgnoreCase(selectedTabStatus)) {
                    if (!s.contains("terpakai") && !s.contains("used")) return false;
                } else if ("dibatalkan".equalsIgnoreCase(selectedTabStatus)) {
                    if (!s.contains("dibatalkan") && !s.contains("ditolak") && !s.contains("cancelled")) return false;
                } else if (!s.contains(selectedTabStatus.toLowerCase())) {
                    return false;
                }
            }
            // Search query filter
            if (!query.isEmpty()) {
                boolean matchCode = o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(query);
                boolean matchEvent = o.getEventName() != null && o.getEventName().toLowerCase().contains(query);
                boolean matchTickets = o.getTicketCodes() != null && o.getTicketCodes().toLowerCase().contains(query);
                boolean matchStatus = o.getStatus() != null && o.getStatus().toLowerCase().contains(query);
                if (!matchCode && !matchEvent && !matchTickets && !matchStatus) return false;
            }
            return true;
        }).collect(Collectors.toList());

        activeTicketsBox.getChildren().clear();
        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40));
            Label empty = new Label("Tidak ada tiket atau pesanan yang ditemukan.");
            empty.setFont(TiketinTheme.fontRegular(13.5));
            empty.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_MUTED) + ";");
            emptyState.getChildren().add(empty);
            activeTicketsBox.getChildren().add(emptyState);
        } else {
            for (Order ord : filtered) {
                List<Ticket> tickets = ticketsByOrderMap.getOrDefault(ord.getId(), List.of());
                activeTicketsBox.getChildren().add(new TicketCardPanel(ord, tickets, this::refreshData));
            }
        }
    }
}
