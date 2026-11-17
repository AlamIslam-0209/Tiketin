package com.tiketin.ui.dialogs;

import com.tiketin.dao.EventDAO;
import com.tiketin.model.Event;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pure JavaFX modal dialog for creating or editing an event.
 */
public class EventFormDialog {

    private static boolean savedResult = false;

    public static boolean showDialog(Object owner, Event existingEvent, int defaultOwnerId) {
        savedResult = false;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existingEvent == null ? "Buat Acara Baru" : "Edit Acara");
        stage.setResizable(false);

        Window ownerWin = TiketinTheme.resolveWindow(owner);
        if (ownerWin != null) {
            stage.initOwner(ownerWin);
        }

        VBox shell = new VBox();
        shell.setStyle("-fx-background-color: #F7F8F6;");
        shell.setPrefWidth(560);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-width: 0 0 1 0;");
        Label titleLbl = new Label(existingEvent == null ? "Buat Acara Baru" : "Edit Acara");
        titleLbl.setFont(TiketinTheme.fontBold(16));
        titleLbl.setStyle("-fx-text-fill: #20252A;");
        header.getChildren().add(titleLbl);

        // Form content card
        VBox formContent = new VBox(14);
        formContent.setPadding(new Insets(20, 22, 20, 22));
        formContent.getStyleClass().add("card-editorial");

        TextField nameField = TiketinTheme.createTextField("Contoh: Konser Musik Senja");
        if (existingEvent != null && existingEvent.getName() != null) {
            nameField.setText(existingEvent.getName());
        }

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Konser", "Seminar", "Wisata", "Festival", "Olahraga", "Lainnya");
        categoryCombo.getStyleClass().add("combo-box-editorial");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        if (existingEvent != null && existingEvent.getCategory() != null && !existingEvent.getCategory().isBlank()) {
            String cat = existingEvent.getCategory().trim();
            if (!categoryCombo.getItems().contains(cat)) {
                categoryCombo.getItems().add(cat);
            }
            categoryCombo.setValue(cat);
        } else {
            categoryCombo.getSelectionModel().select(0);
        }

        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (existingEvent != null && existingEvent.getDate() != null) {
            LocalDate parsed = DateUtil.parseDateSafe(existingEvent.getDate());
            datePicker.setValue(parsed != null ? parsed : LocalDate.now().plusDays(7));
        } else {
            datePicker.setValue(LocalDate.now().plusDays(7));
        }

        TextField timeField = TiketinTheme.createTextField("Contoh: 19:00 WIB");
        timeField.setText(existingEvent != null && existingEvent.getTime() != null ? existingEvent.getTime() : "19:00 WIB");

        TextField locationField = TiketinTheme.createTextField("Contoh: Balai Sarbini, Jakarta");
        if (existingEvent != null && existingEvent.getLocation() != null) {
            locationField.setText(existingEvent.getLocation());
        }

        TextField priceField = TiketinTheme.createTextField("Contoh: 50000");
        priceField.setText(existingEvent != null ? String.valueOf(existingEvent.getPrice()) : "50000");

        TextField slotsField = TiketinTheme.createTextField("Contoh: 100");
        slotsField.setText(existingEvent != null ? String.valueOf(existingEvent.getSlots()) : "100");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Deskripsikan acara secara lengkap...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        descArea.setFont(TiketinTheme.fontRegular(13));
        if (existingEvent != null && existingEvent.getDescription() != null) {
            descArea.setText(existingEvent.getDescription());
        }

        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHgrow(Priority.ALWAYS);

        GridPane dateTimeGrid = new GridPane();
        dateTimeGrid.setHgap(14);
        dateTimeGrid.getColumnConstraints().addAll(half, half);
        dateTimeGrid.add(TiketinTheme.createFieldGroup("Tanggal Acara", datePicker), 0, 0);
        dateTimeGrid.add(TiketinTheme.createFieldGroup("Waktu", timeField), 1, 0);

        GridPane priceSlotsGrid = new GridPane();
        priceSlotsGrid.setHgap(14);
        priceSlotsGrid.getColumnConstraints().addAll(half, half);
        priceSlotsGrid.add(TiketinTheme.createFieldGroup("Harga Tiket (Rp)", priceField), 0, 0);
        priceSlotsGrid.add(TiketinTheme.createFieldGroup("Kuota / Slot", slotsField), 1, 0);

