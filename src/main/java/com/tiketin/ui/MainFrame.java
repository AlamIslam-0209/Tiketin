package com.tiketin.ui;

import com.tiketin.model.User;
import com.tiketin.ui.components.SidebarPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.dialogs.AuthDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.ui.views.AdminDashboardView;
import com.tiketin.ui.views.ApplyOrganizerView;
import com.tiketin.ui.views.HomeView;
import com.tiketin.ui.views.MyTicketsView;
import com.tiketin.ui.views.OrganizerDashboardView;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application frame managing sidebar navigation and CardLayout views.
 */
public class MainFrame extends JFrame {

    public static final String VIEW_HOME = "HOME";
    public static final String VIEW_MY_TICKETS = "MY_TICKETS";
    public static final String VIEW_APPLY_ORGANIZER = "APPLY_ORGANIZER";
    public static final String VIEW_ORGANIZER = "ORGANIZER_DASHBOARD";
    public static final String VIEW_ADMIN = "ADMIN_DASHBOARD";

    private final SidebarPanel sidebarPanel;
    private final CardLayout cardLayout;
    private final JPanel contentContainer;
    private final Map<String, Runnable> viewRefreshers = new HashMap<>();
    private String currentViewKey = VIEW_HOME;

    public MainFrame() {
        super("Tiketin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        getContentPane().setBackground(TiketinTheme.BG_APP);

        sidebarPanel = new SidebarPanel(
                this::navigateTo,
                () -> AuthDialog.showLoginDialog(this)
        );
        add(sidebarPanel, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(TiketinTheme.BG_APP);
        add(contentContainer, BorderLayout.CENTER);

        SessionManager.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            sidebarPanel.refreshSessionState();
            User user = SessionManager.getCurrentUser();
            if (user == null) {
                if (VIEW_ORGANIZER.equals(currentViewKey)
                        || VIEW_ADMIN.equals(currentViewKey)
                        || VIEW_APPLY_ORGANIZER.equals(currentViewKey)) {
                    navigateTo(VIEW_HOME);
                } else {
                    refreshCurrentView();
                }
            } else {
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    navigateTo(VIEW_ADMIN);
                } else if ("organizer".equalsIgnoreCase(user.getRole())) {
                    navigateTo(VIEW_ORGANIZER);
                } else {
                    refreshCurrentView();
                }
            }
        }));

        initViews();
        SwingUtilities.invokeLater(() -> navigateTo(VIEW_HOME));
    }

    private void initViews() {
        HomeView homeView = new HomeView(this);
        MyTicketsView myTicketsView = new MyTicketsView(this);
        ApplyOrganizerView applyOrganizerView = new ApplyOrganizerView(this);
        OrganizerDashboardView organizerView = new OrganizerDashboardView(this);
        AdminDashboardView adminView = new AdminDashboardView(this);

        registerView(VIEW_HOME, homeView, homeView::refreshData);
        registerView(VIEW_MY_TICKETS, myTicketsView, myTicketsView::refreshData);
        registerView(VIEW_APPLY_ORGANIZER, applyOrganizerView, applyOrganizerView::refreshData);
        registerView(VIEW_ORGANIZER, organizerView, organizerView::refreshData);
        registerView(VIEW_ADMIN, adminView, adminView::refreshData);
    }

    public void registerView(String viewKey, JComponent component, Runnable onShowRefresh) {
        contentContainer.add(component, viewKey);
        if (onShowRefresh != null) {
            viewRefreshers.put(viewKey, onShowRefresh);
        }
        contentContainer.revalidate();
        contentContainer.repaint();
    }

    public void navigateTo(String viewKey) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> navigateTo(viewKey));
            return;
        }

        String targetKey = viewKey;
        if ((VIEW_MY_TICKETS.equals(targetKey) || VIEW_APPLY_ORGANIZER.equals(targetKey)) && !SessionManager.isLoggedIn()) {
            if (!AuthDialog.showLoginDialog(this)) {
                sidebarPanel.setActiveView(currentViewKey);
                return;
            }
        }

        if (VIEW_ORGANIZER.equals(targetKey) && !SessionManager.hasRole("organizer")) {
            ToastNotification.error(this, "Akses khusus organizer.");
            targetKey = VIEW_HOME;
        } else if (VIEW_ADMIN.equals(targetKey) && !SessionManager.hasRole("admin")) {
            ToastNotification.error(this, "Akses khusus admin.");
            targetKey = VIEW_HOME;
        }

        this.currentViewKey = targetKey;
        cardLayout.show(contentContainer, targetKey);
        sidebarPanel.setActiveView(targetKey);

        Runnable refresher = viewRefreshers.get(targetKey);
        if (refresher != null) {
            refresher.run();
        }
    }

    public void refreshCurrentView() {
        if (currentViewKey != null) {
            Runnable refresher = viewRefreshers.get(currentViewKey);
            if (refresher != null) {
                refresher.run();
            }
        }
    }

    public SidebarPanel getSidebarPanel() {
        return sidebarPanel;
    }
}
