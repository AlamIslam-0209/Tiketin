package com.tiketin.ui.views;

import com.tiketin.dao.ApplicationDAO;
import com.tiketin.model.Application;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.EventImageUploadPanel;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure JavaFX VBox view for submitting organizer status & first event proposal.
 */
public class ApplyOrganizerView extends VBox {

    private final MainFrame mainFrame;
    private final VBox statusBannerContainer;
    private final VBox contentContainer;
    private final AtomicInteger loadSeq = new AtomicInteger(0);

    private TextField fullNameField;
    private TextField contactField;
    private TextField orgNameField;

    private TextField eventNameField;
    private ComboBox<String> eventCategoryCombo;
    private DatePicker eventDatePicker;
    private TextField eventTimeField;
    private TextField eventLocationField;
    private TextField eventPriceField;
    private TextField eventSlotsField;
    private TextArea eventDescArea;
    private EventImageUploadPanel eventImageUploader;

    private AnimatedButton submitBtn;

    public ApplyOrganizerView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(0);
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        VBox scrollBody = new VBox(20);
        scrollBody.setPadding(new Insets(30, 36, 36, 36));
        scrollBody.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        PageHeaderPanel header = new PageHeaderPanel("Daftar Organizer", null);
        Button refreshBtn = TiketinTheme.createSquareIconButton("refresh", 42, "Muat ulang status");
        refreshBtn.setOnAction(e -> refreshData());
        header.addAction(refreshBtn);

        statusBannerContainer = new VBox(12);
        contentContainer = new VBox(18);

        scrollBody.getChildren().addAll(header, statusBannerContainer, contentContainer);

        ScrollPane scrollPane = new ScrollPane(scrollBody);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    public void refreshData() {
        final int seq = loadSeq.incrementAndGet();
        statusBannerContainer.getChildren().clear();
        contentContainer.getChildren().clear();

        if (!SessionManager.isLoggedIn()) {
            VBox loginCard = new VBox(16);
            loginCard.setAlignment(Pos.CENTER);
            loginCard.setPadding(new Insets(44));
            loginCard.getStyleClass().add("card-editorial");
            Label msg = new Label("Silakan login terlebih dahulu untuk mengajukan diri sebagai Organizer.");
            msg.setFont(TiketinTheme.fontBold(14));
            msg.setStyle("-fx-text-fill: #525A62;");

            AnimatedButton loginBtn = TiketinTheme.createPrimaryButton("Masuk / Daftar", "user");
            loginBtn.setOnAction(e -> mainFrame.openAuth(MainFrame.VIEW_APPLY_ORGANIZER, () -> mainFrame.navigateTo(MainFrame.VIEW_APPLY_ORGANIZER)));
            loginCard.getChildren().addAll(msg, loginBtn);
            contentContainer.getChildren().add(loginCard);
            return;
        }

        User user = SessionManager.getCurrentUser();
        if ("organizer".equalsIgnoreCase(user.getRole()) || "admin".equalsIgnoreCase(user.getRole())) {
            VBox successCard = new VBox(14);
            successCard.setAlignment(Pos.CENTER);
            successCard.setPadding(new Insets(36));
            successCard.getStyleClass().add("card-editorial");
            Label info = new Label("Akun Anda (" + user.getUsername() + ") sudah berstatus " + user.getRole().toUpperCase() + ".");
            info.setFont(TiketinTheme.fontBold(15.5));
            info.setStyle("-fx-text-fill: #15803D;");

            AnimatedButton studioBtn = TiketinTheme.createPrimaryButton("Buka Studio Organizer", "grid");
            studioBtn.setOnAction(e -> mainFrame.navigateTo(MainFrame.VIEW_ORGANIZER));

            successCard.getChildren().addAll(info, studioBtn);
            contentContainer.getChildren().add(successCard);
            return;
        }

        TiketinTheme.runAsync(() -> new ApplicationDAO().getLatestApplicationByUser(user.getId()),
                app -> {
                    if (seq != loadSeq.get()) return;
                    buildForm(user, app);
                },
                err -> {
                    if (seq != loadSeq.get()) return;
                    buildForm(user, null);
                }
        );
    }

