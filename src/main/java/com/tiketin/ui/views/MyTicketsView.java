package com.tiketin.ui.views;

import com.tiketin.dao.OrderDAO;
import com.tiketin.model.Order;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.TicketCardPanel;
import com.tiketin.ui.dialogs.AuthDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean user tickets & order history view with async loading.
 */
public class MyTicketsView extends JPanel {

    private final MainFrame mainFrame;
    private final OrderDAO orderDAO = new OrderDAO();

    private JTabbedPane tabbedPane;
    private JPanel upcomingContainer;
    private JPanel pastContainer;

    public MyTicketsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(TiketinTheme.BG_APP);

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(TiketinTheme.BG_APP);
        root.setBorder(new EmptyBorder(24, 28, 28, 28));

        PageHeaderPanel header = new PageHeaderPanel("Tiket Saya", "Daftar tiket aktif dan riwayat pesanan.");
        JButton reloadBtn = TiketinTheme.createSecondaryButton("Muat Ulang", "refresh");
        reloadBtn.addActionListener(e -> refreshData());
        header.addAction(reloadBtn);
        root.add(header, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(TiketinTheme.fontBold(13));

        upcomingContainer = createStackPanel();
        pastContainer = createStackPanel();

        tabbedPane.addTab("Mendatang", createScrollPane(upcomingContainer));
        tabbedPane.addTab("Riwayat", createScrollPane(pastContainer));

        root.add(tabbedPane, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private JPanel createStackPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(TiketinTheme.BG_APP);
        return p;
    }

    private JScrollPane createScrollPane(JPanel content) {
        JPanel northWrap = TiketinTheme.createScrollableVerticalPanel();
        northWrap.setLayout(new BorderLayout());
        northWrap.setBackground(TiketinTheme.BG_APP);
        northWrap.setBorder(new EmptyBorder(14, 0, 14, 0));
        northWrap.add(content, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(northWrap);
        sp.setBorder(null);
        sp.getViewport().setBackground(TiketinTheme.BG_APP);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    public void refreshData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            renderGuestState();
            return;
        }

        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() {
                return orderDAO.getOrdersByUser(user.getId());
            }

            @Override
            protected void done() {
                try {
                    renderOrders(get());
                } catch (Exception ex) {
                    renderOrders(new ArrayList<>());
                }
            }
        }.execute();
    }

    private void renderGuestState() {
        upcomingContainer.removeAll();
        pastContainer.removeAll();

        upcomingContainer.add(createGuestEmptyState());
        pastContainer.add(createGuestEmptyState());
        tabbedPane.setTitleAt(0, "Mendatang");
        tabbedPane.setTitleAt(1, "Riwayat");

        upcomingContainer.revalidate();
        upcomingContainer.repaint();
        pastContainer.revalidate();
        pastContainer.repaint();
    }

    private void renderOrders(List<Order> orders) {
        upcomingContainer.removeAll();
        pastContainer.removeAll();

        List<Order> upcomingList = new ArrayList<>();
        List<Order> pastList = new ArrayList<>();
        if (orders != null) {
            for (Order o : orders) {
                if (DateUtil.isUpcoming(o.getEventDate())) {
                    upcomingList.add(o);
                } else {
                    pastList.add(o);
                }
            }
        }

        tabbedPane.setTitleAt(0, "Mendatang (" + upcomingList.size() + ")");
        tabbedPane.setTitleAt(1, "Riwayat (" + pastList.size() + ")");

        if (upcomingList.isEmpty()) {
            upcomingContainer.add(createEmptyStatePanel(
                    "Belum ada tiket aktif",
                    "Tiket untuk event mendatang yang kamu pesan akan muncul di sini.",
                    true
            ));
        } else {
            for (Order o : upcomingList) {
                TicketCardPanel card = new TicketCardPanel(o);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                upcomingContainer.add(card);
                upcomingContainer.add(Box.createVerticalStrut(12));
            }
        }

        if (pastList.isEmpty()) {
            pastContainer.add(createEmptyStatePanel(
                    "Belum ada riwayat transaksi",
                    "Pesanan untuk event yang sudah lewat akan tercatat di sini.",
                    false
            ));
        } else {
            for (Order o : pastList) {
                TicketCardPanel card = new TicketCardPanel(o);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                pastContainer.add(card);
                pastContainer.add(Box.createVerticalStrut(12));
            }
        }

        upcomingContainer.revalidate();
        upcomingContainer.repaint();
        pastContainer.revalidate();
        pastContainer.repaint();
    }

    private JPanel createEmptyStatePanel(String title, String subtitle, boolean showExploreButton) {
        JPanel card = TiketinTheme.createCardPanel(32, 24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(TiketinTheme.getIcon("ticket", 34, TiketinTheme.TEXT_MUTED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TiketinTheme.fontBold(15));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(TiketinTheme.fontRegular(12));
        subLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subLabel);

        if (showExploreButton) {
            card.add(Box.createVerticalStrut(14));
            JButton exploreBtn = TiketinTheme.createPrimaryButton("Lihat Event");
            exploreBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            exploreBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.VIEW_HOME));
            card.add(exploreBtn);
        }
        return card;
    }

    private JPanel createGuestEmptyState() {
        JPanel card = TiketinTheme.createCardPanel(32, 24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(TiketinTheme.getIcon("user", 34, TiketinTheme.TEXT_MUTED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Masuk untuk melihat tiket");
        titleLabel.setFont(TiketinTheme.fontBold(15));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Tiket dan bukti pembayaran tersimpan di akun kamu.");
        subLabel.setFont(TiketinTheme.fontRegular(12));
        subLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginBtn = TiketinTheme.createPrimaryButton("Masuk");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> {
            if (AuthDialog.showLoginDialog(this)) {
                refreshData();
            }
        });

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(loginBtn);
        return card;
    }
}
