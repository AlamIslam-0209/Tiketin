package com.tiketin.ui.views;

import com.tiketin.dao.UserDAO;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * In-frame authentication view (Masuk & Daftar) rendered inside MainFrame
 * without opening a secondary Stage window.
 */
public class AuthView extends VBox {
    private final MainFrame mainFrame;
    private final UserDAO userDAO = new UserDAO();

    private boolean isRegisterMode = false;
    private Runnable onAuthSuccess;

    private Button tabLoginBtn;
    private Button tabRegisterBtn;
    private Label headingLbl;
    private Label helperLbl;

    private VBox fullNameGroup;
    private TextField fullNameField;
    private TextField usernameField;
    private PasswordField passwordField;
    private AnimatedButton submitBtn;

    public AuthView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setSpacing(20);
        setPadding(new Insets(28, 36, 36, 36));
        setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_APP) + ";");

        // Top Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("Kembali");
        backBtn.setGraphic(TiketinIcons.getIcon("arrow-left", 15, TiketinTheme.TEXT_SECONDARY));
        backBtn.setGraphicTextGap(8);
        backBtn.getStyleClass().setAll("btn-back-link");
        backBtn.setOnAction(e -> mainFrame.cancelAuthFlow());
        topBar.getChildren().add(backBtn);

        // Centered Standalone Brand + Card Container (rendered without Sidebar)
        StackPane centerWrapper = new StackPane();
        VBox.setVgrow(centerWrapper, Priority.ALWAYS);

        VBox brandAndCard = new VBox(18);
        brandAndCard.setAlignment(Pos.CENTER);
        brandAndCard.setMaxWidth(420);
        brandAndCard.setMaxHeight(Region.USE_PREF_SIZE);

        HBox brandWordmark = new HBox(0);
        brandWordmark.setAlignment(Pos.CENTER);
        Label brandTitle = new Label("Tiketin");
        brandTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #20252A;");
        Label brandDot = new Label(".");
        brandDot.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #E06338;");
        brandWordmark.getChildren().addAll(brandTitle, brandDot);

        VBox authCard = new VBox(18);
        authCard.setMaxWidth(420);
        authCard.setPrefWidth(420);
        authCard.setMaxHeight(Region.USE_PREF_SIZE);
        authCard.setPadding(new Insets(28, 28, 28, 28));
        authCard.getStyleClass().add("card-editorial");

        VBox headBox = new VBox(4);
        headingLbl = new Label("Masuk ke Tiketin");
        headingLbl.setFont(TiketinTheme.fontBold(22));
        headingLbl.setStyle("-fx-text-fill: #20252A;");

        helperLbl = new Label("Gunakan akun Anda untuk memesan dan mengelola tiket.");
        helperLbl.setFont(TiketinTheme.fontRegular(13));
        helperLbl.setStyle("-fx-text-fill: #525A62;");
        helperLbl.setWrapText(true);
        headBox.getChildren().addAll(headingLbl, helperLbl);

        // Underline mode tabs
        HBox modeTabBar = new HBox(22);
        modeTabBar.setAlignment(Pos.CENTER_LEFT);
        modeTabBar.getStyleClass().add("underline-tab-bar");

        tabLoginBtn = new Button("Masuk");
        tabRegisterBtn = new Button("Daftar");
        tabLoginBtn.setOnAction(e -> switchMode(false));
        tabRegisterBtn.setOnAction(e -> switchMode(true));
        modeTabBar.getChildren().addAll(tabLoginBtn, tabRegisterBtn);

        // Form Fields
        fullNameField = TiketinTheme.createTextField("Nama lengkap Anda");
        fullNameGroup = TiketinTheme.createFieldGroup("Nama Lengkap", fullNameField);

        usernameField = TiketinTheme.createTextField("Username");
        passwordField = TiketinTheme.createPasswordField("Password");

        submitBtn = TiketinTheme.createPrimaryButton("Masuk", "arrow-right");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit());

        fullNameField.setOnAction(e -> handleSubmit());
        usernameField.setOnAction(e -> handleSubmit());
        passwordField.setOnAction(e -> handleSubmit());

        VBox fieldsBox = new VBox(12,
                fullNameGroup,
                TiketinTheme.createFieldGroup("Username", usernameField),
                TiketinTheme.createFieldGroup("Password", passwordField)
        );

        authCard.getChildren().addAll(headBox, modeTabBar, fieldsBox, submitBtn);
        brandAndCard.getChildren().addAll(brandWordmark, authCard);
        centerWrapper.getChildren().add(brandAndCard);

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                mainFrame.cancelAuthFlow();
            }
        });

        getChildren().addAll(topBar, centerWrapper);
        switchMode(false);
    }

    public void prepare(boolean registerMode, Runnable onAuthSuccess) {
        this.onAuthSuccess = onAuthSuccess;
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        switchMode(registerMode);
        javafx.application.Platform.runLater(() -> {
            if (isRegisterMode) {
                fullNameField.requestFocus();
            } else {
                usernameField.requestFocus();
            }
        });
    }

    private void switchMode(boolean register) {
        this.isRegisterMode = register;
        tabLoginBtn.getStyleClass().setAll(register ? "underline-tab" : "underline-tab-active");
        tabRegisterBtn.getStyleClass().setAll(register ? "underline-tab-active" : "underline-tab");

        fullNameGroup.setVisible(register);
        fullNameGroup.setManaged(register);

        if (register) {
            headingLbl.setText("Buat akun baru");
            helperLbl.setText("Username minimal 3 karakter (tanpa spasi) & password minimal 6 karakter.");
            submitBtn.setText("Daftar");
            submitBtn.setIconName("check");
        } else {
            headingLbl.setText("Masuk ke akun");
            helperLbl.setText("Masukkan username dan password Anda.");
            submitBtn.setText("Masuk");
            submitBtn.setIconName("arrow-right");
        }
    }

    private void handleSubmit() {
        String u = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String p = passwordField.getText() != null ? passwordField.getText() : "";

        if (isRegisterMode) {
            String full = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
            String fullErr = ValidationUtil.validateRequired(full, "Nama lengkap");
            if (fullErr != null) {
                ToastNotification.error(mainFrame, fullErr);
                fullNameField.requestFocus();
                return;
            }
            String userErr = ValidationUtil.validateUsername(u);
            if (userErr != null) {
                ToastNotification.error(mainFrame, userErr);
                usernameField.requestFocus();
                return;
            }
            String passErr = ValidationUtil.validatePassword(p);
            if (passErr != null) {
                ToastNotification.error(mainFrame, passErr);
                passwordField.requestFocus();
                return;
            }
            submitBtn.setDisable(true);
            TiketinTheme.runAsync(() -> userDAO.register(u, p, full), user -> {
                submitBtn.setDisable(false);
                if (user != null) {
                    SessionManager.login(user);
                    ToastNotification.success(mainFrame, "Akun berhasil dibuat!");
                    completeAuth();
                } else {
                    ToastNotification.error(mainFrame, "Pendaftaran gagal.");
                }
            }, err -> {
                submitBtn.setDisable(false);
                ToastNotification.error(mainFrame, "Gagal daftar: " + err.getMessage());
            });
        } else {
            if (u.isEmpty() || p.isEmpty()) {
                ToastNotification.error(mainFrame, "Username dan password wajib diisi.");
                return;
            }
            submitBtn.setDisable(true);
            TiketinTheme.runAsync(() -> userDAO.login(u, p), user -> {
                submitBtn.setDisable(false);
                if (user != null) {
                    SessionManager.login(user);
                    ToastNotification.success(mainFrame, "Selamat datang, " + user.getFullName() + "!");
                    completeAuth();
                } else {
                    ToastNotification.error(mainFrame, "Username atau password salah.");
                }
            }, err -> {
                submitBtn.setDisable(false);
                ToastNotification.error(mainFrame, "Gagal masuk: " + err.getMessage());
            });
        }
    }

    private void completeAuth() {
        Runnable cb = onAuthSuccess;
        onAuthSuccess = null;
        if (cb != null) {
            cb.run();
        } else {
            mainFrame.completeAuthFlow();
        }
    }
}
