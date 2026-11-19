package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ProofStorageUtil;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Komponen unggah 1 foto acara wajib (drag-and-drop + pemilih file OS)
 * dengan pratinjau bingkai 4:3 non-cropped agar komposisi foto utuh.
 */
public class EventImageUploadPanel extends VBox {

    private File pendingFile;
    private String existingFilename;
    private boolean disabledState;

    private final StackPane dropZone;
    private final VBox dropContent;
    private final HBox previewCard;
    private final ImageView previewImageView;
    private final Label titleLabel;
    private final Label metaLabel;
    private final Button chooseBtn;
    private final Button replaceBtn;
    private final Button removeBtn;
    private final Label errorLabel;

    public EventImageUploadPanel() {
        setSpacing(8);

        HBox headRow = new HBox(8);
        headRow.setAlignment(Pos.CENTER_LEFT);
        Label fieldLbl = new Label("Foto Acara *");
        fieldLbl.getStyleClass().add("form-label");

        Label reqBadge = new Label("Wajib 1 foto · Rasio 4:3 utuh");
        reqBadge.setStyle(
                "-fx-background-color: #FFF0EB; -fx-text-fill: #E06338;" +
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 2 8;"
        );
        headRow.getChildren().addAll(fieldLbl, reqBadge);

        Label hintLbl = new Label("Tampil utuh (fit) tanpa terpotong di kartu & halaman pemesanan · PNG, JPG, JPEG, WEBP maks. 5MB");
        hintLbl.setStyle("-fx-text-fill: #687078; -fx-font-size: 11px;");
        hintLbl.setWrapText(true);

        // 1. Empty drop zone
        dropZone = new StackPane();
        dropZone.getStyleClass().add("dropzone-box");
        dropZone.setPadding(new Insets(16, 18, 16, 18));
        dropZone.setMinHeight(128);
        dropZone.setCursor(Cursor.HAND);

        dropContent = new VBox(6);
        dropContent.setAlignment(Pos.CENTER);

        StackPane iconBubble = new StackPane();
        iconBubble.setMinSize(38, 38);
        iconBubble.setMaxSize(38, 38);
        iconBubble.setStyle("-fx-background-color: #FFF0EB; -fx-background-radius: 999;");
        iconBubble.getChildren().add(TiketinIcons.getIcon("image", 18, TiketinTheme.ACCENT));

        Label dropTitle = new Label("Seret & letakkan foto acara ke sini");
        dropTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #20252A;");

        Label dropSub = new Label("Atau pilih gambar terbaik dari komputer Anda (1 foto)");
        dropSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #525A62;");

        chooseBtn = new Button("Pilih Foto Acara");
        chooseBtn.getStyleClass().add("btn-outline-slate");
        chooseBtn.setGraphic(TiketinIcons.getIcon("upload", 13, TiketinTheme.TEXT_PRIMARY));
        chooseBtn.setGraphicTextGap(6);
        chooseBtn.setCursor(Cursor.HAND);
        chooseBtn.setOnAction(e -> openFilePicker());
        VBox.setMargin(chooseBtn, new Insets(4, 0, 0, 0));

        dropContent.getChildren().addAll(iconBubble, dropTitle, dropSub, chooseBtn);
        dropZone.getChildren().add(dropContent);
        dropZone.setOnMouseClicked(e -> {
            if (!disabledState && e.getTarget() != chooseBtn) {
                openFilePicker();
            }
        });
        installDropHandlers(dropZone);

        // 2. Preview card when a photo is selected
        previewCard = new HBox(14);
        previewCard.setAlignment(Pos.CENTER_LEFT);
        previewCard.setPadding(new Insets(12));
        previewCard.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 12;" +
                "-fx-border-color: #E3E6E2; -fx-border-radius: 12;"
        );
        installDropHandlers(previewCard);

        StackPane thumbBox = new StackPane();
        thumbBox.setMinSize(168, 126);
        thumbBox.setPrefSize(168, 126);
        thumbBox.setMaxSize(168, 126);
        thumbBox.setStyle(
                "-fx-background-color: " + EventImageSupport.MATTE_BG_HEX + "; -fx-background-radius: 10;" +
                "-fx-border-color: #E3E6E2; -fx-border-radius: 10;"
        );
        EventImageSupport.applyRoundedClip(thumbBox, 10);
        previewImageView = EventImageSupport.createFittedImageView(thumbBox, 4);
        thumbBox.getChildren().add(previewImageView);

        VBox infoCol = new VBox(6);
        infoCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        Label statusTag = new Label("FOTO UTAMA · FIT 4:3");
        statusTag.setStyle(
                "-fx-background-color: #ECFDF5; -fx-text-fill: #0F7B45;" +
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 2 8;"
        );

        titleLabel = new Label("Foto acara terpilih");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #20252A;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        metaLabel = new Label();
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #525A62;");
        metaLabel.setWrapText(true);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(actionRow, new Insets(2, 0, 0, 0));

        replaceBtn = new Button("Ganti Foto");
        replaceBtn.getStyleClass().add("btn-outline-slate");
        replaceBtn.setGraphic(TiketinIcons.getIcon("upload", 12, TiketinTheme.TEXT_PRIMARY));
        replaceBtn.setGraphicTextGap(6);
        replaceBtn.setCursor(Cursor.HAND);
        replaceBtn.setOnAction(e -> openFilePicker());

