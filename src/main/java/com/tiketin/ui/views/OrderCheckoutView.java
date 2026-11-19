package com.tiketin.ui.views;

import com.tiketin.dao.OrderDAO;
import com.tiketin.dao.PaymentDAO;
import com.tiketin.model.Event;
import com.tiketin.model.Payment;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.EventImageViewerPanel;
import com.tiketin.ui.components.ImageDropZonePanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import com.tiketin.util.ProofStorageUtil;
import com.tiketin.util.QrCodeGenerator;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified single-screen checkout view inside MainFrame:
 * - Left column (50%): 4:3 Event Image Viewer (fit non-cropped, click for lightbox) + Event Details + Quantity
 * - Right column (50%): Payment Method Selector + QRIS QR / Bank Detail + Purchase Summary + Proof Upload + Pay CTA
 */
public class OrderCheckoutView extends VBox {
    private final MainFrame mainFrame;
    private Event currentEvent;
    private Runnable onOrderSuccess;
    private final AtomicInteger loadSeq = new AtomicInteger(0);

    private int quantity = 1;
    private int maxQty = 10;

    // Left controls (Image Showcase + Details + Qty)
    private EventImageViewerPanel imageViewerPanel;
    private Label heroCatLbl;
    private Label heroTitleLbl;
    private Label heroOrgLbl;
    private Label heroScheduleLbl;
    private Label heroLocationLbl;
    private Label heroDescLbl;
    private Label stockNoteLbl;
    private Label buyerNameLbl;
    private Label qtyValueLbl;
    private Button minusQtyBtn;
    private Button plusQtyBtn;

    // Right controls (Payment + QR + Summary + Proof + Pay)
    private VBox methodCard;
    private VBox paymentMethodListBox;
    private VBox paymentDetailBox;
    private Label sumEventNameLbl;
    private Label sumScheduleLbl;
    private Label sumQtyLbl;
    private Label sumUnitPriceLbl;
    private HBox sumMethodRow;
    private Label sumMethodLbl;
    private Label sumTotalLbl;

    private VBox proofSectionBox;
    private ImageDropZonePanel dropZone;
    private File proofFile;
    private AnimatedButton confirmOrderBtn;

    private List<Payment> activePayments = List.of();
    private Payment selectedPayment;

    public OrderCheckoutView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(0);
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        VBox scrollBody = new VBox(14);
        scrollBody.setPadding(new Insets(22, 32, 28, 32));
        scrollBody.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        // Header
        Button topBackBtn = new Button("Kembali ke Acara");
        topBackBtn.setGraphic(TiketinIcons.getIcon("arrow-left", 15, TiketinTheme.TEXT_SECONDARY));
        topBackBtn.setGraphicTextGap(6);
        topBackBtn.getStyleClass().setAll("btn-back-link");
        topBackBtn.setOnAction(e -> mainFrame.navigateTo(MainFrame.VIEW_HOME));

        Label pageTitle = new Label("Pesan Tiket");
        pageTitle.setFont(TiketinTheme.fontBold(24));
        pageTitle.setStyle("-fx-text-fill: #20252A;");

        VBox headerBox = new VBox(4, topBackBtn, pageTitle);

