package com.tiketin.ui.components;

import com.tiketin.dao.ApplicationDAO;
import com.tiketin.model.Application;
import com.tiketin.model.User;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Clean 236px navigation sidebar with full-width labels and non-blocking status lookup.
 */
public class SidebarPanel extends JPanel {

    private final Consumer<String> onNavigate;
    private final Runnable onLoginClick;
    private final ApplicationDAO applicationDAO = new ApplicationDAO();

    private final JPanel navContainer;
    private final JPanel accountContainer;
    private final Map<String, JButton> menuButtons = new HashMap<>();
    private final Map<String, String> menuIcons = new HashMap<>();
    private String activeViewKey = "HOME";

    public SidebarPanel(Consumer<String> onNavigate, Runnable onLoginClick) {
        this.onNavigate = onNavigate;
        this.onLoginClick = onLoginClick;

        setPreferredSize(new Dimension(236, 0));
        setMinimumSize(new Dimension(236, 0));
        setBackground(TiketinTheme.BG_SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, TiketinTheme.BORDER));
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setBackground(TiketinTheme.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(22, 20, 16, 20));

        JLabel logoLabel = new JLabel("<html><span style='color:#18181b'>Tiketin</span><span style='color:#c2412d'>.</span></html>");
        logoLabel.setFont(TiketinTheme.fontBold(20));
        headerPanel.add(logoLabel);
        add(headerPanel, BorderLayout.NORTH);

        navContainer = new JPanel();
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setBackground(TiketinTheme.BG_SURFACE);
        navContainer.setBorder(new EmptyBorder(6, 12, 8, 12));

        JScrollPane scrollNav = new JScrollPane(navContainer);
        scrollNav.setBorder(null);
        scrollNav.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollNav.getViewport().setBackground(TiketinTheme.BG_SURFACE);
        add(scrollNav, BorderLayout.CENTER);

        accountContainer = new JPanel(new BorderLayout());
        accountContainer.setBackground(TiketinTheme.BG_SURFACE);
        accountContainer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TiketinTheme.BORDER),
                new EmptyBorder(14, 14, 18, 14)
        ));
        add(accountContainer, BorderLayout.SOUTH);

        refreshSessionState();
    }

    public void setActiveView(String viewKey) {
        this.activeViewKey = viewKey;
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            String key = entry.getKey();
            JButton btn = entry.getValue();
            String iconName = menuIcons.get(key);

            boolean isActive = key.equals(viewKey);
            if (isActive) {
                btn.setBackground(TiketinTheme.ACCENT_SOFT);
                btn.setForeground(TiketinTheme.ACCENT_TEXT);
                if (iconName != null) {
                    btn.setIcon(TiketinTheme.getIcon(iconName, 16, TiketinTheme.ACCENT_TEXT));
                }
            } else {
                btn.setBackground(TiketinTheme.BG_SURFACE);
                btn.setForeground(TiketinTheme.TEXT_SECONDARY);
                if (iconName != null) {
                    btn.setIcon(TiketinTheme.getIcon(iconName, 16, TiketinTheme.TEXT_MUTED));
                }
            }
        }
        repaint();
    }

    public void refreshSessionState() {
        navContainer.removeAll();
        menuButtons.clear();
        menuIcons.clear();

        User user = SessionManager.getCurrentUser();
        boolean isLoggedIn = user != null;
        String role = isLoggedIn && user.getRole() != null ? user.getRole().toLowerCase() : "guest";

        addMenuItem("HOME", "Event", "home");

        if (!"admin".equals(role)) {
            addMenuItem("MY_TICKETS", "Tiket Saya", "ticket");
        }

        if (isLoggedIn && "user".equals(role)) {
            JButton applyBtn = addMenuItem("APPLY_ORGANIZER", "Jadi Organizer", "apply");
            loadApplicationStatusHint(user.getId(), applyBtn);
        }

        if (isLoggedIn && "organizer".equals(role)) {
            addMenuItem("ORGANIZER_DASHBOARD", "Kelola Event", "organizer");
        }

        if (isLoggedIn && "admin".equals(role)) {
            addMenuItem("ADMIN_DASHBOARD", "Admin", "admin");
        }

        navContainer.add(Box.createVerticalGlue());
        navContainer.revalidate();
        navContainer.repaint();

        // Bottom account slot
        accountContainer.removeAll();
        if (!isLoggedIn) {
            JButton loginBtn = TiketinTheme.createPrimaryButton("Masuk");
            loginBtn.addActionListener(e -> {
                if (onLoginClick != null) {
                    onLoginClick.run();
                }
            });
            accountContainer.add(loginBtn, BorderLayout.CENTER);
        } else {
            JPanel userBox = new JPanel();
            userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
            userBox.setOpaque(false);

            String displayName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getUsername();
            JLabel nameLabel = new JLabel(displayName);
            nameLabel.setFont(TiketinTheme.fontBold(13));
            nameLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
            nameLabel.setToolTipText(displayName);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            userBox.add(nameLabel);

            userBox.add(Box.createVerticalStrut(4));

            JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            metaRow.setOpaque(false);
            metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            metaRow.add(TiketinTheme.createStatusBadge(role));

            if (user.getOrganization() != null && !user.getOrganization().isBlank()) {
                JLabel orgLabel = new JLabel(user.getOrganization());
                orgLabel.setFont(TiketinTheme.fontRegular(11));
                orgLabel.setForeground(TiketinTheme.TEXT_MUTED);
                orgLabel.setToolTipText(user.getOrganization());
                metaRow.add(orgLabel);
            }
            userBox.add(metaRow);

            userBox.add(Box.createVerticalStrut(10));

            JButton logoutBtn = TiketinTheme.createSecondaryButton("Keluar", "logout");
            logoutBtn.setFont(TiketinTheme.fontBold(12));
            logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            logoutBtn.addActionListener(e -> {
                SessionManager.logout();
                ToastNotification.info(this, "Anda telah keluar.");
            });
            userBox.add(logoutBtn);

            accountContainer.add(userBox, BorderLayout.CENTER);
        }

        accountContainer.revalidate();
        accountContainer.repaint();
        setActiveView(activeViewKey);
    }

    private void loadApplicationStatusHint(int userId, JButton applyBtn) {
        new SwingWorker<Application, Void>() {
            @Override
            protected Application doInBackground() {
                return applicationDAO.getLatestApplicationByUser(userId);
            }

            @Override
            protected void done() {
                try {
                    Application latest = get();
                    if (latest == null) return;
                    if ("pending".equalsIgnoreCase(latest.getStatus())) {
                        applyBtn.setText("Jadi Organizer · Proses");
                    } else if ("rejected".equalsIgnoreCase(latest.getStatus())) {
                        applyBtn.setText("Jadi Organizer · Ditolak");
                    }
                    applyBtn.revalidate();
                    applyBtn.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JButton addMenuItem(String key, String title, String iconName) {
        JButton btn = new JButton(title);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);
        btn.setFont(TiketinTheme.fontBold(13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 12, 9, 12));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        menuButtons.put(key, btn);
        menuIcons.put(key, iconName);

        btn.addActionListener(e -> {
            setActiveView(key);
            if (onNavigate != null) {
                onNavigate.accept(key);
            }
        });

        navContainer.add(btn);
        navContainer.add(Box.createVerticalStrut(3));
        return btn;
    }
}
