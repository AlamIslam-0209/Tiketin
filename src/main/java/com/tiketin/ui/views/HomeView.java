package com.tiketin.ui.views;

import com.tiketin.dao.EventDAO;
import com.tiketin.model.Event;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.CommandSearchBar;
import com.tiketin.ui.components.EventCardPanel;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.DateUtil;
import com.tiketin.util.EventSortUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HomeView extends StackPane {
    private final MainFrame mainFrame;
    private final EventDAO eventDAO = new EventDAO();
    private final GridPane gridPane;
    private final ScrollPane scrollPane;
    private final CommandSearchBar searchBar;
    private final HBox underlineTabBar;
    private final ComboBox<String> sortCombo;
    private final Button filterModalBtn;
    private final Button resetQuickBtn;
    private final Label resultCountLabel;

    // Floating in-frame specific filter popover modal (never alters underlying layout or opens a Stage)
    private final StackPane filterOverlay;
    private final VBox specificFilterPopover;
    private ComboBox<String> dateCombo;
    private HBox customDateBox;
    private DatePicker startPicker;
    private DatePicker endPicker;
    private ComboBox<String> priceCombo;
    private HBox customPriceBox;
    private TextField minPriceField;
    private TextField maxPriceField;
    private ComboBox<String> locCombo;
    private TextField locKeywordField;
    private ComboBox<String> orgCombo;
    private CheckBox availCheck;

    private final AtomicInteger loadSeq = new AtomicInteger(0);
    private List<Event> allEvents = List.of();
    private List<Event> currentFilteredEvents = List.of();
    private int currentCols = 3;
    private String selectedCategory = "Semua";

    // Advanced filter state
    private String filterDateMode = "Semua Tanggal";
    private LocalDate customStartDate = null;
    private LocalDate customEndDate = null;
    private String filterPriceMode = "Semua Harga";
    private long customMinPrice = -1;
    private long customMaxPrice = -1;
    private String filterLocation = "Semua Lokasi";
    private String filterLocationKeyword = "";
    private String filterOrganizer = "Semua Penyelenggara";
    private boolean filterAvailableOnly = false;
    private boolean drawerOpen = false;

    public HomeView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        VBox mainContent = new VBox(16);
        mainContent.setPadding(new Insets(28, 36, 28, 36));
        mainContent.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        // Header — concise single hierarchy
        PageHeaderPanel header = new PageHeaderPanel("Acara", null);
        Button refreshBtn = TiketinTheme.createSquareIconButton("refresh", 42, "Muat ulang acara");
        refreshBtn.setOnAction(e -> refreshData());
        header.addAction(refreshBtn);

        // Search Bar
        searchBar = new CommandSearchBar("Cari acara, lokasi, atau penyelenggara");
        searchBar.setOnSearchChange(query -> filterAndRender());
        mainFrame.registerSearchFocusHandler(MainFrame.VIEW_HOME, searchBar::focusInput);

        // Filter & Tab Bar
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.BOTTOM_LEFT);
        toolbar.getStyleClass().add("underline-tab-bar");

        underlineTabBar = new HBox(22);
        underlineTabBar.setAlignment(Pos.BOTTOM_LEFT);

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);

        HBox rightControls = new HBox(8);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        rightControls.setPadding(new Insets(0, 0, 8, 0));

        resetQuickBtn = new Button("Reset filter");
        resetQuickBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E06338; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 8;");
        resetQuickBtn.setVisible(false);
        resetQuickBtn.setManaged(false);
        resetQuickBtn.setOnAction(e -> {
            resetSpecificFilters();
            filterAndRender();
        });

        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Tanggal terdekat", "Harga terendah", "Harga tertinggi", "Kuota terbanyak");
        sortCombo.setValue("Tanggal terdekat");
        sortCombo.setOnAction(e -> filterAndRender());
        sortCombo.getStyleClass().add("combo-box-editorial");
        sortCombo.setPrefHeight(38);

        filterModalBtn = TiketinTheme.createSquareIconButton("filter", 38, "Filter acara");
        filterModalBtn.setOnAction(e -> toggleFilterDrawer(!drawerOpen));

        rightControls.getChildren().addAll(resetQuickBtn, sortCombo, filterModalBtn);
        toolbar.getChildren().addAll(underlineTabBar, tbSpacer, rightControls);

        // Result count label
        resultCountLabel = new Label("0 acara tersedia");
        resultCountLabel.setFont(TiketinTheme.fontRegular(13));
        resultCountLabel.setStyle("-fx-text-fill: #525A62;");

        // Responsive GridPane inside ScrollPane
        gridPane = new GridPane();
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setPadding(new Insets(2, 4, 24, 0));
        updateGridConstraints(currentCols);

        scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Responsive column reflow on viewport width changes
        scrollPane.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
            double w = newV.getWidth();
            if (w <= 0) return;
            int cols = w >= 860 ? 3 : (w >= 540 ? 2 : 1);
            if (cols != currentCols) {
                currentCols = cols;
                updateGridConstraints(cols);
                layoutCards();
            }
        });

        mainContent.getChildren().addAll(header, searchBar, toolbar, resultCountLabel, scrollPane);

        // Layer 1: In-frame floating filter popover overlay
        filterOverlay = new StackPane();
        filterOverlay.setStyle("-fx-background-color: rgba(32, 37, 42, 0.14);");
        filterOverlay.setVisible(false);
        filterOverlay.setManaged(false);
        filterOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == filterOverlay) {
                toggleFilterDrawer(false);
            }
        });

        specificFilterPopover = buildSpecificFilterPopover();
        specificFilterPopover.setOnMouseClicked(javafx.event.Event::consume);
        StackPane.setAlignment(specificFilterPopover, Pos.TOP_RIGHT);
        StackPane.setMargin(specificFilterPopover, new Insets(180, 36, 24, 24));
        filterOverlay.getChildren().add(specificFilterPopover);

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && drawerOpen) {
                toggleFilterDrawer(false);
                e.consume();
            }
        });

        getChildren().addAll(mainContent, filterOverlay);
    }

    private VBox buildSpecificFilterPopover() {
        VBox popover = new VBox(14);
        popover.getStyleClass().add("filter-popover");
        popover.setMinWidth(410);
        popover.setPrefWidth(430);
        popover.setMaxWidth(450);
        popover.setMaxHeight(Region.USE_PREF_SIZE);

        // Header row
        HBox topHeader = new HBox(10);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        Label drawerTitle = new Label("Filter Acara");
        drawerTitle.setFont(TiketinTheme.fontBold(15));
        drawerTitle.setStyle("-fx-text-fill: #20252A;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button closeDrawerBtn = TiketinTheme.createSquareIconButton("close", 28, "Tutup");
        closeDrawerBtn.setOnAction(e -> toggleFilterDrawer(false));

        topHeader.getChildren().addAll(drawerTitle, hSpacer, closeDrawerBtn);

        // 2x2 clean GridPane inside popover
        GridPane fieldsGrid = new GridPane();
        fieldsGrid.setHgap(14);
        fieldsGrid.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50.0);
            cc.setHgrow(Priority.ALWAYS);
            fieldsGrid.getColumnConstraints().add(cc);
        }

        // 1. Jadwal
        VBox dateBox = new VBox(6);
        dateCombo = new ComboBox<>();
        dateCombo.getItems().addAll("Semua Tanggal", "Hari Ini", "Minggu Ini", "Bulan Ini", "Custom");
        dateCombo.setValue(filterDateMode);
        dateCombo.getStyleClass().add("combo-box-editorial");
        dateCombo.setMaxWidth(Double.MAX_VALUE);

        customDateBox = new HBox(6);
        startPicker = new DatePicker(customStartDate);
        startPicker.setPromptText("Dari");
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker = new DatePicker(customEndDate);
        endPicker.setPromptText("Sampai");
        endPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(startPicker, Priority.ALWAYS);
        HBox.setHgrow(endPicker, Priority.ALWAYS);
        customDateBox.getChildren().addAll(startPicker, endPicker);
        customDateBox.setVisible(false);
        customDateBox.setManaged(false);

        dateCombo.setOnAction(e -> {
            boolean isCustom = "Custom".equals(dateCombo.getValue());
            customDateBox.setVisible(isCustom);
            customDateBox.setManaged(isCustom);
        });
        Label dateLbl = fieldTitle("Jadwal");
        dateBox.getChildren().addAll(dateLbl, dateCombo, customDateBox);

        // 2. Harga
        VBox priceBox = new VBox(6);
        priceCombo = new ComboBox<>();
        priceCombo.getItems().addAll(
                "Semua Harga",
                "Gratis",
                "Di bawah Rp 50.000",
                "Rp 50.000 - Rp 100.000",
                "Di atas Rp 50.000",
                "Di atas Rp 100.000",
                "Custom"
        );
        priceCombo.setValue(filterPriceMode);
        priceCombo.getStyleClass().add("combo-box-editorial");
        priceCombo.setMaxWidth(Double.MAX_VALUE);

        customPriceBox = new HBox(6);
        minPriceField = TiketinTheme.createTextField("Min Rp");
        maxPriceField = TiketinTheme.createTextField("Max Rp");
        HBox.setHgrow(minPriceField, Priority.ALWAYS);
        HBox.setHgrow(maxPriceField, Priority.ALWAYS);
        customPriceBox.getChildren().addAll(minPriceField, maxPriceField);
        customPriceBox.setVisible(false);
        customPriceBox.setManaged(false);

        priceCombo.setOnAction(e -> {
            boolean isCustom = "Custom".equals(priceCombo.getValue());
            customPriceBox.setVisible(isCustom);
            customPriceBox.setManaged(isCustom);
        });
        Label priceLbl = fieldTitle("Harga");
        priceBox.getChildren().addAll(priceLbl, priceCombo, customPriceBox);

        // 3. Lokasi
        VBox locBox = new VBox(6);
        locCombo = new ComboBox<>();
        locCombo.getItems().add("Semua Lokasi");
        locCombo.setValue("Semua Lokasi");
        locCombo.getStyleClass().add("combo-box-editorial");
        locCombo.setMaxWidth(Double.MAX_VALUE);

        locKeywordField = TiketinTheme.createTextField("Ketik kota atau venue...");
        Label locLbl = fieldTitle("Lokasi");
        locBox.getChildren().addAll(locLbl, locCombo, locKeywordField);

        // 4. Penyelenggara
        VBox orgBox = new VBox(6);
        orgCombo = new ComboBox<>();
        orgCombo.getItems().add("Semua Penyelenggara");
        orgCombo.setValue("Semua Penyelenggara");
        orgCombo.getStyleClass().add("combo-box-editorial");
        orgCombo.setMaxWidth(Double.MAX_VALUE);
        Label orgLbl = fieldTitle("Penyelenggara");
        orgBox.getChildren().addAll(orgLbl, orgCombo);

        fieldsGrid.add(dateBox, 0, 0);
        fieldsGrid.add(priceBox, 1, 0);
        fieldsGrid.add(locBox, 0, 1);
        fieldsGrid.add(orgBox, 1, 1);

        // Availability option + actions
        availCheck = new CheckBox("Hanya yang masih tersedia (> 0)");
        availCheck.setFont(TiketinTheme.fontRegular(12.5));
        availCheck.setStyle("-fx-text-fill: #20252A;");

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #EEF0ED;");

        HBox footerRow = new HBox(10);
        footerRow.setAlignment(Pos.CENTER_RIGHT);

        AnimatedButton resetBtn = TiketinTheme.createSecondaryButton("Reset", "refresh");
        resetBtn.setOnAction(e -> {
            resetSpecificFilters();
            filterAndRender();
        });

        AnimatedButton applyBtn = TiketinTheme.createPrimaryButton("Terapkan", "check");
        applyBtn.setOnAction(e -> applyDrawerFilters());

        footerRow.getChildren().addAll(resetBtn, applyBtn);

        popover.getChildren().addAll(topHeader, fieldsGrid, availCheck, separator, footerRow);
        return popover;
    }

    private Label fieldTitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(TiketinTheme.fontBold(12));
        lbl.setStyle("-fx-text-fill: #20252A;");
        return lbl;
    }

    private void toggleFilterDrawer(boolean open) {
        drawerOpen = open;
        if (open) {
            syncDrawerInputsFromState();
            positionFilterPopover();
        }
        filterOverlay.setVisible(open);
        filterOverlay.setManaged(open);
        updateFilterButtonVisual(isAnySpecificFilterActive());
    }

    private void positionFilterPopover() {
        try {
            Bounds btnBounds = filterModalBtn.localToScene(filterModalBtn.getBoundsInLocal());
            if (btnBounds != null && getScene() != null) {
                Point2D bottomRightInHome = sceneToLocal(btnBounds.getMaxX(), btnBounds.getMaxY());
                if (bottomRightInHome != null) {
                    double topMargin = Math.max(88.0, bottomRightInHome.getY() + 8.0);
                    double rightMargin = Math.max(24.0, getWidth() - bottomRightInHome.getX());
                    StackPane.setMargin(specificFilterPopover, new Insets(topMargin, rightMargin, 24, 24));
                    return;
                }
            }
        } catch (Exception ignored) {}
        StackPane.setMargin(specificFilterPopover, new Insets(180, 36, 24, 24));
    }

    private void syncDrawerInputsFromState() {
        populateFilterOptions();
        dateCombo.setValue(dateCombo.getItems().contains(filterDateMode) ? filterDateMode : "Semua Tanggal");
        boolean isCustomDate = "Custom".equals(dateCombo.getValue());
        startPicker.setValue(customStartDate);
        endPicker.setValue(customEndDate);
        customDateBox.setVisible(isCustomDate);
        customDateBox.setManaged(isCustomDate);

        priceCombo.setValue(priceCombo.getItems().contains(filterPriceMode) ? filterPriceMode : "Semua Harga");
        boolean isCustomPrice = "Custom".equals(priceCombo.getValue());
        minPriceField.setText(customMinPrice >= 0 ? String.valueOf(customMinPrice) : "");
        maxPriceField.setText(customMaxPrice >= 0 ? String.valueOf(customMaxPrice) : "");
        customPriceBox.setVisible(isCustomPrice);
        customPriceBox.setManaged(isCustomPrice);

        locCombo.setValue(locCombo.getItems().contains(filterLocation) ? filterLocation : "Semua Lokasi");
        locKeywordField.setText(filterLocationKeyword != null ? filterLocationKeyword : "");
        orgCombo.setValue(orgCombo.getItems().contains(filterOrganizer) ? filterOrganizer : "Semua Penyelenggara");
        availCheck.setSelected(filterAvailableOnly);
    }

    private void populateFilterOptions() {
        String prevLoc = filterLocation;
        locCombo.getItems().setAll("Semua Lokasi");
        allEvents.stream()
                .map(Event::getLocation)
                .filter(l -> l != null && !l.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .forEach(locCombo.getItems()::add);
        locCombo.setValue(locCombo.getItems().contains(prevLoc) ? prevLoc : "Semua Lokasi");

        String prevOrg = filterOrganizer;
        orgCombo.getItems().setAll("Semua Penyelenggara");
        allEvents.stream()
                .map(e -> e.getOwnerName() != null && !e.getOwnerName().isBlank() ? e.getOwnerName() : e.getOwnerOrg())
                .filter(o -> o != null && !o.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .forEach(orgCombo.getItems()::add);
        orgCombo.setValue(orgCombo.getItems().contains(prevOrg) ? prevOrg : "Semua Penyelenggara");
    }

    private void applyDrawerFilters() {
        filterDateMode = dateCombo.getValue() != null ? dateCombo.getValue() : "Semua Tanggal";
        customStartDate = startPicker.getValue();
        customEndDate = endPicker.getValue();
        filterPriceMode = priceCombo.getValue() != null ? priceCombo.getValue() : "Semua Harga";
        try {
            String rawMin = minPriceField.getText() != null ? minPriceField.getText().replaceAll("[^0-9]", "") : "";
            customMinPrice = rawMin.isEmpty() ? -1 : Long.parseLong(rawMin);
        } catch (Exception ex) {
            customMinPrice = -1;
        }
        try {
            String rawMax = maxPriceField.getText() != null ? maxPriceField.getText().replaceAll("[^0-9]", "") : "";
            customMaxPrice = rawMax.isEmpty() ? -1 : Long.parseLong(rawMax);
        } catch (Exception ex) {
            customMaxPrice = -1;
        }
        filterLocation = locCombo.getValue() != null ? locCombo.getValue() : "Semua Lokasi";
        filterLocationKeyword = locKeywordField.getText() != null ? locKeywordField.getText().trim() : "";
        filterOrganizer = orgCombo.getValue() != null ? orgCombo.getValue() : "Semua Penyelenggara";
        filterAvailableOnly = availCheck.isSelected();

        filterAndRender();
        toggleFilterDrawer(false);
    }

    private boolean isAnySpecificFilterActive() {
        return !"Semua Tanggal".equals(filterDateMode) ||
                !"Semua Harga".equals(filterPriceMode) ||
                !"Semua Lokasi".equals(filterLocation) ||
                (filterLocationKeyword != null && !filterLocationKeyword.isEmpty()) ||
                !"Semua Penyelenggara".equals(filterOrganizer) ||
                filterAvailableOnly;
    }

    private void updateGridConstraints(int cols) {
        gridPane.getColumnConstraints().clear();
        double percent = 100.0 / cols;
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percent);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            gridPane.getColumnConstraints().add(cc);
        }
    }

    public void refreshData() {
        final int seq = loadSeq.incrementAndGet();
        TiketinTheme.runAsync(eventDAO::getAllPublishedEvents, events -> {
            if (seq != loadSeq.get()) return;
            allEvents = events != null ? events : List.of();
            populateFilterOptions();
            rebuildUnderlineTabs();
            filterAndRender();
        }, err -> {
            if (seq != loadSeq.get()) return;
            gridPane.getChildren().clear();
            Label errLbl = new Label("Gagal memuat data event: " + err.getMessage());
            errLbl.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.DANGER_FG) + ";");
            gridPane.add(errLbl, 0, 0);
            GridPane.setColumnSpan(errLbl, Math.max(1, currentCols));
        });
    }

    private void rebuildUnderlineTabs() {
        underlineTabBar.getChildren().clear();
        underlineTabBar.getChildren().add(createUnderlineTab("Semua", "Semua"));

        List<String> categories = allEvents.stream()
                .map(Event::getCategory)
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        for (String cat : categories) {
            underlineTabBar.getChildren().add(createUnderlineTab(cat, cat));
        }
    }

    private Button createUnderlineTab(String text, String categoryKey) {
        Button tab = new Button(text);
        boolean isActive = selectedCategory.equalsIgnoreCase(categoryKey);
        tab.setFont(TiketinTheme.fontRegular(13.5));
        tab.getStyleClass().setAll(isActive ? "underline-tab-active" : "underline-tab");

        tab.setOnAction(e -> {
            selectedCategory = categoryKey;
            rebuildUnderlineTabs();
            filterAndRender();
        });
        return tab;
    }

    private void resetSpecificFilters() {
        filterDateMode = "Semua Tanggal";
        customStartDate = null;
        customEndDate = null;
        filterPriceMode = "Semua Harga";
        customMinPrice = -1;
        customMaxPrice = -1;
        filterLocation = "Semua Lokasi";
        filterLocationKeyword = "";
        filterOrganizer = "Semua Penyelenggara";
        filterAvailableOnly = false;
        syncDrawerInputsFromState();
        updateFilterButtonVisual(false);
    }

    private void updateFilterButtonVisual(boolean active) {
        boolean highlighted = active || drawerOpen;
        filterModalBtn.getStyleClass().setAll(highlighted ? "icon-btn-square-active" : "icon-btn-square");
        filterModalBtn.setGraphic(TiketinIcons.getIcon("filter", 15, highlighted ? TiketinTheme.ACCENT : TiketinTheme.TEXT_PRIMARY));
        if (resetQuickBtn != null) {
            resetQuickBtn.setVisible(active);
            resetQuickBtn.setManaged(active);
        }
    }

    private void filterAndRender() {
        String query = searchBar.getText() != null ? searchBar.getText().toLowerCase().trim() : "";
        String sortOption = sortCombo.getValue();
        LocalDateTime nowDt = LocalDateTime.now();
        LocalDate now = nowDt.toLocalDate();

        List<Event> filtered = allEvents.stream().filter(ev -> {
            // Category filter
            if (!"Semua".equalsIgnoreCase(selectedCategory)) {
                if (ev.getCategory() == null || !ev.getCategory().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }
            // Availability filter
            if (filterAvailableOnly && !EventSortUtil.isAvailable(ev, nowDt)) {
                return false;
            }
            // Date filter
            if (!"Semua Tanggal".equals(filterDateMode)) {
                LocalDate evDate = DateUtil.parseDateSafe(ev.getDate());
                if (evDate == null) return false;
                if ("Hari Ini".equals(filterDateMode) && !evDate.equals(now)) return false;
                if ("Minggu Ini".equals(filterDateMode) && (evDate.isBefore(now) || evDate.isAfter(now.plusDays(7)))) return false;
                if ("Bulan Ini".equals(filterDateMode) && (evDate.getMonth() != now.getMonth() || evDate.getYear() != now.getYear())) return false;
                if ("Custom".equals(filterDateMode)) {
                    if (customStartDate != null && evDate.isBefore(customStartDate)) return false;
                    if (customEndDate != null && evDate.isAfter(customEndDate)) return false;
                }
            }
            // Price filter
            if (!"Semua Harga".equals(filterPriceMode)) {
                long price = ev.getPrice();
                if ("Gratis".equals(filterPriceMode) && price > 0) return false;
                if ("Di bawah Rp 50.000".equals(filterPriceMode) && price >= 50000) return false;
                if ("Rp 50.000 - Rp 100.000".equals(filterPriceMode) && (price < 50000 || price > 100000)) return false;
                if ("Di atas Rp 50.000".equals(filterPriceMode) && price <= 50000) return false;
                if ("Di atas Rp 100.000".equals(filterPriceMode) && price <= 100000) return false;
                if ("Custom".equals(filterPriceMode)) {
                    if (customMinPrice >= 0 && price < customMinPrice) return false;
                    if (customMaxPrice >= 0 && price > customMaxPrice) return false;
                }
            }
            // Location filter
            if (!"Semua Lokasi".equals(filterLocation)) {
                if (ev.getLocation() == null || !ev.getLocation().equalsIgnoreCase(filterLocation)) return false;
            }
            if (filterLocationKeyword != null && !filterLocationKeyword.isEmpty()) {
                if (ev.getLocation() == null || !ev.getLocation().toLowerCase().contains(filterLocationKeyword.toLowerCase())) {
                    return false;
                }
            }
            // Organizer filter
            if (!"Semua Penyelenggara".equals(filterOrganizer)) {
                String org = ev.getOwnerName() != null && !ev.getOwnerName().isBlank() ? ev.getOwnerName() : ev.getOwnerOrg();
                if (org == null || !org.equalsIgnoreCase(filterOrganizer)) return false;
            }
            // Search query
            if (!query.isEmpty()) {
                boolean matchTitle = ev.getName() != null && ev.getName().toLowerCase().contains(query);
                boolean matchLoc = ev.getLocation() != null && ev.getLocation().toLowerCase().contains(query);
                boolean matchOrg = (ev.getOwnerName() != null && ev.getOwnerName().toLowerCase().contains(query))
                        || (ev.getOwnerOrg() != null && ev.getOwnerOrg().toLowerCase().contains(query));
                boolean matchCat = ev.getCategory() != null && ev.getCategory().toLowerCase().contains(query);
                if (!matchTitle && !matchLoc && !matchOrg && !matchCat) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // Sorting — 3-tier priority (Aktif -> Habis -> Berakhir) is always preserved
        filtered.sort(EventSortUtil.homeComparator(sortOption, nowDt));

        currentFilteredEvents = filtered;
        resultCountLabel.setText(filtered.size() + " acara ditemukan");
        layoutCards();
    }

    private void layoutCards() {
        gridPane.getChildren().clear();
        if (currentFilteredEvents.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(44));
            emptyState.getStyleClass().add("card-editorial");
            Label emptyLbl = new Label("Tidak ada event yang sesuai dengan filter atau pencarian.");
            emptyLbl.setFont(TiketinTheme.fontRegular(14));
            emptyLbl.setStyle("-fx-text-fill: #525A62;");
            emptyState.getChildren().add(emptyLbl);
            gridPane.add(emptyState, 0, 0);
            GridPane.setColumnSpan(emptyState, Math.max(1, currentCols));
            return;
        }

        int r = 0, c = 0;
        for (Event ev : currentFilteredEvents) {
            EventCardPanel card = new EventCardPanel(mainFrame, ev, this::refreshData);
            gridPane.add(card, c, r);
            c++;
            if (c >= currentCols) {
                c = 0;
                r++;
            }
        }
    }
}
