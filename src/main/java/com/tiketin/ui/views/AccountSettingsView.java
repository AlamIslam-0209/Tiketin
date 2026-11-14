package com.tiketin.ui.views;

import com.tiketin.dao.UserDAO;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ProofStorageUtil;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * In-frame Account Settings page (Foto Profil, Nama, Username, Organisasi, Bio/Deskripsi, Password)
 * rendered inside MainFrame without opening a new Stage window.
 */
public class AccountSettingsView extends VBox {
    private final MainFrame mainFrame;
    private final UserDAO userDAO = new UserDAO();

    private StackPane avatarPreviewHolder;
    private Label previewNameLbl;
    private Label previewHandleLbl;
    private Label previewBioLbl;
    private Button removePhotoBtn;

    private TextField fullNameField;
    private TextField usernameField;
    private TextField orgField;
    private TextArea bioArea;
    private PasswordField newPassField;
    private PasswordField confirmPassField;
    private AnimatedButton saveBtn;

    private File pendingAvatarFile;
    private boolean avatarRemoved = false;
    private String currentSavedAvatar;

    public AccountSettingsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(0);
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        VBox scrollBody = new VBox(20);
        scrollBody.setPadding(new Insets(28, 36, 36, 36));
        scrollBody.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        // Header bar
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("Kembali");
        backBtn.setGraphic(TiketinIcons.getIcon("arrow-left", 15, TiketinTheme.TEXT_SECONDARY));
        backBtn.setGraphicTextGap(6);
        backBtn.getStyleClass().setAll("btn-back-link");
        backBtn.setOnAction(e -> mainFrame.navigateTo(MainFrame.VIEW_HOME));

        Label pageTitle = new Label("Pengaturan Akun");
        pageTitle.setFont(TiketinTheme.fontBold(26));
        pageTitle.setStyle("-fx-text-fill: #20252A;");

        VBox headerBox = new VBox(4, backBtn, pageTitle);
        headerRow.getChildren().add(headerBox);

