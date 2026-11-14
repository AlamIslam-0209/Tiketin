package com.tiketin.ui.components;

import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ProofStorageUtil;
import com.tiketin.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends VBox {
    private Consumer<String> onNavigate;
    private Runnable onLoginClick;
    private Runnable onAccountSettingsClick;
    private Map<String, Button> navButtons = new HashMap<>();
    private Map<String, String> navIcons = new HashMap<>();
    private VBox navContainer;
    private VBox footerContainer;
    private String activeViewKey = MainFrame.VIEW_HOME;

    public SidebarPanel(Consumer<String> onNavigate, Runnable onLoginClick, Runnable onAccountSettingsClick) {
        this.onNavigate = onNavigate;
        this.onLoginClick = onLoginClick;
        this.onAccountSettingsClick = onAccountSettingsClick;

        getStyleClass().add("sidebar");
        setMinWidth(248);
        setPrefWidth(248);
        setMaxWidth(248);
        setSpacing(20);

        HBox brandBox = new HBox(0);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(0, 12, 4, 12));
        Label brandTitle = new Label("Tiketin");
        brandTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #20252A;");
        Label brandDot = new Label(".");
        brandDot.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E06338;");
        brandBox.getChildren().addAll(brandTitle, brandDot);

        navContainer = new VBox(4);
        VBox.setVgrow(navContainer, Priority.ALWAYS);

        footerContainer = new VBox(4);
        footerContainer.setPadding(new Insets(16, 0, 0, 0));
        footerContainer.setStyle("-fx-border-color: #E3E6E2; -fx-border-width: 1 0 0 0;");

        getChildren().addAll(brandBox, navContainer, footerContainer);

        rebuildNav();
        SessionManager.addChangeListener(this::rebuildNav);
    }

    public void rebuildNav() {
        navContainer.getChildren().clear();
        navButtons.clear();
        navIcons.clear();

        addNavItem("Acara", MainFrame.VIEW_HOME, "compass");
        addNavItem("Tiket Saya", MainFrame.VIEW_MY_TICKETS, "ticket");

        User user = SessionManager.getCurrentUser();
        Label groupLabel = new Label("KELOLA");
        groupLabel.getStyleClass().add("sidebar-group-label");
        navContainer.getChildren().add(groupLabel);

        if (user == null || "user".equalsIgnoreCase(user.getRole())) {
            addNavItem("Daftar Organizer", MainFrame.VIEW_APPLY_ORGANIZER, "sparkles");
        }
        if (user != null && ("organizer".equalsIgnoreCase(user.getRole()) || "admin".equalsIgnoreCase(user.getRole()))) {
            addNavItem("Kelola Acara", MainFrame.VIEW_ORGANIZER, "grid");
        }
        if (user != null && "admin".equalsIgnoreCase(user.getRole())) {
            addNavItem("Admin", MainFrame.VIEW_ADMIN, "shield");
        }

        footerContainer.getChildren().clear();

        if (user == null) {
            VBox guestCol = new VBox(8);
            guestCol.setPadding(new Insets(4, 12, 0, 12));
            Label guestLbl = new Label("Belum masuk akun");
            guestLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #687078;");
            
            AnimatedButton loginBtn = new AnimatedButton("Masuk / Daftar", "arrow-right");
            loginBtn.setMaxWidth(Double.MAX_VALUE);
            loginBtn.setOnAction(e -> {
                if (onLoginClick != null) onLoginClick.run();
            });
            guestCol.getChildren().addAll(guestLbl, loginBtn);
            footerContainer.getChildren().add(guestCol);
        } else {
            HBox profileRow = new HBox(12);
            profileRow.setAlignment(Pos.CENTER_LEFT);
            profileRow.setPadding(new Insets(6, 12, 6, 12));

            StackPane avatar = new StackPane();
            avatar.setPrefSize(36, 36);
            avatar.setMinSize(36, 36);
            avatar.setMaxSize(36, 36);
            avatar.setStyle("-fx-background-color: #E06338; -fx-background-radius: 999px;");
            boolean renderedPhoto = false;
            if (user.getAvatarPath() != null && !user.getAvatarPath().isBlank()) {
                File avatarFile = ProofStorageUtil.findAvatarFile(user.getAvatarPath());
                if (avatarFile != null && avatarFile.exists()) {
                    try {
                        Image img = new Image(avatarFile.toURI().toString(), 72, 72, true, true);
                        if (!img.isError()) {
                            ImageView iv = new ImageView(img);
                            double iw = img.getWidth();
                            double ih = img.getHeight();
                            if (iw > 0 && ih > 0) {
                                // Center-crop square before circular clip
                                double side = Math.min(iw, ih);
                                iv.setViewport(new javafx.geometry.Rectangle2D((iw - side) / 2.0, (ih - side) / 2.0, side, side));
                            }
                            iv.setFitWidth(36);
                            iv.setFitHeight(36);
                            iv.setPreserveRatio(false);
                            iv.setSmooth(true);
                            iv.setClip(new Circle(18, 18, 18));
                            avatar.setStyle("-fx-background-color: transparent; -fx-background-radius: 999px;");
                            avatar.getChildren().add(iv);
                            renderedPhoto = true;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (!renderedPhoto) {
                String initials = user.getFullName() != null && !user.getFullName().isEmpty()
                        ? user.getFullName().substring(0, 1).toUpperCase()
                        : "U";
                Label initLbl = new Label(initials);
                initLbl.setFont(TiketinTheme.fontBold(14));
                initLbl.setTextFill(Color.WHITE);
                avatar.getChildren().add(initLbl);
            }

            VBox infoCol = new VBox(2);
            HBox.setHgrow(infoCol, Priority.ALWAYS);
            Label nameLbl = new Label(user.getFullName() != null ? user.getFullName() : user.getUsername());
            nameLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #20252A;");
            nameLbl.setMaxWidth(148);
            
            String roleText = "User";
            if ("admin".equalsIgnoreCase(user.getRole())) roleText = "Admin";
            else if ("organizer".equalsIgnoreCase(user.getRole())) roleText = "Organizer";
            Label roleLbl = new Label(roleText);
            roleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #687078;");
            infoCol.getChildren().addAll(nameLbl, roleLbl);
            profileRow.getChildren().addAll(avatar, infoCol);

            footerContainer.getChildren().add(profileRow);

            Button settingsBtn = new Button("Pengaturan");
            settingsBtn.setGraphic(TiketinTheme.getIcon("settings", 16, Color.web("#687078")));
            settingsBtn.setMaxWidth(Double.MAX_VALUE);
            settingsBtn.setAlignment(Pos.CENTER_LEFT);
            settingsBtn.getStyleClass().setAll("sidebar-item");
            settingsBtn.setOnAction(e -> {
                if (onAccountSettingsClick != null) onAccountSettingsClick.run();
            });
            navButtons.put(MainFrame.VIEW_SETTINGS, settingsBtn);
            navIcons.put(MainFrame.VIEW_SETTINGS, "settings");

            Button logoutBtn = new Button("Keluar");
            logoutBtn.setGraphic(TiketinTheme.getIcon("logout", 16, Color.web("#687078")));
            logoutBtn.setMaxWidth(Double.MAX_VALUE);
            logoutBtn.setAlignment(Pos.CENTER_LEFT);
            logoutBtn.getStyleClass().setAll("sidebar-item");
            logoutBtn.setOnAction(e -> {
                SessionManager.logout();
                ToastNotification.info(this, "Berhasil keluar.");
                if (onNavigate != null) onNavigate.accept(MainFrame.VIEW_HOME);
            });

            footerContainer.getChildren().addAll(settingsBtn, logoutBtn);
        }

        setActiveView(activeViewKey);
    }

    private void addNavItem(String title, String viewKey, String iconName) {
        Button btn = new Button(title);
        boolean isActive = viewKey.equals(activeViewKey);
        Color iconColor = isActive ? Color.web("#B94E3A") : Color.web("#687078");
        btn.setGraphic(TiketinTheme.getIcon(iconName, 16, iconColor));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        if (isActive) {
            btn.getStyleClass().setAll("sidebar-item-active");
        } else {
            btn.getStyleClass().setAll("sidebar-item");
        }
        btn.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept(viewKey);
            }
        });
        navButtons.put(viewKey, btn);
        navIcons.put(viewKey, iconName);
        navContainer.getChildren().add(btn);
    }

    public void setActiveView(String viewKey) {
        this.activeViewKey = viewKey != null ? viewKey : MainFrame.VIEW_HOME;
        navButtons.forEach((k, btn) -> {
            boolean isActive = k.equals(this.activeViewKey);
            if (isActive) {
                btn.getStyleClass().setAll("sidebar-item-active");
            } else {
                btn.getStyleClass().setAll("sidebar-item");
            }
            String iconName = navIcons.get(k);
            if (iconName != null) {
                Color iconColor = isActive ? Color.web("#B94E3A") : Color.web("#687078");
                btn.setGraphic(TiketinTheme.getIcon(iconName, 16, iconColor));
            }
        });
    }

    public void refreshSessionState() {
        rebuildNav();
    }
}
