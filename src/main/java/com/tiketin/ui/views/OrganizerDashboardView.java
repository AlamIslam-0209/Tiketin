package com.tiketin.ui.views;

import com.tiketin.dao.EventDAO;
import com.tiketin.dao.OrderDAO;
import com.tiketin.dao.PaymentDAO;
import com.tiketin.model.Event;
import com.tiketin.model.Order;
import com.tiketin.model.Payment;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.*;
import com.tiketin.ui.dialogs.EventFormDialog;
import com.tiketin.ui.dialogs.ProofViewerDialog;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import com.tiketin.util.EventSortUtil;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Pure JavaFX VBox studio dashboard for organizer events, order verification, and bank/QRIS accounts.
 */
public class OrganizerDashboardView extends VBox {

    private final MainFrame mainFrame;
    private final TabPane tabPane;
    private final AtomicInteger loadSeq = new AtomicInteger(0);

    // Cached data for instant local search/pill filtering
    private List<Event> cachedEvents = List.of();
    private List<Order> cachedOrders = List.of();

    // Events tab components
    private VBox eventsContainer;
    private CommandSearchBar eventSearch;

    // Orders tab components
    private VBox ordersContainer;
    private CommandSearchBar orderSearch;
    private String currentOrderFilter = "Semua";

    // Payments tab components
    private VBox paymentsContainer;
    private ComboBox<String> bankCombo;
    private TextField otherBankField;
    private TextField accountNumberField;
    private TextField accountNameField;

    // Stat cards
    private StatCardPanel cardTotalEvents;
    private StatCardPanel cardTotalOrders;
    private StatCardPanel cardPendingOrders;
    private StatCardPanel cardVerifiedRevenue;

    public OrganizerDashboardView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(20);
        setPadding(new Insets(30, 36, 32, 36));
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        // Header — concise desktop hierarchy
        PageHeaderPanel header = new PageHeaderPanel("Kelola Acara", null);
        Button refreshBtn = TiketinTheme.createSquareIconButton("refresh", 42, "Muat ulang data");
        refreshBtn.setOnAction(e -> refreshData());
        AnimatedButton createEventBtn = TiketinTheme.createPrimaryButton("Buat Acara", "plus");
        createEventBtn.setOnAction(e -> {
            User user = SessionManager.getCurrentUser();
            if (user == null) {
                mainFrame.openAuth(MainFrame.VIEW_ORGANIZER, () -> mainFrame.navigateTo(MainFrame.VIEW_ORGANIZER));
                return;
            }
            boolean saved = EventFormDialog.showDialog(mainFrame, null, user.getId());
            if (saved) {
                refreshData();
            }
        });
        header.addAction(refreshBtn);
        header.addAction(createEventBtn);
        getChildren().add(header);

        // Stats Row
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(14);

        cardTotalEvents = new StatCardPanel("0", "Total Acara", "calendar");
        cardTotalOrders = new StatCardPanel("0", "Pesanan Masuk", "ticket");
        cardPendingOrders = new StatCardPanel("0", "Menunggu", "clock");
        cardVerifiedRevenue = new StatCardPanel("Rp 0", "Pendapatan", "sparkles");

        statsGrid.add(cardTotalEvents, 0, 0);
        statsGrid.add(cardTotalOrders, 1, 0);
        statsGrid.add(cardPendingOrders, 2, 0);
        statsGrid.add(cardVerifiedRevenue, 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            statsGrid.getColumnConstraints().add(cc);
        }
        getChildren().add(statsGrid);

        // TabPane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabEvents = new Tab("Acara", buildEventsTabContent());
        Tab tabOrders = new Tab("Pesanan", buildOrdersTabContent());
        Tab tabPayments = new Tab("Rekening", buildPaymentsTabContent());

        tabPane.getTabs().addAll(tabEvents, tabOrders, tabPayments);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        getChildren().add(tabPane);