        // 2-Column Responsive Grid: Left (35%) Profile Card + Avatar, Right (65%) Edit Form
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);

        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setPercentWidth(36);
        colLeft.setHgrow(Priority.ALWAYS);
        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setPercentWidth(64);
        colRight.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(colLeft, colRight);

        VBox leftCard = buildAvatarProfileCard();
        VBox rightCard = buildFormCard();
        GridPane.setValignment(leftCard, javafx.geometry.VPos.TOP);
        GridPane.setValignment(rightCard, javafx.geometry.VPos.TOP);

        grid.add(leftCard, 0, 0);
        grid.add(rightCard, 1, 0);

        scrollBody.getChildren().addAll(headerRow, grid);

        ScrollPane scrollPane = new ScrollPane(scrollBody);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private VBox buildAvatarProfileCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("card-editorial");

        avatarPreviewHolder = new StackPane();
        avatarPreviewHolder.setMinSize(104, 104);
        avatarPreviewHolder.setPrefSize(104, 104);
        avatarPreviewHolder.setMaxSize(104, 104);
        avatarPreviewHolder.setStyle("-fx-background-color: #E06338; -fx-background-radius: 999; -fx-cursor: hand;");

        // Support drag-and-drop avatar directly onto preview card
        card.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        card.setOnDragDropped(e -> {
            var db = e.getDragboard();
            boolean ok = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                ok = trySelectAvatarFile(db.getFiles().get(0));
            }
            e.setDropCompleted(ok);
            e.consume();
        });
        avatarPreviewHolder.setOnMouseClicked(e -> openAvatarPicker());

        previewNameLbl = new Label();
        previewNameLbl.setFont(TiketinTheme.fontBold(17));
        previewNameLbl.setStyle("-fx-text-fill: #20252A;");
        previewNameLbl.setWrapText(true);
        previewNameLbl.setAlignment(Pos.CENTER);

        previewHandleLbl = new Label();
        previewHandleLbl.setFont(TiketinTheme.fontRegular(13));
        previewHandleLbl.setStyle("-fx-text-fill: #687078;");

        previewBioLbl = new Label();
        previewBioLbl.setFont(TiketinTheme.fontRegular(12.5));
        previewBioLbl.setStyle("-fx-text-fill: #525A62;");
        previewBioLbl.setWrapText(true);
        previewBioLbl.setAlignment(Pos.CENTER);
        previewBioLbl.setMaxWidth(Double.MAX_VALUE);

        HBox avatarBtns = new HBox(8);
        avatarBtns.setAlignment(Pos.CENTER);

        Button choosePhotoBtn = TiketinTheme.createSecondaryButton("Pilih Foto", "upload");
        choosePhotoBtn.setOnAction(e -> openAvatarPicker());

        removePhotoBtn = TiketinTheme.createDangerButton("Hapus", "trash");
        removePhotoBtn.setOnAction(e -> {
            pendingAvatarFile = null;
            avatarRemoved = true;
            refreshAvatarVisual(null);
        });

        avatarBtns.getChildren().addAll(choosePhotoBtn, removePhotoBtn);

        Label dragHint = new Label("Klik atau seret gambar PNG/JPG ke kartu ini.");
        dragHint.setFont(TiketinTheme.fontRegular(11.5));
        dragHint.setStyle("-fx-text-fill: #687078;");
        dragHint.setWrapText(true);
        dragHint.setAlignment(Pos.CENTER);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #E3E6E2;");

        card.getChildren().addAll(avatarPreviewHolder, avatarBtns, dragHint, sep, previewNameLbl, previewHandleLbl, previewBioLbl);
        return card;
    }

    private VBox buildFormCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("card-editorial");

        Label secProfile = new Label("Informasi Profil");
        secProfile.setFont(TiketinTheme.fontBold(15.5));
        secProfile.setStyle("-fx-text-fill: #20252A;");

        fullNameField = TiketinTheme.createTextField("Nama Lengkap");
        usernameField = TiketinTheme.createTextField("Username");
        orgField = TiketinTheme.createTextField("Organisasi / Komunitas (opsional)");

        bioArea = new TextArea();
        bioArea.setPromptText("Ceritakan sedikit tentang Anda atau aktivitas acara Anda...");
        bioArea.setWrapText(true);
        bioArea.setPrefRowCount(3);
        bioArea.setMinHeight(82);

        fullNameField.textProperty().addListener((o, a, b) -> syncLivePreview());
        usernameField.textProperty().addListener((o, a, b) -> syncLivePreview());
        bioArea.textProperty().addListener((o, a, b) -> syncLivePreview());

        GridPane topFields = new GridPane();
        topFields.setHgap(14);
        topFields.setVgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            topFields.getColumnConstraints().add(cc);
        }
        topFields.add(TiketinTheme.createFieldGroup("Nama Lengkap", fullNameField), 0, 0);
        topFields.add(TiketinTheme.createFieldGroup("Username", usernameField), 1, 0);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #E3E6E2;");
        VBox.setMargin(sep, new Insets(4, 0, 2, 0));

        Label secSecurity = new Label("Ubah Password (Opsional)");
        secSecurity.setFont(TiketinTheme.fontBold(15));
        secSecurity.setStyle("-fx-text-fill: #20252A;");

        newPassField = TiketinTheme.createPasswordField("Kosongkan jika tidak ingin mengubah");
        confirmPassField = TiketinTheme.createPasswordField("Ulangi password baru");

        GridPane passGrid = new GridPane();
        passGrid.setHgap(14);
        passGrid.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            passGrid.getColumnConstraints().add(cc);
        }
        passGrid.add(TiketinTheme.createFieldGroup("Password Baru", newPassField), 0, 0);
        passGrid.add(TiketinTheme.createFieldGroup("Konfirmasi Password", confirmPassField), 1, 0);

        saveBtn = TiketinTheme.createPrimaryButton("Simpan Perubahan", "check");
        saveBtn.setOnAction(e -> handleSave());

        HBox footerRow = new HBox(saveBtn);
        footerRow.setAlignment(Pos.CENTER_RIGHT);
        footerRow.setPadding(new Insets(6, 0, 0, 0));

        card.getChildren().addAll(
                secProfile,
                topFields,
                TiketinTheme.createFieldGroup("Organisasi / Komunitas", orgField),
                TiketinTheme.createFieldGroup("Deskripsi / Bio", bioArea),
                sep,
                secSecurity,
                passGrid,
                footerRow
        );
        return card;
    }

    public void refreshData() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            mainFrame.openAuth(MainFrame.VIEW_SETTINGS, () -> mainFrame.navigateTo(MainFrame.VIEW_SETTINGS));
            return;
        }
        pendingAvatarFile = null;
        avatarRemoved = false;
        currentSavedAvatar = currentUser.getAvatarPath();

        fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        usernameField.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "");
        orgField.setText(currentUser.getOrganization() != null ? currentUser.getOrganization() : "");
        bioArea.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
        newPassField.clear();
        confirmPassField.clear();

        File existingAvatar = ProofStorageUtil.findAvatarFile(currentSavedAvatar);
        refreshAvatarVisual(existingAvatar);
        syncLivePreview();
    }

    private void syncLivePreview() {
        String name = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String user = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String bio = bioArea.getText() != null ? bioArea.getText().trim() : "";

        previewNameLbl.setText(name.isEmpty() ? "Nama Pengguna" : name);
        previewHandleLbl.setText("@" + (user.isEmpty() ? "username" : user));
        previewBioLbl.setText(bio.isEmpty() ? "Belum ada deskripsi profil." : bio);

        if (pendingAvatarFile == null && (avatarRemoved || ProofStorageUtil.findAvatarFile(currentSavedAvatar) == null)) {
            refreshAvatarVisual(null);
        }
    }

    private void openAvatarPicker() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Gambar (PNG, JPG, WEBP, GIF)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null) {
            trySelectAvatarFile(f);
        }
    }

    private boolean trySelectAvatarFile(File file) {
        String err = ValidationUtil.validateImageFile(file);
        if (err != null) {
            ToastNotification.error(mainFrame, err);
            return false;
        }
        try {
            Image img = new Image(file.toURI().toString(), 208, 208, true, true, false);
            if (img.isError() || img.getWidth() <= 0 || img.getHeight() <= 0) {
                ToastNotification.error(mainFrame, "File foto profil rusak atau tidak dapat dibaca.");
                return false;
            }
            selectAvatarFile(file);
            return true;
        } catch (Exception ex) {
            ToastNotification.error(mainFrame, "Gagal memuat foto profil: " + ex.getMessage());
            return false;
        }
    }

    private void selectAvatarFile(File file) {
        this.pendingAvatarFile = file;
        this.avatarRemoved = false;
        refreshAvatarVisual(file);
    }

    private void refreshAvatarVisual(File imageFile) {
        avatarPreviewHolder.getChildren().clear();
        boolean hasPhoto = false;
        if (imageFile != null && imageFile.exists()) {
            try {
                Image img = new Image(imageFile.toURI().toString(), 208, 208, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(104);
                    iv.setFitHeight(104);
                    iv.setPreserveRatio(false);
                    iv.setClip(new Circle(52, 52, 52));
                    avatarPreviewHolder.setStyle("-fx-background-color: #E3E6E2; -fx-background-radius: 999; -fx-cursor: hand;");
                    avatarPreviewHolder.getChildren().add(iv);
                    hasPhoto = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!hasPhoto) {
            avatarPreviewHolder.setStyle("-fx-background-color: #E06338; -fx-background-radius: 999; -fx-cursor: hand;");
            String nm = fullNameField != null && fullNameField.getText() != null && !fullNameField.getText().trim().isEmpty()
                    ? fullNameField.getText().trim()
                    : "U";
            Label init = new Label(nm.substring(0, 1).toUpperCase());
            init.setFont(TiketinTheme.fontBold(36));
            init.setTextFill(Color.WHITE);
            avatarPreviewHolder.getChildren().add(init);
        }
        if (removePhotoBtn != null) {
            removePhotoBtn.setVisible(hasPhoto);
            removePhotoBtn.setManaged(hasPhoto);
        }
    }

    private void handleSave() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        String full = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String uname = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String org = orgField.getText() != null ? orgField.getText().trim() : "";
        String bio = bioArea.getText() != null ? bioArea.getText().trim() : "";
        String pass = newPassField.getText() != null ? newPassField.getText() : "";
        String confirm = confirmPassField.getText() != null ? confirmPassField.getText() : "";

        String nameErr = ValidationUtil.validateRequired(full, "Nama lengkap");
        if (nameErr != null) {
            ToastNotification.error(mainFrame, nameErr);
            fullNameField.requestFocus();
            return;
        }
        String userErr = ValidationUtil.validateUsername(uname);
        if (userErr != null) {
            ToastNotification.error(mainFrame, userErr);
            usernameField.requestFocus();
            return;
        }
        if (!pass.isEmpty()) {
            String passErr = ValidationUtil.validatePassword(pass);
            if (passErr != null) {
                ToastNotification.error(mainFrame, passErr);
                newPassField.requestFocus();
                return;
            }
            if (!pass.equals(confirm)) {
                ToastNotification.error(mainFrame, "Konfirmasi password baru tidak cocok.");
                confirmPassField.requestFocus();
                return;
            }
        }

        String nextAvatarPath = avatarRemoved ? null : currentSavedAvatar;
        if (pendingAvatarFile != null) {
            String imgErr = ValidationUtil.validateImageFile(pendingAvatarFile);
            if (imgErr != null) {
                ToastNotification.error(mainFrame, imgErr);
                return;
            }
            try {
                nextAvatarPath = ProofStorageUtil.saveAvatar(pendingAvatarFile);
            } catch (Exception ex) {
                ToastNotification.error(mainFrame, "Gagal menyimpan foto profil: " + ex.getMessage());
                return;
            }
        }
        final String finalAvatarPath = nextAvatarPath;

        saveBtn.setDisable(true);
        TiketinTheme.runAsync(() ->
                userDAO.updateProfile(currentUser.getId(), uname, full, org, bio, finalAvatarPath, pass.isEmpty() ? null : pass),
            updated -> {
                saveBtn.setDisable(false);
                if (updated != null) {
                    SessionManager.login(updated);
                    ToastNotification.success(mainFrame, "Profil akun berhasil disimpan.");
                } else {
                    ToastNotification.error(mainFrame, "Gagal menyimpan profil.");
                }
            },
            err -> {
                saveBtn.setDisable(false);
                ToastNotification.error(mainFrame, err.getMessage());
            }
        );
    }
}
