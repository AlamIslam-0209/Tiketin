package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

public class ImageDropZonePanel extends StackPane {
    private File selectedFile;
    private Consumer<File> onFileSelected;
    private VBox placeholderBox;
    private VBox previewBox;
    private ImageView imageView;
    private Label fileNameLabel;

    public ImageDropZonePanel(Consumer<File> onFileSelected) {
        this.onFileSelected = onFileSelected;
        getStyleClass().add("card-surface");
        setStyle("-fx-border-color: #D4D4D8; -fx-border-style: dashed; -fx-cursor: hand;");
        setPadding(new Insets(12));
        setAlignment(Pos.CENTER);
        setMinHeight(128);

        placeholderBox = new VBox(5);
        placeholderBox.setAlignment(Pos.CENTER);
        Node uploadIcon = TiketinTheme.getIcon("upload", 24, TiketinTheme.ACCENT);
        Label titleLbl = new Label("Seret bukti bayar ke sini");
        titleLbl.setFont(TiketinTheme.fontBold(13));
        titleLbl.setStyle("-fx-text-fill: #20252A;");
        Label subLbl = new Label("PNG, JPG, JPEG, WEBP, atau GIF (Maks. 5MB)");
        subLbl.setFont(TiketinTheme.fontRegular(11.5));
        subLbl.setStyle("-fx-text-fill: #687078;");
        Button pickFileBtn = TiketinTheme.createSecondaryButton("Pilih File", "upload");
        pickFileBtn.setOnAction(e -> {
            e.consume();
            openFilePicker();
        });
        placeholderBox.getChildren().addAll(uploadIcon, titleLbl, subLbl, pickFileBtn);

        previewBox = new VBox(6);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setVisible(false);
        previewBox.setManaged(false);

        imageView = new ImageView();
        imageView.setFitWidth(96);
        imageView.setFitHeight(96);
        imageView.setPreserveRatio(true);

        fileNameLabel = new Label();
        fileNameLabel.setFont(TiketinTheme.fontBold(12));
        fileNameLabel.setTextFill(TiketinTheme.TEXT_PRIMARY);

        Button removeBtn = TiketinTheme.createDangerButton("Ganti / Hapus", "trash");
        removeBtn.setOnAction(e -> {
            e.consume();
            clearSelection();
        });

        previewBox.getChildren().addAll(imageView, fileNameLabel, removeBtn);

        getChildren().addAll(placeholderBox, previewBox);

        setOnMouseClicked(e -> openFilePicker());

        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            var db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                success = trySelectValidatedFile(file);
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    public void openFilePicker() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Gambar Bukti Pembayaran");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            trySelectValidatedFile(file);
        }
    }

    private boolean trySelectValidatedFile(File file) {
        String err = ValidationUtil.validateImageFile(file);
        if (err != null) {
            showToastError(err);
            return false;
        }
        try {
            Image img = new Image(file.toURI().toString(), 96, 96, true, true, false);
            if (img.isError() || img.getWidth() <= 0 || img.getHeight() <= 0) {
                showToastError("File gambar rusak atau tidak dapat dibaca.");
                return false;
            }
            setSelectedFile(file, img);
            return true;
        } catch (Exception ex) {
            showToastError("Gagal membaca gambar: " + ex.getMessage());
            return false;
        }
    }

    private void showToastError(String message) {
        Object owner = getScene() != null && getScene().getWindow() != null ? getScene().getWindow() : this;
        ToastNotification.error(owner, message);
    }

    public void setSelectedFile(File file) {
        if (file == null) {
            clearSelection();
            return;
        }
        trySelectValidatedFile(file);
    }

    private void setSelectedFile(File file, Image decodedImg) {
        this.selectedFile = file;
        imageView.setImage(decodedImg);
        long kb = Math.max(1, file.length() / 1024);
        String sizeText = kb >= 1024 ? String.format("%.1f MB", kb / 1024.0) : kb + " KB";
        fileNameLabel.setText(file.getName() + " (" + sizeText + ")");
        placeholderBox.setVisible(false);
        placeholderBox.setManaged(false);
        previewBox.setVisible(true);
        previewBox.setManaged(true);
        if (onFileSelected != null) {
            onFileSelected.accept(file);
        }
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void clearSelection() {
        this.selectedFile = null;
        imageView.setImage(null);
        fileNameLabel.setText("");
        previewBox.setVisible(false);
        previewBox.setManaged(false);
        placeholderBox.setVisible(true);
        placeholderBox.setManaged(true);
        if (onFileSelected != null) {
            onFileSelected.accept(null);
        }
    }
}