        // Register search focus handler for Ctrl+K / Cmd+K
        mainFrame.registerSearchFocusHandler(MainFrame.VIEW_ORGANIZER, () -> {
            int idx = tabPane.getSelectionModel().getSelectedIndex();
            if (idx == 0 && eventSearch != null) {
                eventSearch.focusInput();
            } else if (idx == 1 && orderSearch != null) {
                orderSearch.focusInput();
            }
        });
    }

    private Region buildEventsTabContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(18, 0, 0, 0));
        root.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        eventSearch = new CommandSearchBar("Cari nama acara atau lokasi...");
        eventSearch.setOnSearchChange(txt -> populateEventsList(cachedEvents, txt));

        eventsContainer = new VBox(12);

        ScrollPane scroll = new ScrollPane(eventsContainer);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(eventSearch, scroll);
        return root;
    }

    private Region buildOrdersTabContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(18, 0, 0, 0));
        root.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        orderSearch = new CommandSearchBar("Cari kode pesanan, pembeli, atau acara...");
        orderSearch.setOnSearchChange(txt -> populateOrdersList(cachedOrders, txt, currentOrderFilter));
        HBox.setHgrow(orderSearch, Priority.ALWAYS);

        // Filter Pills
        HBox pillsBox = new HBox(8);
        pillsBox.setAlignment(Pos.CENTER_RIGHT);
        String[] filters = {"Semua", "Menunggu", "Berhasil", "Terpakai", "Dibatalkan"};
        for (String f : filters) {
            Button pill = new Button(f);
            pill.getStyleClass().setAll(f.equals("Semua") ? "filter-pill-active" : "filter-pill");
            pill.setOnAction(e -> {
                currentOrderFilter = f;
                for (Node n : pillsBox.getChildren()) {
                    if (n instanceof Button) {
                        n.getStyleClass().setAll("filter-pill");
                    }
                }
                pill.getStyleClass().setAll("filter-pill-active");
                populateOrdersList(cachedOrders, orderSearch.getText(), currentOrderFilter);
            });
            pillsBox.getChildren().add(pill);
        }

        topBar.getChildren().addAll(orderSearch, pillsBox);

        ordersContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(ordersContainer);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(topBar, scroll);
        return root;
    }

    private Region buildPaymentsTabContent() {
        // Two-column split layout: Left = Tambah Rekening | Right = Rekening Aktif
        GridPane splitPane = new GridPane();
        splitPane.setHgap(20);
        splitPane.setVgap(16);
        splitPane.setPadding(new Insets(18, 0, 8, 0));
        splitPane.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setPercentWidth(42);
        leftCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setPercentWidth(58);
        rightCol.setHgrow(Priority.ALWAYS);
        splitPane.getColumnConstraints().addAll(leftCol, rightCol);

        // Left: Add Account Form Card
        VBox formCard = new VBox(14);
        formCard.setPadding(new Insets(20));
        formCard.getStyleClass().add("card-editorial");
        formCard.setMaxHeight(Region.USE_PREF_SIZE);

        Label formTitle = new Label("Tambah Rekening");
        formTitle.setFont(TiketinTheme.fontBold(15));
        formTitle.setStyle("-fx-text-fill: #20252A;");

        bankCombo = new ComboBox<>();
        bankCombo.getItems().addAll("BCA", "BRI", "Mandiri", "BNI", "QRIS", "GoPay", "OVO", "DANA", "Lainnya");
        bankCombo.getSelectionModel().select(0);
        bankCombo.getStyleClass().add("combo-box-editorial");
        bankCombo.setMaxWidth(Double.MAX_VALUE);

        otherBankField = TiketinTheme.createTextField("Nama Bank / E-Wallet Lainnya");
        otherBankField.setVisible(false);
        otherBankField.setManaged(false);

        bankCombo.setOnAction(e -> {
            boolean isOther = "Lainnya".equals(bankCombo.getValue());
            otherBankField.setVisible(isOther);
            otherBankField.setManaged(isOther);
        });

        accountNumberField = TiketinTheme.createTextField("Nomor Rekening / Virtual Account / ID");
        accountNameField = TiketinTheme.createTextField("Nama Pemilik Rekening");

        AnimatedButton addAccountBtn = TiketinTheme.createPrimaryButton("Simpan Rekening", "plus");
        addAccountBtn.setMaxWidth(Double.MAX_VALUE);
        addAccountBtn.setOnAction(e -> handleAddPaymentAccount());

        formCard.getChildren().addAll(
                formTitle,
                TiketinTheme.createFieldGroup("Bank / E-Wallet", bankCombo),
                otherBankField,
                TiketinTheme.createFieldGroup("Nomor Rekening / ID", accountNumberField),
                TiketinTheme.createFieldGroup("Atas Nama", accountNameField),
                addAccountBtn
        );

        // Right: Active Accounts List Panel
        VBox rightPanel = new VBox(12);
        rightPanel.setPadding(new Insets(20));
        rightPanel.getStyleClass().add("card-editorial");
        VBox.setVgrow(rightPanel, Priority.ALWAYS);

        Label listTitle = new Label("Rekening Aktif");
        listTitle.setFont(TiketinTheme.fontBold(15));
        listTitle.setStyle("-fx-text-fill: #20252A;");

        paymentsContainer = new VBox(10);
        ScrollPane listScroll = new ScrollPane(paymentsContainer);
        listScroll.setFitToWidth(true);
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        rightPanel.getChildren().addAll(listTitle, listScroll);

        GridPane.setValignment(formCard, javafx.geometry.VPos.TOP);
        GridPane.setVgrow(rightPanel, Priority.ALWAYS);
        splitPane.add(formCard, 0, 0);
        splitPane.add(rightPanel, 1, 0);

        return splitPane;
    }

    public void refreshData() {
        if (!SessionManager.isLoggedIn()) {
            return;
        }
        User user = SessionManager.getCurrentUser();
        if (!"organizer".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole())) {
            return;
        }
        final int seq = loadSeq.incrementAndGet();

        TiketinTheme.runAsync(() -> {
            EventDAO eventDAO = new EventDAO();
            OrderDAO orderDAO = new OrderDAO();
            PaymentDAO paymentDAO = new PaymentDAO();

            List<Event> events = eventDAO.getEventsByOwner(user.getId());
            List<Order> orders = orderDAO.getOrdersByOrganizer(user.getId());
            List<Payment> payments = paymentDAO.getPaymentsByOrganizer(user.getId());

            return new Object[]{events, orders, payments};
        }, result -> {
            if (seq != loadSeq.get()) return;
            Object[] res = (Object[]) result;
            @SuppressWarnings("unchecked")
            List<Event> events = res[0] != null ? (List<Event>) res[0] : List.of();
            @SuppressWarnings("unchecked")
            List<Order> orders = res[1] != null ? (List<Order>) res[1] : List.of();
            @SuppressWarnings("unchecked")
            List<Payment> payments = res[2] != null ? (List<Payment>) res[2] : List.of();

            cachedEvents = events;
            cachedOrders = orders;

            updateMetrics(events, orders);
            populateEventsList(events, eventSearch != null ? eventSearch.getText() : "");
            populateOrdersList(orders, orderSearch != null ? orderSearch.getText() : "", currentOrderFilter);
            populatePaymentsList(payments);
        }, err -> {
            if (seq != loadSeq.get()) return;
            ToastNotification.error(mainFrame, "Gagal memuat data organizer: " + err.getMessage());
        });
    }

    private void updateMetrics(List<Event> events, List<Order> orders) {
        int totalEvents = events.size();
        int totalOrders = orders.size();
        long pendingCount = orders.stream().filter(o -> {
            String s = o.getStatus() != null ? o.getStatus().toLowerCase() : "";
            return s.contains("pending") || s.contains("menunggu");
        }).count();

        long verifiedRevenue = orders.stream().filter(o -> {
            String s = o.getStatus() != null ? o.getStatus().toLowerCase() : "";
            return s.contains("berhasil") || s.contains("terpakai") || s.contains("used")
                    || s.contains("confirmed") || s.contains("approved");
        }).mapToLong(Order::getTotalPrice).sum();

        cardTotalEvents.setValue(String.valueOf(totalEvents));
        cardTotalOrders.setValue(String.valueOf(totalOrders));
        cardPendingOrders.setValue(String.valueOf(pendingCount));
        cardVerifiedRevenue.setValue(CurrencyUtil.formatRupiah(verifiedRevenue));
    }

    private void populateEventsList(List<Event> events, String query) {
        eventsContainer.getChildren().clear();
        String q = query != null ? query.toLowerCase().trim() : "";
        LocalDateTime nowDt = LocalDateTime.now();

        List<Event> filtered = events.stream().filter(ev -> {
            if (q.isEmpty()) return true;
            return (ev.getName() != null && ev.getName().toLowerCase().contains(q)) ||
                    (ev.getLocation() != null && ev.getLocation().toLowerCase().contains(q));
        }).collect(Collectors.toCollection(ArrayList::new));
        filtered.sort(EventSortUtil.defaultComparator(nowDt));

        if (filtered.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(32));
            emptyCard.getStyleClass().add("card-editorial");
            Label emptyLbl = new Label("Belum ada acara ditemukan.");
            emptyLbl.setFont(TiketinTheme.fontRegular(13.5));
            emptyLbl.setStyle("-fx-text-fill: #525A62;");
            emptyCard.getChildren().add(emptyLbl);
            eventsContainer.getChildren().add(emptyCard);
            return;
        }

        for (Event ev : filtered) {
            EventSortUtil.EventStatus status = EventSortUtil.getStatus(ev, nowDt);
            boolean isExpired = (status == EventSortUtil.EventStatus.BERAKHIR);

            VBox card = new VBox(10);
            card.setPadding(new Insets(16, 20, 16, 20));
            card.getStyleClass().add(isExpired ? "card-editorial-expired" : "card-editorial");

            HBox topRow = new HBox(14);
            topRow.setAlignment(Pos.CENTER_LEFT);

            if (ev.getImagePath() != null && !ev.getImagePath().isBlank()) {
                StackPane thumb = new StackPane();
                thumb.setMinSize(96, 72);
                thumb.setPrefSize(96, 72);
                thumb.setMaxSize(96, 72);
                thumb.setStyle("-fx-background-color: " + com.tiketin.ui.components.EventImageSupport.MATTE_BG_HEX +
                        "; -fx-background-radius: 10; -fx-border-color: #E3E6E2; -fx-border-radius: 10;");
                com.tiketin.ui.components.EventImageSupport.applyRoundedClip(thumb, 9);
                javafx.scene.image.ImageView iv = com.tiketin.ui.components.EventImageSupport.createFittedImageView(thumb, 4);
                iv.setImage(com.tiketin.ui.components.EventImageSupport.loadStoredEventImage(ev.getImagePath(), 320, 240));
                thumb.getChildren().add(iv);
                if (isExpired) {
                    thumb.setOpacity(0.58);
                }
                topRow.getChildren().add(thumb);
            }

            VBox infoBox = new VBox(6);
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = new Label(ev.getName());
            nameLbl.setFont(TiketinTheme.fontBold(15));
            nameLbl.setStyle("-fx-text-fill: #20252A;");
            nameLbl.setWrapText(true);
            titleRow.getChildren().addAll(
                    nameLbl,
                    TiketinTheme.createCategoryBadge(ev.getCategory()),
                    TiketinTheme.createStatusBadge(status.getLabel())
            );

            String dateStr = DateUtil.formatTanggalId(ev.getDate());
            Label metaLbl = new Label(
                    "Jadwal: " + dateStr + " • " + (ev.getTime() != null ? ev.getTime() : "-")
                            + "  |  Lokasi: " + (ev.getLocation() != null ? ev.getLocation() : "-")
                            + "  |  Harga: " + (ev.getPrice() <= 0 ? "Gratis" : CurrencyUtil.formatRupiah(ev.getPrice()))
                            + "  |  Sisa Kuota: " + ev.getSlots() + " tiket"
            );
            metaLbl.setFont(TiketinTheme.fontRegular(12.5));
            metaLbl.setStyle("-fx-text-fill: #525A62;");
            metaLbl.setWrapText(true);

            infoBox.getChildren().addAll(titleRow, metaLbl);
            if (isExpired) {
                infoBox.setOpacity(0.72);
            }
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox actionsBox = new HBox(8);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);
            actionsBox.setMinWidth(Region.USE_PREF_SIZE);

            Button editBtn = TiketinTheme.createSecondaryButton("Edit", "edit");
            editBtn.setOnAction(e -> {
                boolean saved = EventFormDialog.showDialog(mainFrame, ev);
                if (saved) refreshData();
            });

            Button deleteBtn = TiketinTheme.createDangerButton("Hapus", "trash");
            deleteBtn.setOnAction(e -> {
                User currentUser = SessionManager.getCurrentUser();
                if (currentUser == null) return;
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hapus acara \"" + ev.getName() + "\"?", ButtonType.YES, ButtonType.NO);
                if (mainFrame.getScene() != null && mainFrame.getScene().getWindow() != null) {
                    confirm.initOwner(mainFrame.getScene().getWindow());
                }
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        TiketinTheme.runAsync(() -> new EventDAO().deleteEvent(ev.getId(), currentUser.getId()), succ -> {
                            if (Boolean.TRUE.equals(succ)) {
                                ToastNotification.success(mainFrame, "Acara berhasil dihapus");
                                refreshData();
                            } else {
                                ToastNotification.error(mainFrame, "Acara tidak ditemukan atau gagal dihapus.");
                            }
                        }, err -> {
                            ToastNotification.error(mainFrame, "Gagal menghapus acara: " + err.getMessage());
                        });
                    }
                });
            });

            actionsBox.getChildren().addAll(editBtn, deleteBtn);
            topRow.getChildren().addAll(infoBox, actionsBox);
            card.getChildren().add(topRow);

            eventsContainer.getChildren().add(card);
        }
    }

    private void populateOrdersList(List<Order> orders, String query, String filter) {
        ordersContainer.getChildren().clear();
        String q = query != null ? query.toLowerCase().trim() : "";

        List<Order> filtered = orders.stream().filter(ord -> {
            if (!q.isEmpty()) {
                boolean matchCode = ord.getOrderCode() != null && ord.getOrderCode().toLowerCase().contains(q);
                boolean matchBuyer = ord.getBuyerName() != null && ord.getBuyerName().toLowerCase().contains(q);
                boolean matchEvent = ord.getEventName() != null && ord.getEventName().toLowerCase().contains(q);
                if (!matchCode && !matchBuyer && !matchEvent) return false;
            }

            if ("Semua".equals(filter)) return true;
            String status = ord.getStatus() != null ? ord.getStatus().toLowerCase() : "";
            if ("Menunggu".equals(filter)) {
                return status.contains("pending") || status.contains("menunggu");
            } else if ("Berhasil".equals(filter)) {
                return status.contains("berhasil") || status.contains("confirmed") || status.contains("approved");
            } else if ("Terpakai".equals(filter)) {
                return status.contains("terpakai") || status.contains("used");
            } else if ("Dibatalkan".equals(filter)) {
                return status.contains("cancelled") || status.contains("rejected") || status.contains("dibatalkan") || status.contains("ditolak");
            }
            return true;
        }).toList();

        if (filtered.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(32));
            emptyCard.getStyleClass().add("card-editorial");
            Label emptyLbl = new Label("Tidak ada pesanan ditemukan.");
            emptyLbl.setFont(TiketinTheme.fontRegular(13.5));
            emptyLbl.setStyle("-fx-text-fill: #525A62;");
            emptyCard.getChildren().add(emptyLbl);
            ordersContainer.getChildren().add(emptyCard);
            return;
        }

        for (Order ord : filtered) {
            VBox accordion = new VBox(0);
            accordion.getStyleClass().add("accordion-row");

            // Collapsed clickable summary bar
            HBox headerBar = new HBox(12);
            headerBar.setAlignment(Pos.CENTER_LEFT);
            headerBar.setPadding(new Insets(14, 18, 14, 18));
            headerBar.getStyleClass().add("accordion-header");

            Label codeLbl = new Label("#" + ord.getOrderCode());
            codeLbl.setFont(TiketinTheme.fontBold(13.5));
            codeLbl.setStyle("-fx-text-fill: #20252A;");

            Label dotSep = new Label("•");
            dotSep.setStyle("-fx-text-fill: #9AA29C;");

            Label eventLbl = new Label(ord.getEventName() != null ? ord.getEventName() : "Acara");
            eventLbl.setFont(TiketinTheme.fontBold(13.5));
            eventLbl.setStyle("-fx-text-fill: #20252A;");
            HBox.setHgrow(eventLbl, Priority.SOMETIMES);

            Node statusBadge = TiketinTheme.createStatusBadge(ord.getStatus());

            Region hSpacer = new Region();
            HBox.setHgrow(hSpacer, Priority.ALWAYS);

            Label quickQtyLbl = new Label(ord.getQuantity() + " tiket");
            quickQtyLbl.setFont(TiketinTheme.fontRegular(12.5));
            quickQtyLbl.setStyle("-fx-text-fill: #687078;");

            Label quickTotalLbl = new Label(CurrencyUtil.formatRupiah(ord.getTotalPrice()));
            quickTotalLbl.setFont(TiketinTheme.fontBold(13.5));
            quickTotalLbl.setStyle("-fx-text-fill: #20252A;");

            StackPane chevronHolder = new StackPane();
            chevronHolder.setMinSize(22, 22);
            chevronHolder.setPrefSize(22, 22);
            Node chevronDown = TiketinIcons.getIcon("chevron-down", 15, TiketinTheme.TEXT_SECONDARY);
            Node chevronUp = TiketinIcons.getIcon("chevron-up", 15, TiketinTheme.ACCENT);
            chevronHolder.getChildren().add(chevronDown);

            headerBar.getChildren().addAll(codeLbl, dotSep, eventLbl, statusBadge, hSpacer, quickQtyLbl, quickTotalLbl, chevronHolder);

            // Expandable detail pane (hidden until bar clicked)
            VBox detailsPane = new VBox(14);
            detailsPane.setPadding(new Insets(0, 18, 16, 18));
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);

            Region topSep = new Region();
            topSep.setPrefHeight(1);
            topSep.setStyle("-fx-background-color: #EEF0ED;");

            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(24);
            infoGrid.setVgap(10);
            for (int i = 0; i < 3; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(33.33);
                cc.setHgrow(Priority.ALWAYS);
                infoGrid.getColumnConstraints().add(cc);
            }

            String buyerDisplay = (ord.getBuyerName() != null ? ord.getBuyerName() : "-")
                    + (ord.getBuyerUsername() != null && !ord.getBuyerUsername().isBlank() ? " (@" + ord.getBuyerUsername() + ")" : "");
            String purchasedDisplay = formatOrderTimestamp(ord.getPurchasedAt());
            String scheduleDisplay = DateUtil.formatTanggalId(ord.getEventDate())
                    + (ord.getEventTime() != null && !ord.getEventTime().isBlank() ? " • " + ord.getEventTime() : "");
            String qtyDisplay = ord.getQuantity() + " Tiket"
                    + (ord.getUnitPrice() > 0 ? " (@ " + CurrencyUtil.formatRupiah(ord.getUnitPrice()) + ")" : "");
            String methodDisplay = (ord.getPaymentMethod() != null && !ord.getPaymentMethod().isBlank())
                    ? ord.getPaymentMethod()
                    : "-";
            String totalDisplay = CurrencyUtil.formatRupiah(ord.getTotalPrice());

            infoGrid.add(buildDetailCell("Pembeli", buyerDisplay), 0, 0);
            infoGrid.add(buildDetailCell("Waktu Pembelian", purchasedDisplay), 1, 0);
            infoGrid.add(buildDetailCell("Jadwal Acara", scheduleDisplay), 2, 0);
            infoGrid.add(buildDetailCell("Kuota & Harga", qtyDisplay), 0, 1);
            infoGrid.add(buildDetailCell("Metode Pembayaran", methodDisplay), 1, 1);
            infoGrid.add(buildDetailCell("Total Pembayaran", totalDisplay), 2, 1);

            detailsPane.getChildren().addAll(topSep, infoGrid);

            if (ord.getTicketCodes() != null && !ord.getTicketCodes().isBlank()) {
                detailsPane.getChildren().add(buildDetailCell("Kode E-Ticket", ord.getTicketCodes()));
            }

            HBox actionsRow = new HBox(10);
            actionsRow.setAlignment(Pos.CENTER_RIGHT);

            if (ord.getPaymentProof() != null && !ord.getPaymentProof().isEmpty()) {
                AnimatedButton proofBtn = TiketinTheme.createSecondaryButton("Lihat Bukti", "eye");
                proofBtn.setOnAction(e -> ProofViewerDialog.showProof(mainFrame, ord.getPaymentProof()));
                actionsRow.getChildren().add(proofBtn);
            }

            String st = ord.getStatus() != null ? ord.getStatus().toLowerCase() : "";
            if (st.contains("pending") || st.contains("menunggu")) {
                AnimatedButton rejectBtn = TiketinTheme.createDangerButton("Tolak", "close");
                rejectBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Tolak & batalkan pesanan #" + ord.getOrderCode() + "? Kuota tiket akan dikembalikan ke acara.",
                            ButtonType.YES, ButtonType.NO);
                    if (mainFrame.getScene() != null && mainFrame.getScene().getWindow() != null) {
                        confirm.initOwner(mainFrame.getScene().getWindow());
                    }
                    confirm.setTitle("Konfirmasi Penolakan Pesanan");
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            updateOrderStatus(ord.getId(), "dibatalkan");
                        }
                    });
                });

                AnimatedButton approveBtn = new AnimatedButton("Setujui", "check");
                approveBtn.getStyleClass().setAll("btn-success");
                approveBtn.setOnAction(e -> updateOrderStatus(ord.getId(), "berhasil"));

                actionsRow.getChildren().addAll(rejectBtn, approveBtn);
            }

            if (!actionsRow.getChildren().isEmpty()) {
                detailsPane.getChildren().add(actionsRow);
            }

            headerBar.setOnMouseClicked(e -> {
                boolean expand = !detailsPane.isVisible();
                detailsPane.setVisible(expand);
                detailsPane.setManaged(expand);
                chevronHolder.getChildren().setAll(expand ? chevronUp : chevronDown);
            });

            accordion.getChildren().addAll(headerBar, detailsPane);
            ordersContainer.getChildren().add(accordion);
        }
    }

    private VBox buildDetailCell(String label, String value) {
        VBox cell = new VBox(2);
        Label k = new Label(label);
        k.setFont(TiketinTheme.fontBold(11));
        k.setStyle("-fx-text-fill: #687078;");
        Label v = new Label(value != null && !value.isBlank() ? value : "-");
        v.setFont(TiketinTheme.fontRegular(13));
        v.setStyle("-fx-text-fill: #20252A;");
        v.setWrapText(true);
        cell.getChildren().addAll(k, v);
        return cell;
    }

    private String formatOrderTimestamp(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        String datePart = DateUtil.formatTanggalId(raw);
        int spaceIdx = raw.indexOf(' ');
        if (spaceIdx > 0 && spaceIdx + 5 < raw.length()) {
            String timePart = raw.substring(spaceIdx + 1, Math.min(raw.length(), spaceIdx + 6));
            return datePart + " • " + timePart;
        }
        return datePart;
    }

    private void updateOrderStatus(int orderId, String newStatus) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        TiketinTheme.runAsync(() -> {
            new OrderDAO().updateOrderStatus(orderId, user.getId(), newStatus);
            return true;
        }, success -> {
            ToastNotification.success(mainFrame, "Status pesanan berhasil diperbarui!");
            refreshData();
        }, err -> {
            ToastNotification.error(mainFrame, "Gagal memperbarui status: " + err.getMessage());
        });
    }

    private void handleAddPaymentAccount() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        if (!"organizer".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole())) {
            ToastNotification.error(mainFrame, "Hanya Organizer yang dapat mengelola rekening pembayaran.");
            return;
        }

        String bank = bankCombo.getValue();
        if ("Lainnya".equals(bank)) {
            bank = otherBankField.getText() != null ? otherBankField.getText().trim() : "";
        }
        String accNum = accountNumberField.getText() != null ? accountNumberField.getText().trim() : "";
        String accName = accountNameField.getText() != null ? accountNameField.getText().trim() : "";

        String err = ValidationUtil.validatePaymentAccount(bank, accNum, accName);
        if (err != null) {
            ToastNotification.error(mainFrame, err);
            return;
        }

        final String bankNameFinal = bank.trim();
        final String accNumFinal = ValidationUtil.normalizeAccountNumber(bankNameFinal, accNum);
        final String accNameFinal = accName;
        TiketinTheme.runAsync(() -> new PaymentDAO().addPayment(user.getId(), bankNameFinal, accNumFinal, accNameFinal), success -> {
            ToastNotification.success(mainFrame, "Rekening berhasil ditambahkan!");
            otherBankField.clear();
            accountNumberField.clear();
            accountNameField.clear();
            refreshData();
        }, errMsg -> {
            ToastNotification.error(mainFrame, "Gagal menambah rekening: " + errMsg.getMessage());
        });
    }

    private void populatePaymentsList(List<Payment> payments) {
        paymentsContainer.getChildren().clear();

        if (payments.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(24));
            emptyCard.getStyleClass().add("card-editorial");
            Label emptyLbl = new Label("Belum ada rekening terdaftar.");
            emptyLbl.setFont(TiketinTheme.fontRegular(13));
            emptyLbl.setStyle("-fx-text-fill: #525A62;");
            emptyCard.getChildren().add(emptyLbl);
            paymentsContainer.getChildren().add(emptyCard);
            return;
        }

        User user = SessionManager.getCurrentUser();
        for (Payment p : payments) {
            HBox itemRow = new HBox(15);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setPadding(new Insets(14, 18, 14, 18));
            itemRow.getStyleClass().add("card-editorial");

            VBox info = new VBox(4);
            Label bankLbl = new Label(p.getBankName() + " — " + p.getAccountNumber());
            bankLbl.setFont(TiketinTheme.fontBold(14));
            bankLbl.setStyle("-fx-text-fill: #20252A;");

            Label nameLbl = new Label("Atas Nama: " + p.getAccountName());
            nameLbl.setFont(TiketinTheme.fontRegular(12.5));
            nameLbl.setStyle("-fx-text-fill: #525A62;");

            info.getChildren().addAll(bankLbl, nameLbl);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button deleteBtn = TiketinTheme.createDangerButton("Hapus", "trash");
            deleteBtn.setOnAction(e -> {
                if (user == null) return;
                TiketinTheme.runAsync(() -> {
                    new PaymentDAO().deletePayment(p.getId(), user.getId());
                    return true;
                }, success -> {
                    ToastNotification.success(mainFrame, "Rekening dihapus");
                    refreshData();
                }, err -> {
                    ToastNotification.error(mainFrame, "Gagal menghapus rekening: " + err.getMessage());
                });
            });

            itemRow.getChildren().addAll(info, deleteBtn);
            paymentsContainer.getChildren().add(itemRow);
        }
    }
}