        removeBtn = new Button("Hapus");
        removeBtn.getStyleClass().add("btn-danger-outline");
        removeBtn.setGraphic(TiketinIcons.getIcon("trash", 12, Color.web("#C53030")));
        removeBtn.setGraphicTextGap(5);
        removeBtn.setCursor(Cursor.HAND);
        removeBtn.setOnAction(e -> clearSelection());

        actionRow.getChildren().addAll(replaceBtn, removeBtn);
        infoCol.getChildren().addAll(statusTag, titleLabel, metaLabel, actionRow);
        previewCard.getChildren().addAll(thumbBox, infoCol);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #C53030; -fx-font-size: 11.5px; -fx-font-weight: 600;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        getChildren().addAll(headRow, hintLbl, dropZone, previewCard, errorLabel);
        refreshUi();
    }

    private void installDropHandlers(javafx.scene.Node target) {
        target.setOnDragOver(e -> {
            if (!disabledState && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                if (!dropZone.getStyleClass().contains("dropzone-box-active")) {
                    dropZone.getStyleClass().add("dropzone-box-active");
                }
            }
            e.consume();
        });
        target.setOnDragExited(e -> {
            dropZone.getStyleClass().remove("dropzone-box-active");
            e.consume();
        });
        target.setOnDragDropped(e -> {
            dropZone.getStyleClass().remove("dropzone-box-active");
            boolean ok = false;
            Dragboard db = e.getDragboard();
            if (!disabledState && db.hasFiles() && !db.getFiles().isEmpty()) {
                applyCandidateFile(db.getFiles().get(0));
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private void openFilePicker() {
        if (disabledState || getScene() == null || getScene().getWindow() == null) {
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Acara (PNG, JPG, JPEG, WEBP — Maks. 5MB)");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("File Gambar", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        List<File> files = fc.showOpenMultipleDialog(getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            applyCandidateFile(files.get(0));
        }
    }

    private void applyCandidateFile(File file) {
        clearError();
        if (file == null) {
            return;
        }
        String err = ValidationUtil.validateImageFile(file);
        if (err != null) {
            showError(err);
            return;
        }
        Image decoded = EventImageSupport.loadLocalImageFile(file, 960, 720);
        if (decoded == null || decoded.isError() || decoded.getWidth() <= 0) {
            showError("File gambar rusak atau tidak dapat dibaca.");
            return;
        }
        pendingFile = file;
        existingFilename = null;
        previewImageView.setImage(decoded);
        titleLabel.setText(file.getName());
        metaLabel.setText(String.format(Locale.ROOT, "Ukuran %.1f KB · Siap diunggah & disimpan", file.length() / 1024.0));
        refreshUi();
    }

    public void setExistingFilename(String filename) {
        clearError();
        pendingFile = null;
        String clean = filename != null && !filename.trim().isEmpty() ? filename.trim() : null;
        existingFilename = clean;
        if (clean != null) {
            Image img = EventImageSupport.loadStoredEventImage(clean, 960, 720);
            if (img != null && !img.isError()) {
                previewImageView.setImage(img);
                titleLabel.setText(clean);
                metaLabel.setText("Foto acara aktif tersimpan · klik Ganti Foto bila ingin memperbarui");
            } else {
                existingFilename = null;
                previewImageView.setImage(null);
                showError("Foto lama (" + clean + ") tidak ditemukan di penyimpanan. Silakan unggah ulang foto acara.");
            }
        } else {
            previewImageView.setImage(null);
        }
        refreshUi();
    }

    public void clearSelection() {
        pendingFile = null;
        existingFilename = null;
        previewImageView.setImage(null);
        clearError();
        refreshUi();
    }

    public boolean isEmpty() {
        return pendingFile == null && (existingFilename == null || existingFilename.trim().isEmpty());
    }

    public String commitImageToStorage() throws IOException {
        if (pendingFile != null) {
            String err = ValidationUtil.validateImageFile(pendingFile);
            if (err != null) {
                throw new IOException(err);
            }
            String stored = ProofStorageUtil.saveEventImage(pendingFile);
            existingFilename = stored;
            pendingFile = null;
            return stored;
        }
        return existingFilename;
    }

    public void setDisabledState(boolean disabled) {
        this.disabledState = disabled;
        chooseBtn.setDisable(disabled);
        replaceBtn.setDisable(disabled);
        removeBtn.setDisable(disabled);
        dropZone.setOpacity(disabled ? 0.65 : 1.0);
        dropZone.setCursor(disabled ? Cursor.DEFAULT : Cursor.HAND);
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void showError(String message) {
        errorLabel.setText(message != null ? message : "");
        boolean hasMsg = message != null && !message.trim().isEmpty();
        errorLabel.setVisible(hasMsg);
        errorLabel.setManaged(hasMsg);
    }

    private void refreshUi() {
        boolean hasSelection = !isEmpty();
        dropZone.setVisible(!hasSelection);
        dropZone.setManaged(!hasSelection);
        previewCard.setVisible(hasSelection);
        previewCard.setManaged(hasSelection);
    }
}
