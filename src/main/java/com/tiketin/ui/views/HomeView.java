package com.tiketin.ui.views;

import com.tiketin.dao.EventDAO;
import com.tiketin.model.Event;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.EventCardPanel;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.dialogs.AuthDialog;
import com.tiketin.ui.dialogs.OrderWizardDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean event catalog view without marketing hero clutter.
 */
public class HomeView extends JPanel {

    private final MainFrame mainFrame;
    private final EventDAO eventDAO = new EventDAO();

    private final List<Event> allEvents = new ArrayList<>();
    private final List<JButton> chipButtons = new ArrayList<>();
    private String selectedCategory = "Semua";
    private int currentCols = 2;

    private PageHeaderPanel pageHeader;
    private JTextField searchField;
    private JPanel gridContainer;
    private JPanel emptyStatePanel;
    private JScrollPane scrollPane;

    public HomeView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(TiketinTheme.BG_APP);

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(TiketinTheme.BG_APP);
        topPanel.setBorder(new EmptyBorder(24, 28, 16, 28));

        pageHeader = new PageHeaderPanel("Daftar Event", "Memuat acara...");
        JButton refreshBtn = TiketinTheme.createSecondaryButton("Muat Ulang", "refresh");
        refreshBtn.addActionListener(e -> refreshData());
        pageHeader.addAction(refreshBtn);
        pageHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(pageHeader);

        topPanel.add(Box.createVerticalStrut(16));

        JPanel filterBar = createFilterBar();
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(filterBar);

        add(topPanel, BorderLayout.NORTH);

        // Scrollable catalog area anchored to NORTH so cards never stretch vertically
        JPanel bodyWrap = TiketinTheme.createScrollableVerticalPanel();
        bodyWrap.setLayout(new BorderLayout());
        bodyWrap.setBackground(TiketinTheme.BG_APP);
        bodyWrap.setBorder(new EmptyBorder(0, 28, 28, 28));

        gridContainer = new JPanel(new GridLayout(0, currentCols, 16, 16));
        gridContainer.setBackground(TiketinTheme.BG_APP);

        emptyStatePanel = createEmptyStatePanel();
        emptyStatePanel.setVisible(false);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(TiketinTheme.BG_APP);
        gridContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyStatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(gridContainer);
        stack.add(emptyStatePanel);

        bodyWrap.add(stack, BorderLayout.NORTH);

        scrollPane = new JScrollPane(bodyWrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(TiketinTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = scrollPane.getViewport().getWidth();
                int targetCols = w >= 1060 ? 3 : (w < 640 ? 1 : 2);
                if (targetCols != currentCols) {
                    currentCols = targetCols;
                    gridContainer.setLayout(new GridLayout(0, currentCols, 16, 16));
                    gridContainer.revalidate();
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 8));
        bar.setOpaque(false);

        searchField = TiketinTheme.createTextField("Cari nama atau lokasi event...");
        searchField.setPreferredSize(new Dimension(280, 36));
        searchField.putClientProperty("JTextField.showClearButton", true);
        Icon searchIcon = TiketinTheme.getIcon("search", 15, TiketinTheme.TEXT_MUTED);
        if (searchIcon != null) {
            searchField.putClientProperty("JTextField.leadingIcon", searchIcon);
        }
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        bar.add(searchField, BorderLayout.WEST);

        JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        chipsPanel.setOpaque(false);
        String[] categories = {"Semua", "Konser", "Wisata", "Seminar", "Lainnya"};
        for (String cat : categories) {
            JButton chip = createCategoryChip(cat);
            chipButtons.add(chip);
            chipsPanel.add(chip);
        }
        bar.add(chipsPanel, BorderLayout.EAST);
        return bar;
    }