    private void buildForm(User user, Application latestApp) {
        statusBannerContainer.getChildren().clear();
        contentContainer.getChildren().clear();

        boolean isDisabled = false;
        boolean prefillFromRejected = false;

        if (latestApp != null) {
            String status = latestApp.getStatus() != null ? latestApp.getStatus().toLowerCase() : "";
            VBox banner = new VBox(6);
            banner.setPadding(new Insets(16, 20, 16, 20));
            banner.getStyleClass().add("card-editorial");

            Label bannerTitle = new Label();
            bannerTitle.setFont(TiketinTheme.fontBold(14));

            Label bannerDesc = new Label();
            bannerDesc.setFont(TiketinTheme.fontRegular(13));
            bannerDesc.setWrapText(true);
            bannerDesc.setStyle("-fx-text-fill: #20252A;");

            if (status.contains("pending") || status.contains("menunggu")) {
                bannerTitle.setStyle("-fx-text-fill: #805B16;");
                bannerTitle.setText("Pengajuan Anda sedang ditinjau oleh Admin");
                bannerDesc.setText("Mohon tunggu proses verifikasi sebelum mengirim pengajuan baru.");
                isDisabled = true;
            } else if (status.contains("rejected") || status.contains("ditolak")) {
                bannerTitle.setStyle("-fx-text-fill: #B91C1C;");
                bannerTitle.setText("Pengajuan Sebelumnya Ditolak");
                bannerDesc.setText("Catatan Admin: " + (latestApp.getReason() != null && !latestApp.getReason().isBlank()
                        ? latestApp.getReason()
                        : "Tidak ada catatan") + ". Silakan perbaiki data di bawah dan ajukan kembali.");
                prefillFromRejected = true;
            } else if (status.contains("approved") || status.contains("disetujui")) {
                bannerTitle.setStyle("-fx-text-fill: #15803D;");
                bannerTitle.setText("Pengajuan Disetujui!");
                bannerDesc.setText("Selamat, status Anda telah ditingkatkan. Silakan muat ulang atau login kembali bila menu Studio belum muncul.");
                isDisabled = true;
            }

            if (!bannerTitle.getText().isEmpty()) {
                banner.getChildren().addAll(bannerTitle, bannerDesc);
                statusBannerContainer.getChildren().add(banner);
            }
        }

        // Section 1: Profil Penyelenggara
        VBox profileCardContent = new VBox(12);
        profileCardContent.setPadding(new Insets(20));
        profileCardContent.getStyleClass().add("card-editorial");

        Label sec1Title = new Label("1. Profil Penyelenggara");
        sec1Title.setFont(TiketinTheme.fontBold(15));
        sec1Title.setStyle("-fx-text-fill: #20252A;");

        fullNameField = TiketinTheme.createTextField("Nama Lengkap Penanggung Jawab");
        fullNameField.setText(prefillFromRejected && latestApp.getFullName() != null
                ? latestApp.getFullName()
                : (user.getFullName() != null ? user.getFullName() : ""));
        fullNameField.setDisable(isDisabled);

        contactField = TiketinTheme.createTextField("Nomor WhatsApp / Kontak Aktif");
        if (prefillFromRejected && latestApp.getContact() != null) {
            contactField.setText(latestApp.getContact());
        }
        contactField.setDisable(isDisabled);

        orgNameField = TiketinTheme.createTextField("Nama Organisasi / Komunitas / PT");
        orgNameField.setText(prefillFromRejected && latestApp.getOrganization() != null
                ? latestApp.getOrganization()
                : (user.getOrganization() != null ? user.getOrganization() : ""));
        orgNameField.setDisable(isDisabled);

        profileCardContent.getChildren().addAll(
                sec1Title,
                TiketinTheme.createFieldGroup("Nama Lengkap", fullNameField),
                TiketinTheme.createFieldGroup("Kontak / WhatsApp", contactField),
                TiketinTheme.createFieldGroup("Nama Organisasi", orgNameField)
        );

        // Guide Card below Profile to balance the left column naturally
        VBox guideCard = new VBox(12);
        guideCard.setPadding(new Insets(18, 20, 18, 20));
        guideCard.getStyleClass().add("card-editorial");

        Label guideTitle = new Label("Alur Aktivasi Akun Organizer");
        guideTitle.setFont(TiketinTheme.fontBold(14));
        guideTitle.setStyle("-fx-text-fill: #20252A;");

        guideCard.getChildren().addAll(
                guideTitle,
                createGuideStep("1", "Lengkapi identitas penanggung jawab & organisasi penyelenggara."),
                createGuideStep("2", "Ajukan proposal acara perdana yang siap dipublikasikan di Tiketin."),
                createGuideStep("3", "Setelah disetujui Admin, akun Anda langsung mendapat akses Studio Organizer & verifikasi tiket.")
        );

        VBox leftColumn = new VBox(16, profileCardContent, guideCard);

        // Section 2: Proposal Acara Perdana
        VBox eventCardContent = new VBox(12);
        eventCardContent.setPadding(new Insets(20));
        eventCardContent.getStyleClass().add("card-editorial");

        Label sec2Title = new Label("2. Proposal Acara Perdana");
        sec2Title.setFont(TiketinTheme.fontBold(15));
        sec2Title.setStyle("-fx-text-fill: #20252A;");

        eventNameField = TiketinTheme.createTextField("Nama Acara Perdana");
        if (prefillFromRejected && latestApp.getEventName() != null) {
            eventNameField.setText(latestApp.getEventName());
        }
        eventNameField.setDisable(isDisabled);

        eventCategoryCombo = new ComboBox<>();
        eventCategoryCombo.getItems().addAll("Konser", "Seminar", "Wisata", "Festival", "Olahraga", "Lainnya");
        eventCategoryCombo.getStyleClass().add("combo-box-editorial");
        eventCategoryCombo.setMaxWidth(Double.MAX_VALUE);
        if (prefillFromRejected && latestApp.getEventCategory() != null && eventCategoryCombo.getItems().contains(latestApp.getEventCategory())) {
            eventCategoryCombo.setValue(latestApp.getEventCategory());
        } else {
            eventCategoryCombo.getSelectionModel().select(0);
        }
        eventCategoryCombo.setDisable(isDisabled);

        LocalDate defaultDate = LocalDate.now().plusDays(14);
        if (prefillFromRejected && latestApp.getEventDate() != null) {
            LocalDate parsed = DateUtil.parseDateSafe(latestApp.getEventDate());
            if (parsed != null) defaultDate = parsed;
        }
        eventDatePicker = new DatePicker(defaultDate);
        eventDatePicker.setMaxWidth(Double.MAX_VALUE);
        eventDatePicker.setDisable(isDisabled);

        eventTimeField = TiketinTheme.createTextField("Contoh: 15:00 - Selesai");
        if (prefillFromRejected && latestApp.getEventTime() != null) {
            eventTimeField.setText(latestApp.getEventTime());
        }
        eventTimeField.setDisable(isDisabled);

        eventLocationField = TiketinTheme.createTextField("Lokasi / Venue Acara");
        if (prefillFromRejected && latestApp.getEventLocation() != null) {
            eventLocationField.setText(latestApp.getEventLocation());
        }
        eventLocationField.setDisable(isDisabled);

        eventPriceField = TiketinTheme.createTextField("Harga Tiket (0 jika gratis)");
        eventPriceField.setText(prefillFromRejected ? String.valueOf(latestApp.getEventPrice()) : "75000");
        eventPriceField.setDisable(isDisabled);

        eventSlotsField = TiketinTheme.createTextField("Jumlah Kuota Tiket");
        eventSlotsField.setText(prefillFromRejected ? String.valueOf(latestApp.getEventSlots()) : "150");
        eventSlotsField.setDisable(isDisabled);

        eventDescArea = new TextArea();
        eventDescArea.setPromptText("Deskripsikan detail acara perdana Anda...");
        if (prefillFromRejected && latestApp.getEventDescription() != null) {
            eventDescArea.setText(latestApp.getEventDescription());
        }
        eventDescArea.setWrapText(true);
        eventDescArea.setPrefRowCount(3);
        eventDescArea.setFont(TiketinTheme.fontRegular(13));
        eventDescArea.setDisable(isDisabled);

        GridPane dateCatGrid = new GridPane();
        dateCatGrid.setHgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);
        dateCatGrid.getColumnConstraints().addAll(c1, c2);
        dateCatGrid.add(TiketinTheme.createFieldGroup("Kategori", eventCategoryCombo), 0, 0);
        dateCatGrid.add(TiketinTheme.createFieldGroup("Tanggal Event", eventDatePicker), 1, 0);