        formContent.getChildren().addAll(
                TiketinTheme.createFieldGroup("Nama Event", nameField),
                TiketinTheme.createFieldGroup("Kategori", categoryCombo),
                dateTimeGrid,
                TiketinTheme.createFieldGroup("Lokasi", locationField),
                priceSlotsGrid,
                TiketinTheme.createFieldGroup("Deskripsi Acara", descArea)
        );

        VBox formWrap = new VBox(formContent);
        formWrap.setPadding(new Insets(16, 20, 16, 20));
        formWrap.setStyle("-fx-background-color: #F7F8F6;");

        ScrollPane formScroll = new ScrollPane(formWrap);
        formScroll.setFitToWidth(true);
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        // Buttons footer
        HBox footerBox = new HBox(12);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(14, 24, 14, 24));
        footerBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E6E2; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = TiketinTheme.createSecondaryButton("Batal", "close");
        cancelBtn.setOnAction(e -> stage.close());

        AnimatedButton saveBtn = TiketinTheme.createPrimaryButton("Simpan Acara", "check");
        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            LocalDate dateVal = datePicker.getValue();
            String time = timeField.getText().trim();
            String location = locationField.getText().trim();
            String priceStr = priceField.getText().trim();
            String slotsStr = slotsField.getText().trim();
            String desc = descArea.getText().trim();

            int parsedPrice;
            int parsedSlots;
            try {
                parsedPrice = Integer.parseInt(priceStr);
                parsedSlots = Integer.parseInt(slotsStr);
            } catch (NumberFormatException ex) {
                ToastNotification.error(stage, "Harga dan Kuota harus berupa angka!");
                return;
            }

            boolean isCreate = (existingEvent == null);
            String valErr = ValidationUtil.validateEventFields(name, location, dateVal, time, parsedPrice, parsedSlots, isCreate);
            if (valErr != null) {
                ToastNotification.error(stage, valErr);
                return;
            }

            final int price = parsedPrice;
            final int slots = parsedSlots;
            final int actorId = SessionManager.isLoggedIn()
                    ? SessionManager.getCurrentUser().getId()
                    : defaultOwnerId;

            saveBtn.setDisable(true);
            TiketinTheme.runAsync(() -> {
                EventDAO eventDAO = new EventDAO();
                boolean ok;
                if (isCreate) {
                    Event ev = new Event();
                    ev.setOwnerId(defaultOwnerId);
                    ev.setName(name);
                    ev.setCategory(category != null ? category : "Lainnya");
                    ev.setDate(dateVal.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    ev.setTime(time);
                    ev.setLocation(location);
                    ev.setPrice(price);
                    ev.setSlots(slots);
                    ev.setDescription(desc);
                    ok = eventDAO.createEvent(ev);
                } else {
                    existingEvent.setName(name);
                    existingEvent.setCategory(category != null ? category : "Lainnya");
                    existingEvent.setDate(dateVal.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    existingEvent.setTime(time);
                    existingEvent.setLocation(location);
                    existingEvent.setPrice(price);
                    existingEvent.setSlots(slots);
                    existingEvent.setDescription(desc);
                    ok = eventDAO.updateEvent(existingEvent, actorId);
                }
                if (!ok) {
                    throw new java.sql.SQLException("Tidak ada perubahan acara yang tersimpan.");
                }
                return true;
            }, success -> {
                savedResult = true;
                stage.close();
                ToastNotification.success(owner, "Acara berhasil disimpan!");
            }, err -> {
                saveBtn.setDisable(false);
                ToastNotification.error(stage, "Gagal menyimpan acara: " + err.getMessage());
            });
        });

        footerBox.getChildren().addAll(cancelBtn, saveBtn);

        shell.getChildren().addAll(header, formScroll, footerBox);

        shell.setPrefWidth(600);
        StackPane root = new StackPane(shell);
        Scene scene = new Scene(root, 600, 620);
        TiketinTheme.applyStylesheet(scene);
        stage.setScene(scene);
        stage.showAndWait();

        return savedResult;
    }

    public static boolean showDialog(Object owner, Event existingEvent) {
        int defaultOwner = com.tiketin.util.SessionManager.getCurrentUser() != null
                ? com.tiketin.util.SessionManager.getCurrentUser().getId()
                : 1;
        return showDialog(owner, existingEvent, defaultOwner);
    }
}
