package com.tiketin.ui;

import com.tiketin.model.Event;
import com.tiketin.model.User;
import com.tiketin.ui.components.SidebarPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.ui.views.*;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class MainFrame extends StackPane {
    public static final String VIEW_HOME = "home";
    public static final String VIEW_MY_TICKETS = "my-tickets";
    public static final String VIEW_APPLY_ORGANIZER = "apply-organizer";
    public static final String VIEW_ORGANIZER = "organizer";
    public static final String VIEW_ADMIN = "admin";
    public static final String VIEW_AUTH = "auth";
    public static final String VIEW_SETTINGS = "settings";
    public static final String VIEW_CHECKOUT = "checkout";

    private BorderPane mainLayout;
    private SidebarPanel sidebarPanel;
    private StackPane contentArea;
    private StackPane modalOverlayPane;
    private Pane overlayPane;

    private Map<String, Node> viewCache = new HashMap<>();
    private Map<String, Runnable> searchFocusHandlers = new HashMap<>();
    private String currentViewKey = VIEW_HOME;

    private AuthView authView;
    private OrderCheckoutView checkoutView;
    private String returnViewAfterAuth = VIEW_HOME;
    private Runnable pendingAuthCallback;

    public MainFrame() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #F7F8F6;");

        sidebarPanel = new SidebarPanel(
            this::navigateTo,
            () -> openAuth(currentViewKey, null),
            this::openSettings
        );

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #F7F8F6; -fx-padding: 0;");

        mainLayout.setLeft(sidebarPanel);
        mainLayout.setCenter(contentArea);

        modalOverlayPane = new StackPane();
        modalOverlayPane.setVisible(false);
        modalOverlayPane.setManaged(false);
        modalOverlayPane.setFocusTraversable(true);

        overlayPane = new StackPane();
        overlayPane.setMouseTransparent(true);

        getChildren().addAll(mainLayout, modalOverlayPane, overlayPane);

        SessionManager.addChangeListener(() -> Platform.runLater(() -> {
            viewCache.clear();
            searchFocusHandlers.clear();
            if (SessionManager.getCurrentUser() == null) {
                if (!VIEW_AUTH.equals(currentViewKey)) {
                    navigateTo(VIEW_HOME);
                }
            } else if (VIEW_SETTINGS.equals(currentViewKey)) {
                navigateTo(VIEW_SETTINGS);
            }
        }));

        navigateTo(VIEW_HOME);
    }

    private void setSidebarVisible(boolean visible) {
        if (visible) {
            sidebarPanel.setVisible(true);
            sidebarPanel.setManaged(true);
            if (mainLayout.getLeft() != sidebarPanel) {
                mainLayout.setLeft(sidebarPanel);
            }
        } else {
            sidebarPanel.setVisible(false);
            sidebarPanel.setManaged(false);
            mainLayout.setLeft(null);
        }
    }

    public void navigateTo(String viewKey) {
        if (VIEW_AUTH.equals(viewKey)) {
            openAuth(VIEW_HOME, null);
            return;
        }
        setSidebarVisible(true);

        User currentUser = SessionManager.getCurrentUser();
        if (VIEW_ORGANIZER.equals(viewKey)) {
            if (currentUser == null) {
                openAuth(VIEW_ORGANIZER, () -> navigateTo(VIEW_ORGANIZER));
                return;
            }
            if (!"organizer".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
                ToastNotification.error(this, "Akses ditolak: halaman ini khusus Organizer atau Admin.");
                navigateTo(VIEW_HOME);
                return;
            }
        } else if (VIEW_ADMIN.equals(viewKey)) {
            if (currentUser == null) {
                openAuth(VIEW_ADMIN, () -> navigateTo(VIEW_ADMIN));
                return;
            }
            if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
                ToastNotification.error(this, "Akses ditolak: halaman ini khusus Administrator.");
                navigateTo(VIEW_HOME);
                return;
            }
        }

        if (VIEW_CHECKOUT.equals(viewKey) && checkoutView != null) {
            this.currentViewKey = VIEW_CHECKOUT;
            sidebarPanel.setActiveView(VIEW_HOME);
            contentArea.getChildren().setAll(checkoutView);
            return;
        }
        this.currentViewKey = viewKey;
        sidebarPanel.setActiveView(viewKey);

        Node viewNode = viewCache.computeIfAbsent(viewKey, k -> createViewInstance(k));
        contentArea.getChildren().setAll(viewNode);

        if (viewNode instanceof HomeView) ((HomeView) viewNode).refreshData();
        else if (viewNode instanceof MyTicketsView) ((MyTicketsView) viewNode).refreshData();
        else if (viewNode instanceof ApplyOrganizerView) ((ApplyOrganizerView) viewNode).refreshData();
        else if (viewNode instanceof OrganizerDashboardView) ((OrganizerDashboardView) viewNode).refreshData();
        else if (viewNode instanceof AdminDashboardView) ((AdminDashboardView) viewNode).refreshData();
        else if (viewNode instanceof AccountSettingsView) ((AccountSettingsView) viewNode).refreshData();
    }

    private Node createViewInstance(String key) {
        switch (key) {
            case VIEW_HOME:
                return new HomeView(this);
            case VIEW_MY_TICKETS:
                return new MyTicketsView(this);
            case VIEW_APPLY_ORGANIZER:
                return new ApplyOrganizerView(this);
            case VIEW_ORGANIZER:
                return new OrganizerDashboardView(this);
            case VIEW_ADMIN:
                return new AdminDashboardView(this);
            case VIEW_SETTINGS:
                return new AccountSettingsView(this);
            default:
                return new HomeView(this);
        }
    }

    public void openAuth(String returnViewKey, Runnable afterLogin) {
        if (returnViewKey != null && !VIEW_AUTH.equals(returnViewKey) && !VIEW_CHECKOUT.equals(returnViewKey)) {
            this.returnViewAfterAuth = returnViewKey;
        } else if (this.returnViewAfterAuth == null) {
            this.returnViewAfterAuth = VIEW_HOME;
        }
        this.pendingAuthCallback = afterLogin;
        if (authView == null) {
            authView = new AuthView(this);
        }
        authView.prepare(false, this::completeAuthFlow);
        this.currentViewKey = VIEW_AUTH;
        setSidebarVisible(false);
        contentArea.getChildren().setAll(authView);
    }

    public void cancelAuthFlow() {
        this.pendingAuthCallback = null;
        String target = (returnViewAfterAuth != null && !VIEW_AUTH.equals(returnViewAfterAuth))
                ? returnViewAfterAuth
                : VIEW_HOME;
        navigateTo(target);
    }

    public void completeAuthFlow() {
        viewCache.clear();
        searchFocusHandlers.clear();
        setSidebarVisible(true);

        Runnable cb = this.pendingAuthCallback;
        this.pendingAuthCallback = null;
        if (cb != null) {
            cb.run();
            return;
        }

        User loggedUser = SessionManager.getCurrentUser();
        if (loggedUser != null) {
            if ("admin".equalsIgnoreCase(loggedUser.getRole())) {
                navigateTo(VIEW_ADMIN);
                return;
            }
            if ("organizer".equalsIgnoreCase(loggedUser.getRole())) {
                navigateTo(VIEW_ORGANIZER);
                return;
            }
        }
        String target = (returnViewAfterAuth != null && !VIEW_AUTH.equals(returnViewAfterAuth))
                ? returnViewAfterAuth
                : VIEW_HOME;
        navigateTo(target);
    }

    public void openSettings() {
        if (!SessionManager.isLoggedIn()) {
            openAuth(VIEW_SETTINGS, () -> navigateTo(VIEW_SETTINGS));
            return;
        }
        navigateTo(VIEW_SETTINGS);
    }

    public void openCheckout(Event event, Runnable onOrderSuccess) {
        if (event == null) return;
        if (!SessionManager.isLoggedIn()) {
            ToastNotification.info(this, "Silakan masuk untuk melanjutkan pemesanan tiket.");
            openAuth(VIEW_HOME, () -> openCheckout(event, onOrderSuccess));
            return;
        }
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole())) {
            ToastNotification.error(this, "Akun Admin tidak dapat melakukan pemesanan tiket.");
            setSidebarVisible(true);
            if (!VIEW_HOME.equals(currentViewKey) && !VIEW_ADMIN.equals(currentViewKey)) {
                navigateTo(VIEW_ADMIN);
            } else if (VIEW_AUTH.equals(currentViewKey)) {
                navigateTo(VIEW_ADMIN);
            }
            return;
        }
        if (DateUtil.isEventPassed(event.getDate(), event.getTime())) {
            ToastNotification.error(this, "Acara sudah berakhir dan tiket tidak dapat dipesan.");
            navigateTo(VIEW_HOME);
            return;
        }
        if (event.getSlots() <= 0) {
            ToastNotification.error(this, "Kuota tiket untuk acara ini sudah habis.");
            navigateTo(VIEW_HOME);
            return;
        }
        setSidebarVisible(true);
        if (checkoutView == null) {
            checkoutView = new OrderCheckoutView(this);
        }
        this.currentViewKey = VIEW_CHECKOUT;
        sidebarPanel.setActiveView(VIEW_HOME);
        checkoutView.setEvent(event, onOrderSuccess);
        contentArea.getChildren().setAll(checkoutView);
    }

    public void refreshCurrentView() {
        navigateTo(currentViewKey);
    }

    public SidebarPanel getSidebarPanel() {
        return sidebarPanel;
    }

    public String getCurrentViewKey() {
        return currentViewKey;
    }

    public Pane getOverlayPane() {
        return overlayPane;
    }

    public void closeImageLightbox() {
        modalOverlayPane.getChildren().clear();
        modalOverlayPane.setVisible(false);
        modalOverlayPane.setManaged(false);
    }

    public void showImageLightbox(String storedPath, String title) {
        ToastNotification.info(this, "Pratinjau foto ukuran penuh belum tersedia.");
    }

    public void registerSearchFocusHandler(String viewKey, Runnable focusHandler) {
        searchFocusHandlers.put(viewKey, focusHandler);
    }

    public void triggerSearchFocus() {
        Runnable handler = searchFocusHandlers.get(currentViewKey);
        if (handler != null) {
            handler.run();
        }
    }
}