        GridPane priceSlotGrid = new GridPane();
        priceSlotGrid.setHgap(12);
        priceSlotGrid.getColumnConstraints().addAll(c1, c2);
        priceSlotGrid.add(TiketinTheme.createFieldGroup("Harga Tiket (Rp)", eventPriceField), 0, 0);
        priceSlotGrid.add(TiketinTheme.createFieldGroup("Kuota Tiket", eventSlotsField), 1, 0);

        eventImageUploader = new EventImageUploadPanel();
        if (latestApp != null && latestApp.getImagePath() != null && !latestApp.getImagePath().isBlank()) {
            eventImageUploader.setExistingFilename(latestApp.getImagePath());
        }
        eventImageUploader.setDisabledState(isDisabled);

        eventCardContent.getChildren().addAll(
                sec2Title,
                eventImageUploader,
                TiketinTheme.createFieldGroup("Nama Event", eventNameField),
                dateCatGrid,
                TiketinTheme.createFieldGroup("Waktu Event", eventTimeField),
                TiketinTheme.createFieldGroup("Lokasi / Venue", eventLocationField),
                priceSlotGrid,
                TiketinTheme.createFieldGroup("Deskripsi Event", eventDescArea)
        );

        // Two-column responsive GridPane (42% / 58%)
        GridPane columnsGrid = new GridPane();
        columnsGrid.setHgap(20);
        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setPercentWidth(42);
        leftCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setPercentWidth(58);
        rightCol.setHgrow(Priority.ALWAYS);
        columnsGrid.getColumnConstraints().addAll(leftCol, rightCol);
        columnsGrid.add(leftColumn, 0, 0);
        columnsGrid.add(eventCardContent, 1, 0);