        // Balanced 2-Column Layout (50% Gallery Showcase Left, 50% Payment + QR + Summary Right)
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);

        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setPercentWidth(50);
        colLeft.setHgrow(Priority.ALWAYS);
        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setPercentWidth(50);
        colRight.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(colLeft, colRight);

        VBox leftColumn = buildLeftColumn();
        VBox rightColumn = buildRightColumn();
        GridPane.setValignment(leftColumn, VPos.TOP);
        GridPane.setValignment(rightColumn, VPos.TOP);

        grid.add(leftColumn, 0, 0);
        grid.add(rightColumn, 1, 0);

        scrollBody.getChildren().addAll(headerBox, grid);

        ScrollPane scrollPane = new ScrollPane(scrollBody);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private VBox buildLeftColumn() {
        VBox leftCol = new VBox(14);

        // Card 1: 4:3 Event Photo Viewer + Event Details
        VBox showcaseCard = new VBox(12);
        showcaseCard.setPadding(new Insets(18));
        showcaseCard.getStyleClass().add("card-editorial");

        imageViewerPanel = new EventImageViewerPanel();
        imageViewerPanel.setOnOpenLightbox(() -> {
            if (currentEvent != null && currentEvent.getImagePath() != null && !currentEvent.getImagePath().isBlank()) {
                mainFrame.showImageLightbox(currentEvent.getImagePath(), currentEvent.getName());
            }
        });

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        heroCatLbl = TiketinTheme.createCategoryBadge("Acara");
        heroTitleLbl = new Label();
        heroTitleLbl.setFont(TiketinTheme.fontBold(18));
        heroTitleLbl.setStyle("-fx-text-fill: #20252A;");
        heroTitleLbl.setWrapText(true);
        HBox.setHgrow(heroTitleLbl, Priority.ALWAYS);
        titleRow.getChildren().addAll(heroTitleLbl, heroCatLbl);

        heroOrgLbl = new Label();
        heroOrgLbl.setFont(TiketinTheme.fontRegular(12.5));
        heroOrgLbl.setStyle("-fx-text-fill: #687078;");

        heroScheduleLbl = new Label();
        heroScheduleLbl.setFont(TiketinTheme.fontBold(12.5));
        heroScheduleLbl.setStyle("-fx-text-fill: #20252A;");
        heroScheduleLbl.setWrapText(true);
        HBox schedRow = new HBox(7, TiketinIcons.getIcon("calendar", 13, TiketinTheme.ACCENT), heroScheduleLbl);
        schedRow.setAlignment(Pos.CENTER_LEFT);

        heroLocationLbl = new Label();
        heroLocationLbl.setFont(TiketinTheme.fontRegular(12.5));
        heroLocationLbl.setStyle("-fx-text-fill: #20252A;");
        heroLocationLbl.setWrapText(true);
        HBox locRow = new HBox(7, TiketinIcons.getIcon("location", 13, TiketinTheme.ACCENT), heroLocationLbl);
        locRow.setAlignment(Pos.CENTER_LEFT);

        VBox metaBlock = new VBox(5, heroOrgLbl, schedRow, locRow);

        heroDescLbl = new Label();
        heroDescLbl.setFont(TiketinTheme.fontRegular(13));
        heroDescLbl.setStyle("-fx-text-fill: #525A62;");
        heroDescLbl.setWrapText(true);

        showcaseCard.getChildren().addAll(imageViewerPanel, titleRow, metaBlock, divider(), heroDescLbl);

        // Card 2: Inline Ticket Quantity Stepper + Buyer Info
        VBox qtyCard = new VBox(10);
        qtyCard.setPadding(new Insets(16, 18, 16, 18));
        qtyCard.getStyleClass().add("card-editorial");

        HBox qtyRow = new HBox(12);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        VBox qtyTexts = new VBox(3);
        Label qtyTitle = new Label("Jumlah Tiket");
        qtyTitle.setFont(TiketinTheme.fontBold(14.5));
        qtyTitle.setStyle("-fx-text-fill: #20252A;");
        stockNoteLbl = new Label("Maksimal 10 tiket per pesanan");
        stockNoteLbl.setFont(TiketinTheme.fontRegular(12));
        stockNoteLbl.setStyle("-fx-text-fill: #687078;");
        qtyTexts.getChildren().addAll(qtyTitle, stockNoteLbl);

        Region qSpacer = new Region();
        HBox.setHgrow(qSpacer, Priority.ALWAYS);

        HBox stepper = new HBox(0);
        stepper.setAlignment(Pos.CENTER);
        stepper.setStyle("-fx-background-color: #F7F8F6; -fx-border-color: #D0D5CF; -fx-border-width: 1px; -fx-border-radius: 9px; -fx-background-radius: 9px;");

        minusQtyBtn = new Button("−");
        minusQtyBtn.setMinSize(34, 34);
        minusQtyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #20252A; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        minusQtyBtn.setOnAction(e -> setQuantity(quantity - 1));

        qtyValueLbl = new Label("1");
        qtyValueLbl.setMinWidth(38);
        qtyValueLbl.setAlignment(Pos.CENTER);
        qtyValueLbl.setFont(TiketinTheme.fontBold(14.5));
        qtyValueLbl.setStyle("-fx-text-fill: #20252A;");

        plusQtyBtn = new Button("+");
        plusQtyBtn.setMinSize(34, 34);
        plusQtyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #20252A; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        plusQtyBtn.setOnAction(e -> setQuantity(quantity + 1));

        stepper.getChildren().addAll(minusQtyBtn, qtyValueLbl, plusQtyBtn);
        qtyRow.getChildren().addAll(qtyTexts, qSpacer, stepper);

        HBox buyerRow = new HBox(7);
        buyerRow.setAlignment(Pos.CENTER_LEFT);
        buyerNameLbl = new Label();
        buyerNameLbl.setFont(TiketinTheme.fontRegular(12.5));
        buyerNameLbl.setStyle("-fx-text-fill: #525A62;");
        buyerRow.getChildren().addAll(TiketinIcons.getIcon("user", 13, TiketinTheme.TEXT_SECONDARY), buyerNameLbl);

        qtyCard.getChildren().addAll(qtyRow, divider(), buyerRow);

        leftCol.getChildren().addAll(showcaseCard, qtyCard);
        return leftCol;
    }

    private VBox buildRightColumn() {
        VBox rightCol = new VBox(14);

        // Card 1: Payment Method Selector + Dynamic QRIS QR / Bank Transfer Card
        methodCard = new VBox(12);
        methodCard.setPadding(new Insets(18));
        methodCard.getStyleClass().add("card-editorial");

        Label methodHeading = new Label("Metode Pembayaran");
        methodHeading.setFont(TiketinTheme.fontBold(15));
        methodHeading.setStyle("-fx-text-fill: #20252A;");

        paymentMethodListBox = new VBox(8);
        paymentDetailBox = new VBox(10);

        methodCard.getChildren().addAll(methodHeading, paymentMethodListBox, paymentDetailBox);

        // Card 2: Purchase Summary + Proof Upload + Confirm & Pay CTA
        VBox summaryCard = new VBox(12);
        summaryCard.setPadding(new Insets(18));
        summaryCard.getStyleClass().add("card-editorial");

        Label rightHeadingLbl = new Label("Ringkasan Pembelian");
        rightHeadingLbl.setFont(TiketinTheme.fontBold(15.5));
        rightHeadingLbl.setStyle("-fx-text-fill: #20252A;");

        sumEventNameLbl = new Label();
        sumEventNameLbl.setFont(TiketinTheme.fontBold(14));
        sumEventNameLbl.setStyle("-fx-text-fill: #20252A;");
        sumEventNameLbl.setWrapText(true);

        sumScheduleLbl = new Label();
        sumScheduleLbl.setFont(TiketinTheme.fontRegular(12));
        sumScheduleLbl.setStyle("-fx-text-fill: #687078;");
        sumScheduleLbl.setWrapText(true);

        sumQtyLbl = new Label();
        HBox rQty = summaryRow("Jumlah Tiket", sumQtyLbl);

        sumUnitPriceLbl = new Label();
        HBox rUnit = summaryRow("Harga Satuan", sumUnitPriceLbl);

        sumMethodLbl = new Label("QRIS");
        sumMethodRow = summaryRow("Metode Bayar", sumMethodLbl);

        HBox totalRow = new HBox(8);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalKey = new Label("Total Pembayaran");
        totalKey.setFont(TiketinTheme.fontBold(14));
        totalKey.setStyle("-fx-text-fill: #20252A;");
        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        sumTotalLbl = new Label();
        sumTotalLbl.setFont(TiketinTheme.fontBold(18));
        sumTotalLbl.setStyle("-fx-text-fill: #E06338;");
        totalRow.getChildren().addAll(totalKey, tSpacer, sumTotalLbl);

        // Embedded Proof Upload Section
        proofSectionBox = new VBox(8);
        Label proofHeading = new Label("Bukti Pembayaran");
        proofHeading.setFont(TiketinTheme.fontBold(14));
        proofHeading.setStyle("-fx-text-fill: #20252A;");

        Label proofSub = new Label("Unggah tangkapan layar atau foto bukti transfer/QRIS agar penyelenggara dapat memverifikasi.");
        proofSub.setFont(TiketinTheme.fontRegular(12));
        proofSub.setStyle("-fx-text-fill: #525A62;");
        proofSub.setWrapText(true);

        dropZone = new ImageDropZonePanel(f -> this.proofFile = f);
        proofSectionBox.getChildren().addAll(divider(), proofHeading, proofSub, dropZone);

        confirmOrderBtn = TiketinTheme.createPrimaryButton("Konfirmasi & Bayar", "check");
        confirmOrderBtn.setMaxWidth(Double.MAX_VALUE);
        confirmOrderBtn.setOnAction(e -> submitOrder());

        summaryCard.getChildren().addAll(
                rightHeadingLbl,
                divider(),
                sumEventNameLbl,
                sumScheduleLbl,
                divider(),
                rQty,
                rUnit,
                sumMethodRow,
                divider(),
                totalRow,
                proofSectionBox,
                confirmOrderBtn
        );

        rightCol.getChildren().addAll(methodCard, summaryCard);
        return rightCol;
    }

    private HBox summaryRow(String labelText, Label valueLabel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(labelText);
        k.setFont(TiketinTheme.fontRegular(12.5));
        k.setStyle("-fx-text-fill: #525A62;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        valueLabel.setFont(TiketinTheme.fontBold(12.5));
        valueLabel.setStyle("-fx-text-fill: #20252A;");
        valueLabel.setWrapText(true);
        row.getChildren().addAll(k, sp, valueLabel);
        return row;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxHeight(1);
        r.setStyle("-fx-background-color: #E3E6E2;");
        return r;
    }

    public void setEvent(Event event, Runnable onOrderSuccess) {
        this.currentEvent = event;
        this.onOrderSuccess = onOrderSuccess;
        this.quantity = 1;
        this.maxQty = Math.min(10, Math.max(1, event.getSlots()));
        this.proofFile = null;
        if (dropZone != null) {
            dropZone.clearSelection();
        }

        // Left showcase: 4:3 event photo viewer + event overview
        imageViewerPanel.setImage(event.getImagePath(), event.getCategory());
        String cat = event.getCategory() != null && !event.getCategory().isBlank() ? event.getCategory() : "Umum";
        heroCatLbl.setText(cat);
        heroTitleLbl.setText(event.getName() != null ? event.getName() : "Acara");
        String org = event.getOwnerName() != null && !event.getOwnerName().isBlank()
                ? event.getOwnerName()
                : (event.getOwnerOrg() != null ? event.getOwnerOrg() : "Penyelenggara");
        heroOrgLbl.setText("Diselenggarakan oleh " + org);
        String timeStr = event.getTime() != null && !event.getTime().isBlank() ? " • " + event.getTime() : "";
        String scheduleFull = DateUtil.formatTanggalId(event.getDate()) + timeStr;
        String locFull = event.getLocation() != null && !event.getLocation().isBlank() ? event.getLocation() : "Lokasi TBA";
        heroScheduleLbl.setText(scheduleFull);
        heroLocationLbl.setText(locFull);

        String desc = event.getDescription() != null && !event.getDescription().isBlank()
                ? event.getDescription()
                : "Ikuti acara " + event.getName() + " di " + locFull + ".";
        heroDescLbl.setText(desc);

        stockNoteLbl.setText("Tersedia " + event.getSlots() + " tiket (maks. " + maxQty + " per pesanan)");
        User user = SessionManager.getCurrentUser();
        buyerNameLbl.setText(user != null
                ? "Pemesan: " + user.getFullName() + " (@" + user.getUsername() + ")"
                : "Pemesan: Tamu");

        // Right summary static fields
        sumEventNameLbl.setText(event.getName());
        sumScheduleLbl.setText(scheduleFull + " • " + locFull);
        sumUnitPriceLbl.setText(event.getPrice() <= 0 ? "Gratis" : CurrencyUtil.formatRupiah(event.getPrice()));

        boolean paid = event.getPrice() > 0;
        methodCard.setVisible(paid);
        methodCard.setManaged(paid);
        proofSectionBox.setVisible(paid);
        proofSectionBox.setManaged(paid);
        confirmOrderBtn.setText(paid ? "Konfirmasi & Bayar" : "Konfirmasi Pesanan");
        confirmOrderBtn.setDisable(false);

        setQuantity(1);
        loadPaymentsForEvent();
    }

    private void setQuantity(int newQty) {
        if (newQty < 1) newQty = 1;
        if (newQty > maxQty) newQty = maxQty;
        this.quantity = newQty;
        qtyValueLbl.setText(String.valueOf(quantity));
        minusQtyBtn.setDisable(quantity <= 1);
        plusQtyBtn.setDisable(quantity >= maxQty);

        if (currentEvent != null) {
            sumQtyLbl.setText(quantity + " Tiket");
            long total = (long) currentEvent.getPrice() * quantity;
            sumTotalLbl.setText(total <= 0 ? "Gratis" : CurrencyUtil.formatRupiah(total));
            if (selectedPayment != null && isQrisPayment(selectedPayment)) {
                renderSelectedPaymentDetail();
            }
        }
    }

    private void loadPaymentsForEvent() {
        if (currentEvent == null) return;
        if (currentEvent.getPrice() <= 0) {
            activePayments = List.of();
            selectedPayment = null;
            renderPaymentOptions();
            return;
        }
        final int seq = loadSeq.incrementAndGet();
        int ownerId = currentEvent.getOwnerId();
        TiketinTheme.runAsync(() -> new PaymentDAO().getPaymentsByOrganizer(ownerId), list -> {
            if (seq != loadSeq.get()) return;
            List<Payment> opts = new ArrayList<>();
            boolean hasQris = false;
            if (list != null) {
                for (Payment p : list) {
                    opts.add(p);
                    if (isQrisPayment(p)) {
                        hasQris = true;
                    }
                }
            }
            if (!hasQris) {
                String orgName = currentEvent.getOwnerOrg() != null && !currentEvent.getOwnerOrg().isBlank()
                        ? currentEvent.getOwnerOrg()
                        : (currentEvent.getOwnerName() != null ? currentEvent.getOwnerName() : "Tiketin Official");
                Payment fallbackQris = new Payment();
                fallbackQris.setId(-1);
                fallbackQris.setUserId(ownerId);
                fallbackQris.setBankName("QRIS");
                fallbackQris.setAccountNumber("ID1020" + String.format("%06d", Math.max(1, ownerId)));
                fallbackQris.setAccountName(orgName);
                fallbackQris.setActive(true);
                opts.add(0, fallbackQris);
            }
            activePayments = opts;
            selectedPayment = opts.stream()
                    .filter(this::isQrisPayment)
                    .findFirst()
                    .orElse(opts.isEmpty() ? null : opts.get(0));
            renderPaymentOptions();
        }, err -> {
            if (seq != loadSeq.get()) return;
            activePayments = List.of();
            selectedPayment = null;
            renderPaymentOptions();
        });
    }

    private void renderPaymentOptions() {
        paymentMethodListBox.getChildren().clear();

        if (currentEvent != null && currentEvent.getPrice() <= 0) {
            Label freeLbl = new Label("Acara gratis — langsung konfirmasi pesanan di sisi kanan tanpa bukti transfer.");
            freeLbl.setFont(TiketinTheme.fontRegular(13));
            freeLbl.setStyle("-fx-text-fill: #15803D;");
            freeLbl.setWrapText(true);
            paymentMethodListBox.getChildren().add(freeLbl);
            sumMethodLbl.setText("Gratis");
            renderSelectedPaymentDetail();
            return;
        }

        for (Payment p : activePayments) {
            boolean isQris = isQrisPayment(p);
            boolean isSelected = (p == selectedPayment);

            HBox rowCard = new HBox(10);
            rowCard.setAlignment(Pos.CENTER_LEFT);
            rowCard.setPadding(new Insets(10, 12, 10, 12));
            rowCard.setStyle(stylePaymentOption(isSelected));

            StackPane badge = new StackPane(TiketinIcons.getIcon(isQris ? "qrcode" : "sliders", 15,
                    isSelected ? TiketinTheme.ACCENT : TiketinTheme.TEXT_SECONDARY));
            badge.setMinSize(32, 32);
            badge.setPrefSize(32, 32);
            badge.setStyle("-fx-background-color: " + (isSelected ? "#FFFFFF" : "#F4F1EC") + "; -fx-background-radius: 8px;");

            VBox texts = new VBox(2);
            Label title = new Label(isQris ? "QRIS (Scan QR)" : p.getBankName());
            title.setFont(TiketinTheme.fontBold(13));
            title.setStyle("-fx-text-fill: #20252A;");

            Label sub = new Label(isQris
                    ? p.getAccountName() + " • " + p.getAccountNumber()
                    : p.getAccountNumber() + " • a.n. " + p.getAccountName());
            sub.setFont(TiketinTheme.fontRegular(11.5));
            sub.setStyle("-fx-text-fill: #525A62;");
            texts.getChildren().addAll(title, sub);
            HBox.setHgrow(texts, Priority.ALWAYS);

            Label pickIndicator = new Label(isSelected ? "Dipilih" : "Pilih");
            pickIndicator.setFont(TiketinTheme.fontBold(11));
            pickIndicator.setStyle(isSelected
                    ? "-fx-background-color: #E06338; -fx-text-fill: #FFFFFF; -fx-padding: 3 10; -fx-background-radius: 999;"
                    : "-fx-background-color: #F4F1EC; -fx-text-fill: #525A62; -fx-padding: 3 10; -fx-background-radius: 999;");

            rowCard.getChildren().addAll(badge, texts, pickIndicator);
            rowCard.setOnMouseClicked(e -> {
                selectedPayment = p;
                renderPaymentOptions();
                renderSelectedPaymentDetail();
            });
            paymentMethodListBox.getChildren().add(rowCard);
        }
        renderSelectedPaymentDetail();
    }

    private void renderSelectedPaymentDetail() {
        paymentDetailBox.getChildren().clear();
        if (currentEvent == null || currentEvent.getPrice() <= 0 || selectedPayment == null) {
            return;
        }

        long total = (long) currentEvent.getPrice() * quantity;
        boolean isQris = isQrisPayment(selectedPayment);
        sumMethodLbl.setText(isQris ? "QRIS" : selectedPayment.getBankName());

        if (isQris) {
            // Compact horizontal QRIS layout eliminates large empty vertical flanks
            HBox qrisCard = new HBox(16);
            qrisCard.setAlignment(Pos.CENTER_LEFT);
            qrisCard.setPadding(new Insets(14));
            qrisCard.setStyle("-fx-background-color: #F7F8F6; -fx-border-color: #E3E6E2; -fx-border-width: 1px; -fx-border-radius: 12px; -fx-background-radius: 12px;");

            String qrPayload = String.format("TIKETIN|QRIS|%s|%s|EV%d|QTY%d|IDR%d",
                    selectedPayment.getAccountNumber(),
                    selectedPayment.getAccountName(),
                    currentEvent.getId(),
                    quantity,
                    total);

            StackPane qrFrame = new StackPane();
            qrFrame.setPadding(new Insets(8));
            qrFrame.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            qrFrame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            qrFrame.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CFD4CE; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            Canvas qrCanvas = QrCodeGenerator.createQrCanvas(qrPayload, 134);
            qrFrame.getChildren().add(qrCanvas);

            VBox infoCol = new VBox(5);
            infoCol.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoCol, Priority.ALWAYS);

            Label qrisBadge = new Label("QRIS STANDAR NASIONAL");
            qrisBadge.setFont(TiketinTheme.fontBold(10.5));
            qrisBadge.setStyle("-fx-background-color: #20252A; -fx-text-fill: #FFFFFF; -fx-padding: 3 8; -fx-background-radius: 5px;");

            Label merchantLbl = new Label(selectedPayment.getAccountName());
            merchantLbl.setFont(TiketinTheme.fontBold(13.5));
            merchantLbl.setStyle("-fx-text-fill: #20252A;");
            merchantLbl.setWrapText(true);

            String accNo = selectedPayment.getAccountNumber() != null ? selectedPayment.getAccountNumber().trim() : "";
            String nmidText = accNo.toUpperCase().startsWith("NMID") ? accNo : ("NMID: " + accNo);
            Label nmidLbl = new Label(nmidText);
            nmidLbl.setFont(TiketinTheme.fontRegular(11.5));
            nmidLbl.setStyle("-fx-text-fill: #687078;");

            Label nominalLbl = new Label("Total Scan: " + CurrencyUtil.formatRupiah(total));
            nominalLbl.setFont(TiketinTheme.fontBold(14));
            nominalLbl.setStyle("-fx-text-fill: #E06338;");

            Label guideLbl = new Label("Pindai kode QR dengan m-banking/e-wallet lalu unggah bukti bayar di bawah.");
            guideLbl.setFont(TiketinTheme.fontRegular(12));
            guideLbl.setStyle("-fx-text-fill: #525A62;");
            guideLbl.setWrapText(true);

            infoCol.getChildren().addAll(qrisBadge, merchantLbl, nmidLbl, nominalLbl, guideLbl);
            qrisCard.getChildren().addAll(qrFrame, infoCol);
            paymentDetailBox.getChildren().add(qrisCard);
        } else {
            HBox transferCard = new HBox(12);
            transferCard.setAlignment(Pos.CENTER_LEFT);
            transferCard.setPadding(new Insets(12, 14, 12, 14));
            transferCard.setStyle("-fx-background-color: #F7F8F6; -fx-border-color: #E3E6E2; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

            VBox info = new VBox(3);
            Label lblNum = new Label(selectedPayment.getBankName() + " — " + selectedPayment.getAccountNumber());
            lblNum.setFont(TiketinTheme.fontBold(14));
            lblNum.setStyle("-fx-text-fill: #20252A;");
            Label lblOwner = new Label("Atas Nama: " + selectedPayment.getAccountName() + " • Nominal: " + CurrencyUtil.formatRupiah(total));
            lblOwner.setFont(TiketinTheme.fontRegular(12));
            lblOwner.setStyle("-fx-text-fill: #525A62;");
            lblOwner.setWrapText(true);
            info.getChildren().addAll(lblNum, lblOwner);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button copyBtn = TiketinTheme.createSecondaryButton("Salin Nomor", "copy");
            copyBtn.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(selectedPayment.getAccountNumber());
                Clipboard.getSystemClipboard().setContent(cc);
                ToastNotification.info(mainFrame, "Nomor rekening disalin.");
            });

            transferCard.getChildren().addAll(info, copyBtn);
            paymentDetailBox.getChildren().add(transferCard);
        }
    }

    private void submitOrder() {
        if (currentEvent == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            ToastNotification.error(mainFrame, "Silakan masuk terlebih dahulu.");
            return;
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            ToastNotification.error(mainFrame, "Akun Admin tidak dapat melakukan pemesanan tiket.");
            return;
        }
        if (DateUtil.isEventPassed(currentEvent.getDate(), currentEvent.getTime())) {
            ToastNotification.error(mainFrame, "Acara sudah berakhir dan tiket tidak dapat dipesan.");
            return;
        }
        String qtyErr = ValidationUtil.validateOrderQuantity(quantity);
        if (qtyErr != null) {
            ToastNotification.error(mainFrame, qtyErr);
            return;
        }
        if (currentEvent.getPrice() > 0) {
            if (selectedPayment == null) {
                ToastNotification.error(mainFrame, "Pilih metode pembayaran terlebih dahulu.");
                return;
            }
            if (proofFile == null) {
                ToastNotification.error(mainFrame, "Unggah bukti pembayaran terlebih dahulu.");
                return;
            }
            String fileErr = ValidationUtil.validateImageFile(proofFile);
            if (fileErr != null) {
                ToastNotification.error(mainFrame, fileErr);
                return;
            }
        }

        String proofFilename = null;
        if (currentEvent.getPrice() > 0 && proofFile != null) {
            try {
                proofFilename = ProofStorageUtil.saveProof(proofFile);
            } catch (Exception ex) {
                ToastNotification.error(mainFrame, "Gagal menyimpan bukti bayar: " + ex.getMessage());
                return;
            }
        }
        final String finalProof = proofFilename;
        String method = currentEvent.getPrice() <= 0
                ? "Gratis"
                : (selectedPayment != null ? selectedPayment.getBankName() : "QRIS");

        confirmOrderBtn.setDisable(true);
        TiketinTheme.runAsync(() -> new OrderDAO().createOrderAtomic(user.getId(), currentEvent.getId(), quantity, method, finalProof),
            orderCode -> {
                confirmOrderBtn.setDisable(false);
                if (orderCode != null && !orderCode.isEmpty()) {
                    ToastNotification.success(mainFrame, "Pesanan berhasil dibuat (#" + orderCode + ")");
                    if (onOrderSuccess != null) {
                        onOrderSuccess.run();
                    }
                    mainFrame.navigateTo(MainFrame.VIEW_MY_TICKETS);
                } else {
                    ToastNotification.error(mainFrame, "Gagal memproses pesanan.");
                }
            },
            err -> {
                confirmOrderBtn.setDisable(false);
                ToastNotification.error(mainFrame, "Gagal memesan: " + err.getMessage());
            }
        );
    }

    private boolean isQrisPayment(Payment p) {
        return p != null && p.getBankName() != null && p.getBankName().toLowerCase().contains("qris");
    }

    private String stylePaymentOption(boolean selected) {
        if (selected) {
            return "-fx-background-color: #FFF0EB; -fx-border-color: #E06338; -fx-border-width: 1.5px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-cursor: hand;";
        }
        return "-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-cursor: hand;";
    }
}