    private JButton createCategoryChip(String category) {
        JButton btn = new JButton(category);
        btn.setFont(TiketinTheme.fontBold(12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateChipStyle(btn, category.equalsIgnoreCase(selectedCategory));

        btn.addActionListener(e -> {
            selectedCategory = category;
            for (JButton b : chipButtons) {
                updateChipStyle(b, b.getText().equalsIgnoreCase(selectedCategory));
            }
            applyFilter();
        });
        return btn;
    }

    private void updateChipStyle(JButton btn, boolean isActive) {
        if (isActive) {
            btn.setBackground(TiketinTheme.ACCENT_SOFT);
            btn.setForeground(TiketinTheme.ACCENT_TEXT);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(TiketinTheme.ACCENT, 1, true),
                    new EmptyBorder(6, 13, 6, 13)
            ));
        } else {
            btn.setBackground(TiketinTheme.BG_SURFACE);
            btn.setForeground(TiketinTheme.TEXT_SECONDARY);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(TiketinTheme.BORDER, 1, true),
                    new EmptyBorder(6, 13, 6, 13)
            ));
        }
    }

    private JPanel createEmptyStatePanel() {
        JPanel panel = TiketinTheme.createCardPanel(36, 24);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel(TiketinTheme.getIcon("search", 32, TiketinTheme.TEXT_MUTED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("Event tidak ditemukan");
        msgLabel.setFont(TiketinTheme.fontBold(15));
        msgLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Ubah kata kunci atau pilih kategori lain.");
        subLabel.setFont(TiketinTheme.fontRegular(12));
        subLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton resetBtn = TiketinTheme.createSecondaryButton("Reset Filter");
        resetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            selectedCategory = "Semua";
            for (JButton b : chipButtons) {
                updateChipStyle(b, "Semua".equalsIgnoreCase(b.getText()));
            }
            applyFilter();
        });

        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(msgLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subLabel);
        panel.add(Box.createVerticalStrut(14));
        panel.add(resetBtn);
        return panel;
    }

    public void refreshData() {
        new SwingWorker<List<Event>, Void>() {
            @Override
            protected List<Event> doInBackground() {
                return eventDAO.getAllPublishedEvents();
            }

            @Override
            protected void done() {
                try {
                    allEvents.clear();
                    List<Event> events = get();
                    if (events != null) {
                        allEvents.addAll(events);
                    }
                    applyFilter();
                } catch (Exception ex) {
                    pageHeader.setSubtitle("Gagal memuat daftar event.");
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String keyword = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase()
                : "";

        List<Event> filtered = new ArrayList<>();
        for (Event ev : allEvents) {
            boolean matchCat = "Semua".equalsIgnoreCase(selectedCategory)
                    || (ev.getCategory() != null && ev.getCategory().equalsIgnoreCase(selectedCategory));

            boolean matchKeyword = keyword.isEmpty()
                    || (ev.getName() != null && ev.getName().toLowerCase().contains(keyword))
                    || (ev.getLocation() != null && ev.getLocation().toLowerCase().contains(keyword))
                    || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(keyword))
                    || (ev.getOwnerOrg() != null && ev.getOwnerOrg().toLowerCase().contains(keyword));

            if (matchCat && matchKeyword) {
                filtered.add(ev);
            }
        }

        pageHeader.setSubtitle(filtered.size() + " event tersedia");
        renderGrid(filtered);
    }

    private void renderGrid(List<Event> events) {
        gridContainer.removeAll();
        if (events.isEmpty()) {
            gridContainer.setVisible(false);
            emptyStatePanel.setVisible(true);
        } else {
            emptyStatePanel.setVisible(false);
            gridContainer.setVisible(true);
            for (Event ev : events) {
                gridContainer.add(new EventCardPanel(ev, this::handleBookEvent));
            }
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void handleBookEvent(Event event) {
        if (!SessionManager.isLoggedIn()) {
            if (!AuthDialog.showLoginDialog(this)) {
                return;
            }
        }
        if (SessionManager.hasRole("admin")) {
            ToastNotification.error(this, "Akun admin tidak dapat memesan tiket.");
            return;
        }
        boolean booked = OrderWizardDialog.showWizard(this, event);
        if (booked) {
            refreshData();
            mainFrame.navigateTo(MainFrame.VIEW_MY_TICKETS);
        }
    }
}