        contentContainer.getChildren().add(columnsGrid);

        if (!isDisabled) {
            HBox submitBox = new HBox();
            submitBox.setAlignment(Pos.CENTER_RIGHT);
            submitBox.setPadding(new Insets(4, 0, 12, 0));

            submitBtn = TiketinTheme.createPrimaryButton("Kirim Pengajuan Organizer", "arrow-right");
            submitBtn.setOnAction(e -> handleSubmission(user));
            submitBox.getChildren().add(submitBtn);

            contentContainer.getChildren().add(submitBox);
        }
    }

    private HBox createGuideStep(String num, String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label badge = new Label(num);
        badge.setMinSize(22, 22);
        badge.setPrefSize(22, 22);
        badge.setAlignment(Pos.CENTER);
        badge.setFont(TiketinTheme.fontBold(11.5));
        badge.setStyle("-fx-background-color: #FFF0EB; -fx-text-fill: #E06338; -fx-background-radius: 999;");

        Label lbl = new Label(text);
        lbl.setFont(TiketinTheme.fontRegular(12.5));
        lbl.setStyle("-fx-text-fill: #525A62;");
        lbl.setWrapText(true);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        row.getChildren().addAll(badge, lbl);
        return row;
    }

    private void handleSubmission(User user) {
        if (user == null || !"user".equalsIgnoreCase(user.getRole())) {
            ToastNotification.error(mainFrame, "Hanya akun pengguna biasa yang dapat mengajukan Organizer.");
            return;
        }
        String fullName = fullNameField.getText().trim();
        String contact = contactField.getText().trim();
        String orgName = orgNameField.getText().trim();

        String evName = eventNameField.getText().trim();
        String evCategory = eventCategoryCombo.getValue();
        LocalDate evDate = eventDatePicker.getValue();
        String evTime = eventTimeField.getText().trim();
        String evLocation = eventLocationField.getText().trim();
        String priceStr = eventPriceField.getText().trim();
        String slotsStr = eventSlotsField.getText().trim();
        String evDesc = eventDescArea.getText().trim();

        String err = ValidationUtil.validateRequired(fullName, "Nama lengkap");
        if (err == null) err = ValidationUtil.validateContact(contact);
        if (err == null) err = ValidationUtil.validateRequired(orgName, "Nama organisasi / komunitas");
        if (err == null) err = ValidationUtil.validateRequired(evName, "Nama acara");
        if (err == null) err = ValidationUtil.validateRequired(evLocation, "Lokasi / venue acara");
        if (err == null) err = ValidationUtil.validateFutureSchedule(evDate, evTime);
        if (err != null) {
            ToastNotification.error(mainFrame, err);
            return;
        }

        int price;
        int slots;
        try {
            price = Integer.parseInt(priceStr);
            slots = Integer.parseInt(slotsStr);
            if (price < 0 || slots <= 0) {
                ToastNotification.error(mainFrame, "Harga minimal 0 dan kuota tiket harus lebih dari 0.");
                return;
            }
        } catch (NumberFormatException e) {
            ToastNotification.error(mainFrame, "Harga dan Kuota harus berupa angka!");
            return;
        }
        if (eventImageUploader == null || eventImageUploader.isEmpty()) {
            String msg = "Foto acara wajib diunggah.";
            if (eventImageUploader != null) eventImageUploader.showError(msg);
            ToastNotification.error(mainFrame, msg);
            return;
        }

        Application app = new Application();
        app.setUserId(user.getId());
        app.setFullName(fullName);
        app.setContact(contact);
        app.setOrganization(orgName);
        app.setEventName(evName);
        app.setEventCategory(evCategory != null ? evCategory : "Lainnya");
        app.setEventDate(evDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        app.setEventTime(evTime);
        app.setEventLocation(evLocation);
        app.setEventPrice(price);
        app.setEventSlots(slots);
        app.setEventDescription(evDesc);

        submitBtn.setDisable(true);
        TiketinTheme.runAsync(() -> {
            app.setImagePath(eventImageUploader.commitImageToStorage());
            new ApplicationDAO().submitApplication(app);
            return true;
        }, success -> {
            ToastNotification.success(mainFrame, "Pengajuan berhasil dikirim! Menunggu tinjauan admin.");
            refreshData();
        }, errMsg -> {
            submitBtn.setDisable(false);
            ToastNotification.error(mainFrame, "Gagal mengirim pengajuan: " + errMsg.getMessage());
        });
    }
}
