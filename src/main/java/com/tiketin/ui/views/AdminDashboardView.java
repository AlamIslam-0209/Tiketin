package com.tiketin.ui.views;

import com.tiketin.dao.ApplicationDAO;
import com.tiketin.dao.EventDAO;
import com.tiketin.dao.UserDAO;
import com.tiketin.model.Application;
import com.tiketin.model.Event;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.CommandSearchBar;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.StatCardPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.dialogs.EventFormDialog;
import com.tiketin.ui.dialogs.RejectReasonDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin Dashboard View for managing organizer applications, user accounts, and global events.
 */
public class AdminDashboardView extends VBox {

    private final MainFrame mainFrame;
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final AtomicInteger loadSeq = new AtomicInteger(0);

    private StatCardPanel statPending;
    private StatCardPanel statUsers;
    private StatCardPanel statPublicEvents;
    private StatCardPanel statActiveOrganizers;

    private List<Application> cachedApps = List.of();
    private VBox applicationsContainer;

    private CommandSearchBar appSearchBox;
    private CommandSearchBar userSearchBox;
    private CommandSearchBar eventSearchBox;

    private ToggleButton filterAppAll;
    private ToggleButton filterAppPending;
    private ToggleButton filterAppProcessed;
    private String currentAppFilter = "MENUNGGU";

    private ToggleButton filterUserAll;
    private ToggleButton filterUserAdmin;
    private ToggleButton filterUserOrganizer;
    private ToggleButton filterUserUser;
    private String currentUserFilter = "ALL";

    private TableView<User> userTable;
    private ObservableList<User> userMasterList;
    private FilteredList<User> userFilteredList;

    private TableView<Event> eventTable;
    private ObservableList<Event> eventMasterList;
    private FilteredList<Event> eventFilteredList;
    private ComboBox<String> eventCategoryFilter;

    public AdminDashboardView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(18);
        setPadding(new Insets(30, 36, 32, 36));
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        User sessionUser = com.tiketin.util.SessionManager.getCurrentUser();
        if (sessionUser == null || !"admin".equalsIgnoreCase(sessionUser.getRole())) {
            Label deniedLbl = new Label("Akses ditolak. Panel ini khusus Administrator.");
            deniedLbl.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            getChildren().add(deniedLbl);
            return;
        }

        initUI();

        mainFrame.registerSearchFocusHandler(MainFrame.VIEW_ADMIN, () -> {
            if (appSearchBox != null) appSearchBox.focusInput();
        });
    }

    private void initUI() {
        PageHeaderPanel header = new PageHeaderPanel("Admin", null);
        Button refreshBtn = TiketinTheme.createSquareIconButton("refresh", 42, "Muat ulang data");
        refreshBtn.setOnAction(e -> refreshData());
        header.addAction(refreshBtn);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(14);
        statsGrid.setVgap(14);

        statPending = new StatCardPanel("0", "Menunggu", "clock");
        statUsers = new StatCardPanel("0", "Pengguna", "user");
        statPublicEvents = new StatCardPanel("0", "Acara Publik", "calendar");
        statActiveOrganizers = new StatCardPanel("0", "Organizer Aktif", "shield");

        statsGrid.add(statPending, 0, 0);
        statsGrid.add(statUsers, 1, 0);
        statsGrid.add(statPublicEvents, 2, 0);
        statsGrid.add(statActiveOrganizers, 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            statsGrid.getColumnConstraints().add(cc);
        }

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabApps = new Tab("Pengajuan", createApplicationsTab());
        Tab tabUsers = new Tab("Pengguna", createUsersTab());
        Tab tabEvents = new Tab("Acara", createEventsTab());

        tabPane.getTabs().addAll(tabApps, tabUsers, tabEvents);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabApps) {
                mainFrame.registerSearchFocusHandler(MainFrame.VIEW_ADMIN, () -> {
                    if (appSearchBox != null) appSearchBox.focusInput();
                });
            } else if (newTab == tabUsers) {
                mainFrame.registerSearchFocusHandler(MainFrame.VIEW_ADMIN, () -> {
                    if (userSearchBox != null) userSearchBox.focusInput();
                });
            } else if (newTab == tabEvents) {
                mainFrame.registerSearchFocusHandler(MainFrame.VIEW_ADMIN, () -> {
                    if (eventSearchBox != null) eventSearchBox.focusInput();
                });
            }
        });

        getChildren().addAll(header, statsGrid, tabPane);
    }

    private VBox createApplicationsTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));
        box.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        appSearchBox = new CommandSearchBar("Cari nama pemohon, organisasi, atau proposal event...");
        HBox.setHgrow(appSearchBox, Priority.ALWAYS);
        appSearchBox.setOnSearchChange(s -> populateApplicationsList(cachedApps));

        ToggleGroup group = new ToggleGroup();
        filterAppAll = new ToggleButton("Semua");
        filterAppPending = new ToggleButton("Menunggu");
        filterAppProcessed = new ToggleButton("Diproses");
        filterAppAll.getStyleClass().setAll("filter-pill");
        filterAppPending.getStyleClass().setAll("filter-pill");
        filterAppProcessed.getStyleClass().setAll("filter-pill");
        filterAppAll.setToggleGroup(group);
        filterAppPending.setToggleGroup(group);
        filterAppProcessed.setToggleGroup(group);
        filterAppPending.setSelected(true);

        filterAppAll.setOnAction(e -> { currentAppFilter = "ALL"; populateApplicationsList(cachedApps); });
        filterAppPending.setOnAction(e -> { currentAppFilter = "MENUNGGU"; populateApplicationsList(cachedApps); });
        filterAppProcessed.setOnAction(e -> { currentAppFilter = "PROCESSED"; populateApplicationsList(cachedApps); });

        HBox pills = new HBox(6, filterAppAll, filterAppPending, filterAppProcessed);
        pills.setAlignment(Pos.CENTER_LEFT);

        topBar.getChildren().addAll(appSearchBox, pills);

        applicationsContainer = new VBox(12);

        ScrollPane scroll = new ScrollPane(applicationsContainer);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(topBar, scroll);
        return box;
    }

    private VBox createUsersTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));
        box.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        userSearchBox = new CommandSearchBar("Cari nama, username, atau organisasi...");
        HBox.setHgrow(userSearchBox, Priority.ALWAYS);
        userSearchBox.setOnSearchChange(s -> filterUsers());

        ToggleGroup group = new ToggleGroup();
        filterUserAll = new ToggleButton("Semua");
        filterUserAdmin = new ToggleButton("Admin");
        filterUserOrganizer = new ToggleButton("Organizer");
        filterUserUser = new ToggleButton("User");
        filterUserAll.getStyleClass().setAll("filter-pill");
        filterUserAdmin.getStyleClass().setAll("filter-pill");
        filterUserOrganizer.getStyleClass().setAll("filter-pill");
        filterUserUser.getStyleClass().setAll("filter-pill");
        filterUserAll.setToggleGroup(group);
        filterUserAdmin.setToggleGroup(group);
        filterUserOrganizer.setToggleGroup(group);
        filterUserUser.setToggleGroup(group);
        filterUserAll.setSelected(true);

        filterUserAll.setOnAction(e -> { currentUserFilter = "ALL"; filterUsers(); });
        filterUserAdmin.setOnAction(e -> { currentUserFilter = "ADMIN"; filterUsers(); });
        filterUserOrganizer.setOnAction(e -> { currentUserFilter = "ORGANIZER"; filterUsers(); });
        filterUserUser.setOnAction(e -> { currentUserFilter = "USER"; filterUsers(); });

        HBox pills = new HBox(6, filterUserAll, filterUserAdmin, filterUserOrganizer, filterUserUser);
        pills.setAlignment(Pos.CENTER_LEFT);

        topBar.getChildren().addAll(userSearchBox, pills);

        userTable = new TableView<>();
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(userTable, Priority.ALWAYS);
        userTable.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-radius: 10px;");

        TableColumn<User, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(60);
        colId.setMinWidth(45);

        TableColumn<User, String> colUsername = new TableColumn<>("Username");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(120);

        TableColumn<User, String> colFullName = new TableColumn<>("Nama Lengkap");
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colFullName.setPrefWidth(160);

        TableColumn<User, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                } else {
                    setGraphic(TiketinTheme.createStatusBadge(role));
                }
            }
        });
        colRole.setPrefWidth(105);

        TableColumn<User, String> colOrg = new TableColumn<>("Organisasi");
        colOrg.setCellValueFactory(new PropertyValueFactory<>("organization"));
        colOrg.setPrefWidth(160);

        TableColumn<User, String> colDate = new TableColumn<>("Bergabung");
        colDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatTanggalId(cell.getValue().getCreatedAt())
        ));
        colDate.setPrefWidth(130);

        userTable.getColumns().addAll(colId, colUsername, colFullName, colRole, colOrg, colDate);

        userMasterList = FXCollections.observableArrayList();
        userFilteredList = new FilteredList<>(userMasterList, p -> true);
        userTable.setItems(userFilteredList);

        box.getChildren().addAll(topBar, userTable);
        return box;
    }

    private void filterUsers() {
        String text = userSearchBox != null ? userSearchBox.getText().toLowerCase().trim() : "";
        userFilteredList.setPredicate(user -> {
            if (!"ALL".equals(currentUserFilter)) {
                if (!currentUserFilter.equalsIgnoreCase(user.getRole())) return false;
            }
            if (!text.isEmpty()) {
                boolean match = (user.getUsername() != null && user.getUsername().toLowerCase().contains(text)) ||
                        (user.getFullName() != null && user.getFullName().toLowerCase().contains(text)) ||
                        (user.getOrganization() != null && user.getOrganization().toLowerCase().contains(text));
                if (!match) return false;
            }
            return true;
        });
    }

    private VBox createEventsTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));
        box.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        eventSearchBox = new CommandSearchBar("Cari nama acara, penyelenggara, atau lokasi...");
        HBox.setHgrow(eventSearchBox, Priority.ALWAYS);
        eventSearchBox.setOnSearchChange(s -> filterEvents());

        eventCategoryFilter = new ComboBox<>();
        eventCategoryFilter.getItems().addAll("Semua Kategori", "Konser", "Seminar", "Wisata", "Festival", "Olahraga", "Lainnya");
        eventCategoryFilter.setValue("Semua Kategori");
        eventCategoryFilter.getStyleClass().add("combo-box-editorial");
        eventCategoryFilter.setPrefHeight(40);
        eventCategoryFilter.setOnAction(e -> filterEvents());

        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        AnimatedButton editBtn = TiketinTheme.createSecondaryButton("Edit", "edit");
        editBtn.setOnAction(e -> onEditSelectedEvent());
        AnimatedButton deleteBtn = TiketinTheme.createDangerButton("Hapus", "trash");
        deleteBtn.setOnAction(e -> onDeleteSelectedEvent());

        actionButtons.getChildren().addAll(editBtn, deleteBtn);
        toolbar.getChildren().addAll(eventSearchBox, eventCategoryFilter, actionButtons);
        box.getChildren().addAll(toolbar);

        eventTable = new TableView<>();
        eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(eventTable, Priority.ALWAYS);
        eventTable.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-radius: 10px;");

        TableColumn<Event, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(55);
        colId.setMinWidth(42);

        TableColumn<Event, String> colTitle = new TableColumn<>("Nama Acara");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTitle.setPrefWidth(190);

        TableColumn<Event, String> colOrg = new TableColumn<>("Penyelenggara");
        colOrg.setCellValueFactory(new PropertyValueFactory<>("ownerOrg"));
        colOrg.setPrefWidth(140);

        TableColumn<Event, String> colCategory = new TableColumn<>("Kategori");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) setGraphic(null);
                else setGraphic(TiketinTheme.createCategoryBadge(cat));
            }
        });
        colCategory.setPrefWidth(110);

        TableColumn<Event, String> colDate = new TableColumn<>("Jadwal");
        colDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatTanggalId(cell.getValue().getDate())
        ));
        colDate.setPrefWidth(120);

        TableColumn<Event, String> colLocation = new TableColumn<>("Lokasi");
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colLocation.setPrefWidth(150);

        TableColumn<Event, String> colPrice = new TableColumn<>("Harga Mulai");
        colPrice.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getPrice() <= 0 ? "Gratis" : CurrencyUtil.formatRupiah(cell.getValue().getPrice())
        ));
        colPrice.setPrefWidth(105);

        TableColumn<Event, Integer> colSlots = new TableColumn<>("Sisa");
        colSlots.setCellValueFactory(new PropertyValueFactory<>("slots"));
        colSlots.setMaxWidth(70);

        eventTable.getColumns().addAll(colId, colTitle, colOrg, colCategory, colDate, colLocation, colPrice, colSlots);
        eventTable.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Event ev = row.getItem();
                    if (EventFormDialog.showDialog(mainFrame, ev, ev.getOwnerId())) {
                        refreshData();
                    }
                }
            });
            return row;
        });

        eventMasterList = FXCollections.observableArrayList();
        eventFilteredList = new FilteredList<>(eventMasterList, p -> true);
        eventTable.setItems(eventFilteredList);

        box.getChildren().add(eventTable);
        return box;
    }

    private void filterEvents() {
        String query = eventSearchBox != null ? eventSearchBox.getText().toLowerCase().trim() : "";
        String catFilter = eventCategoryFilter != null ? eventCategoryFilter.getValue() : "Semua Kategori";

        eventFilteredList.setPredicate(event -> {
            if (!"Semua Kategori".equals(catFilter)) {
                String evCat = event.getCategory() != null ? event.getCategory().toLowerCase() : "";
                if (!evCat.equalsIgnoreCase(catFilter) && !evCat.contains(catFilter.toLowerCase())) {
                    return false;
                }
            }
            if (!query.isEmpty()) {
                boolean match = (event.getName() != null && event.getName().toLowerCase().contains(query)) ||
                        (event.getLocation() != null && event.getLocation().toLowerCase().contains(query)) ||
                        (event.getOwnerOrg() != null && event.getOwnerOrg().toLowerCase().contains(query));
                if (!match) return false;
            }
            return true;
        });
    }

    private void onEditSelectedEvent() {
        Event sel = eventTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            ToastNotification.error(mainFrame, "Pilih acara yang ingin diedit.");
            return;
        }
        if (EventFormDialog.showDialog(mainFrame, sel, sel.getOwnerId())) {
            refreshData();
        }
    }

    private void onDeleteSelectedEvent() {
        Event sel = eventTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            ToastNotification.error(mainFrame, "Pilih acara yang ingin dihapus.");
            return;
        }
        User adminUser = SessionManager.getCurrentUser();
        if (adminUser == null || !"admin".equalsIgnoreCase(adminUser.getRole())) {
            ToastNotification.error(mainFrame, "Hanya Administrator yang dapat menghapus acara dari pusat admin.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Hapus acara '" + sel.getName() + "'?", ButtonType.YES, ButtonType.NO);
        if (mainFrame.getScene() != null && mainFrame.getScene().getWindow() != null) {
            alert.initOwner(mainFrame.getScene().getWindow());
        }
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                TiketinTheme.runAsync(() -> eventDAO.deleteEvent(sel.getId(), adminUser.getId()), ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        ToastNotification.success(mainFrame, "Acara berhasil dihapus.");
                        refreshData();
                    } else {
                        ToastNotification.error(mainFrame, "Acara tidak ditemukan atau gagal dihapus.");
                    }
                }, err -> {
                    ToastNotification.error(mainFrame, "Gagal menghapus acara: " + err.getMessage());
                });
            }
        });
    }

    public void refreshData() {
        if (statPending == null) return;
        final int seq = loadSeq.incrementAndGet();

        TiketinTheme.runAsync(() -> {
            List<Application> apps = applicationDAO.getAllApplications();
            int pendingCount = (int) apps.stream().filter(a -> "pending".equalsIgnoreCase(a.getStatus())).count();
            List<User> users = userDAO.getAllUsers();
            List<Event> events = eventDAO.getAllPublishedEvents();
            long activeOrgCount = users.stream().filter(u -> "organizer".equalsIgnoreCase(u.getRole())).count();

            return new Object[]{pendingCount, users.size(), events.size(), (int) activeOrgCount, apps, users, events};
        }, res -> {
            if (seq != loadSeq.get()) return;
            Object[] data = (Object[]) res;
            int pendingCount = (int) data[0];
            int userCount = (int) data[1];
            int eventCount = (int) data[2];
            int activeOrgCount = (int) data[3];

            statPending.setValue(String.valueOf(pendingCount));
            statUsers.setValue(String.valueOf(userCount));
            statPublicEvents.setValue(String.valueOf(eventCount));
            statActiveOrganizers.setValue(String.valueOf(activeOrgCount));

            @SuppressWarnings("unchecked")
            List<Application> apps = data[4] != null ? (List<Application>) data[4] : List.of();
            cachedApps = apps;
            populateApplicationsList(apps);

            @SuppressWarnings("unchecked")
            List<User> users = data[5] != null ? (List<User>) data[5] : List.of();
            userMasterList.setAll(users);
            filterUsers();

            @SuppressWarnings("unchecked")
            List<Event> events = data[6] != null ? (List<Event>) data[6] : List.of();
            eventMasterList.setAll(events);
            filterEvents();
        }, err -> {
            if (seq != loadSeq.get()) return;
            ToastNotification.error(mainFrame, "Gagal memuat data admin: " + err.getMessage());
        });
    }

    private void populateApplicationsList(List<Application> apps) {
        applicationsContainer.getChildren().clear();

        String query = appSearchBox != null ? appSearchBox.getText().toLowerCase().trim() : "";

        for (Application app : apps) {
            boolean isPending = "pending".equalsIgnoreCase(app.getStatus());

            if ("MENUNGGU".equals(currentAppFilter) && !isPending) continue;
            if ("PROCESSED".equals(currentAppFilter) && isPending) continue;

            if (!query.isEmpty()) {
                boolean match = (app.getFullName() != null && app.getFullName().toLowerCase().contains(query)) ||
                        (app.getOrganization() != null && app.getOrganization().toLowerCase().contains(query)) ||
                        (app.getEventName() != null && app.getEventName().toLowerCase().contains(query));
                if (!match) continue;
            }

            VBox cardContent = new VBox(10);
            cardContent.setPadding(new Insets(16, 20, 16, 20));
            cardContent.getStyleClass().add("card-editorial");

            HBox headerBox = new HBox(12);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label orgLabel = new Label(app.getOrganization());
            orgLabel.setFont(TiketinTheme.fontBold(15));
            orgLabel.setStyle("-fx-text-fill: #20252A;");

            Label badge = TiketinTheme.createStatusBadge(app.getStatus());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label dateLbl = new Label(DateUtil.formatTanggalId(app.getCreatedAt()));
            dateLbl.setFont(TiketinTheme.fontRegular(12));
            dateLbl.setStyle("-fx-text-fill: #525A62;");

            headerBox.getChildren().addAll(orgLabel, badge, spacer, dateLbl);

            Label applicantLbl = new Label("Pemohon: " + app.getFullName() + " (User ID: " + app.getUserId() + ")  |  Kontak: " + app.getContact());
            applicantLbl.setFont(TiketinTheme.fontRegular(13));
            applicantLbl.setStyle("-fx-text-fill: #525A62;");
            applicantLbl.setWrapText(true);

            Label proposalLbl = new Label("Acara Perdana: " + app.getEventName() + " (" + app.getEventCategory() + ") — " + app.getEventLocation() + " @ " + DateUtil.formatTanggalId(app.getEventDate()));
            proposalLbl.setFont(TiketinTheme.fontRegular(13));
            proposalLbl.setStyle("-fx-text-fill: #20252A;");
            proposalLbl.setWrapText(true);

            cardContent.getChildren().addAll(headerBox, applicantLbl, proposalLbl);

            if (app.getReason() != null && !app.getReason().isBlank()) {
                Label rejectLbl = new Label("Alasan Penolakan: " + app.getReason());
                rejectLbl.setFont(TiketinTheme.fontRegular(12));
                rejectLbl.setStyle("-fx-text-fill: #B91C1C;");
                rejectLbl.setWrapText(true);
                cardContent.getChildren().add(rejectLbl);
            }

            if (isPending) {
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(Pos.CENTER_RIGHT);

                User adminUser = SessionManager.getCurrentUser();
                int adminId = adminUser != null ? adminUser.getId() : -1;

                AnimatedButton approveBtn = TiketinTheme.createPrimaryButton("Setujui", "check");
                approveBtn.setOnAction(e -> {
                    TiketinTheme.runAsync(() -> {
                        applicationDAO.approveApplication(app.getId(), adminId);
                        return null;
                    }, res -> {
                        ToastNotification.success(mainFrame, "Pengajuan " + app.getOrganization() + " disetujui.");
                        refreshData();
                    }, err -> {
                        ToastNotification.error(mainFrame, "Gagal menyetujui: " + err.getMessage());
                    });
                });

                AnimatedButton rejectBtn = TiketinTheme.createDangerButton("Tolak", "close");
                rejectBtn.setOnAction(e -> {
                    String reason = RejectReasonDialog.showDialog(mainFrame, app.getOrganization());
                    if (reason != null && !reason.trim().isEmpty()) {
                        TiketinTheme.runAsync(() -> {
                            applicationDAO.rejectApplication(app.getId(), adminId, reason.trim());
                            return null;
                        }, res -> {
                            ToastNotification.success(mainFrame, "Pengajuan ditolak.");
                            refreshData();
                        }, err -> {
                            ToastNotification.error(mainFrame, "Gagal menolak: " + err.getMessage());
                        });
                    }
                });

                actionBox.getChildren().addAll(approveBtn, rejectBtn);
                cardContent.getChildren().add(actionBox);
            }

            applicationsContainer.getChildren().add(cardContent);
        }

        if (applicationsContainer.getChildren().isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(32));
            emptyCard.getStyleClass().add("card-editorial");
            Label emptyLbl = new Label("Tidak ada pengajuan ditemukan.");
            emptyLbl.setFont(TiketinTheme.fontRegular(13.5));
            emptyLbl.setStyle("-fx-text-fill: #525A62;");
            emptyCard.getChildren().add(emptyLbl);
            applicationsContainer.getChildren().add(emptyCard);
        }
    }
}
