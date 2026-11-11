package com.tiketin.ui.views;

import com.tiketin.dao.ApplicationDAO;
import com.tiketin.dao.EventDAO;
import com.tiketin.dao.UserDAO;
import com.tiketin.model.Application;
import com.tiketin.model.Event;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.StatCardPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.dialogs.EventFormDialog;
import com.tiketin.ui.dialogs.RejectReasonDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Clean admin governance panel with async SwingWorker refresh, unified tables, and native-wrapping cards.
 */
public class AdminDashboardView extends JPanel {

    private final MainFrame mainFrame;

    private StatCardPanel statUsers;
    private StatCardPanel statPending;
    private StatCardPanel statEvents;

    private JPanel applicationsContainer;
    private JLabel tab1CountLabel;

    private DefaultTableModel userTableModel;
    private TableRowSorter<DefaultTableModel> userSorter;
    private JTextField userSearchField;

    private JTable eventTable;
    private DefaultTableModel eventTableModel;
    private TableRowSorter<DefaultTableModel> eventSorter;
    private JTextField eventSearchField;

    public AdminDashboardView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(TiketinTheme.BG_APP);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        PageHeaderPanel header = new PageHeaderPanel("Panel Admin", "Tinjau pengajuan organizer, akun pengguna, dan event.");
        JButton refreshBtn = TiketinTheme.createSecondaryButton("Muat Ulang", "refresh");
        refreshBtn.addActionListener(e -> refreshData());
        header.addAction(refreshBtn);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        topContainer.add(header);
        topContainer.add(Box.createVerticalStrut(16));

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        statsPanel.setOpaque(false);
        statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statUsers = new StatCardPanel("0", "Pengguna");
        statPending = new StatCardPanel("0", "Menunggu Tinjauan");
        statEvents = new StatCardPanel("0", "Event Aktif");

        statsPanel.add(statUsers);
        statsPanel.add(statPending);
        statsPanel.add(statEvents);
        topContainer.add(statsPanel);
        topContainer.add(Box.createVerticalStrut(16));

        add(topContainer, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(TiketinTheme.fontBold(13));
        tabbedPane.addTab("Pengajuan", createApplicationsTab());
        tabbedPane.addTab("Pengguna", createUsersTab());
        tabbedPane.addTab("Event", createEventsTab());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JComponent createApplicationsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel subHeader = new JPanel(new BorderLayout());
        subHeader.setOpaque(false);

        JLabel title = new JLabel("Pengajuan Masuk");
        title.setFont(TiketinTheme.fontBold(14));
        title.setForeground(TiketinTheme.TEXT_PRIMARY);
        subHeader.add(title, BorderLayout.WEST);

        tab1CountLabel = new JLabel("0 menunggu");
        tab1CountLabel.setFont(TiketinTheme.fontBold(12));
        tab1CountLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subHeader.add(tab1CountLabel, BorderLayout.EAST);
        panel.add(subHeader, BorderLayout.NORTH);

        applicationsContainer = new JPanel();
        applicationsContainer.setLayout(new BoxLayout(applicationsContainer, BoxLayout.Y_AXIS));
        applicationsContainer.setBackground(TiketinTheme.BG_APP);

        JPanel scrollContent = TiketinTheme.createScrollableVerticalPanel();
        scrollContent.setLayout(new BorderLayout());
        scrollContent.setBackground(TiketinTheme.BG_APP);
        scrollContent.add(applicationsContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(TiketinTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchBar.setOpaque(false);

        userSearchField = TiketinTheme.createTextField("Cari username, nama, atau role...");
        userSearchField.setPreferredSize(new Dimension(280, 36));
        userSearchField.putClientProperty("JTextField.showClearButton", true);
        userSearchField.putClientProperty("JTextField.leadingIcon", TiketinTheme.getIcon("search", 15, TiketinTheme.TEXT_MUTED));
        userSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterUserTable(); }
            public void removeUpdate(DocumentEvent e) { filterUserTable(); }
            public void changedUpdate(DocumentEvent e) { filterUserTable(); }
        });
        searchBar.add(userSearchField);
        panel.add(searchBar, BorderLayout.NORTH);

        String[] columns = {"ID", "Username", "Nama Lengkap", "Role", "Organisasi", "Terdaftar"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable userTable = TiketinTheme.createStyledTable(userTableModel);
        userSorter = new TableRowSorter<>(userTableModel);
        userTable.setRowSorter(userSorter);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(44);
        userTable.getColumnModel().getColumn(0).setMaxWidth(56);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(3).setMaxWidth(120);

        userTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel badge = TiketinTheme.createStatusBadge(value != null ? value.toString() : "-");
                JPanel wrap = new JPanel(new GridBagLayout());
                wrap.setOpaque(true);
                wrap.setBackground(isSelected ? table.getSelectionBackground() : TiketinTheme.BG_SURFACE);
                wrap.add(badge);
                return wrap;
            }
        });

        panel.add(TiketinTheme.wrapInTableScroll(userTable), BorderLayout.CENTER);
        return panel;
    }

    private void filterUserTable() {
        String text = userSearchField.getText().trim();
        userSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2, 3, 4));
    }

    private JComponent createEventsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        eventSearchField = TiketinTheme.createTextField("Cari event, kategori, atau penyelenggara...");
        eventSearchField.setPreferredSize(new Dimension(280, 36));
        eventSearchField.putClientProperty("JTextField.showClearButton", true);
        eventSearchField.putClientProperty("JTextField.leadingIcon", TiketinTheme.getIcon("search", 15, TiketinTheme.TEXT_MUTED));
        eventSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterEventTable(); }
            public void removeUpdate(DocumentEvent e) { filterEventTable(); }
            public void changedUpdate(DocumentEvent e) { filterEventTable(); }
        });
        toolbar.add(eventSearchField, BorderLayout.WEST);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton editBtn = TiketinTheme.createSecondaryButton("Edit", "edit");
        editBtn.addActionListener(e -> onEditSelectedEvent());
        JButton deleteBtn = TiketinTheme.createDangerButton("Hapus", "trash");
        deleteBtn.addActionListener(e -> onDeleteSelectedEvent());

        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);
        toolbar.add(actionButtons, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Nama Event", "Penyelenggara", "Kategori", "Jadwal", "Lokasi", "Harga", "Sisa"};
        eventTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        eventTable = TiketinTheme.createStyledTable(eventTableModel);
        eventSorter = new TableRowSorter<>(eventTableModel);
        eventTable.setRowSorter(eventSorter);
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(44);
        eventTable.getColumnModel().getColumn(0).setMaxWidth(56);
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        eventTable.getColumnModel().getColumn(3).setMaxWidth(115);

        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditSelectedEvent();
                }
            }
        });

        panel.add(TiketinTheme.wrapInTableScroll(eventTable), BorderLayout.CENTER);
        return panel;
    }

    private void filterEventTable() {
        String text = eventSearchField.getText().trim();
        eventSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2, 3, 5));
    }

    private void onEditSelectedEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow == -1) {
            ToastNotification.info(this, "Pilih event terlebih dahulu.");
            return;
        }
        int eventId = (Integer) eventTableModel.getValueAt(eventTable.convertRowIndexToModel(selectedRow), 0);
        Event event = new EventDAO().getEventById(eventId);
        if (event == null) {
            ToastNotification.error(this, "Data event tidak ditemukan.");
            return;
        }
        if (EventFormDialog.showDialog(this, event, event.getOwnerId())) {
            refreshData();
        }
    }

    private void onDeleteSelectedEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow == -1) {
            ToastNotification.info(this, "Pilih event terlebih dahulu.");
            return;
        }
        int modelRow = eventTable.convertRowIndexToModel(selectedRow);
        int eventId = (Integer) eventTableModel.getValueAt(modelRow, 0);
        String eventName = (String) eventTableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Hapus event \"" + eventName + "\"?",
                "Hapus Event",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (new EventDAO().deleteEvent(eventId)) {
                ToastNotification.success(this, "Event dihapus.");
                refreshData();
            } else {
                ToastNotification.error(this, "Gagal menghapus event.");
            }
        }
    }

    public void refreshData() {
        new SwingWorker<Void, Void>() {
            private List<User> users;
            private List<Application> apps;
            private List<Event> events;

            @Override
            protected Void doInBackground() {
                users = new UserDAO().getAllUsers();
                apps = new ApplicationDAO().getAllApplications();
                events = new EventDAO().getAllPublishedEvents();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    long pendingCount = apps.stream().filter(a -> "pending".equalsIgnoreCase(a.getStatus())).count();
                    statUsers.setValue(String.valueOf(users.size()));
                    statPending.setValue(String.valueOf(pendingCount));
                    statEvents.setValue(String.valueOf(events.size()));
                    tab1CountLabel.setText(pendingCount + " menunggu");

                    refreshApplicationsList(apps);

                    userTableModel.setRowCount(0);
                    for (User u : users) {
                        String dateFormatted = u.getCreatedAt();
                        if (dateFormatted != null && dateFormatted.length() >= 10) {
                            dateFormatted = DateUtil.formatTanggalId(dateFormatted.substring(0, 10));
                        } else if (dateFormatted == null) {
                            dateFormatted = "-";
                        }
                        userTableModel.addRow(new Object[]{
                                u.getId(),
                                u.getUsername(),
                                u.getFullName() != null ? u.getFullName() : "-",
                                u.getRole() != null ? u.getRole() : "-",
                                u.getOrganization() != null && !u.getOrganization().isBlank() ? u.getOrganization() : "-",
                                dateFormatted
                        });
                    }

                    eventTableModel.setRowCount(0);
                    for (Event ev : events) {
                        String organizer = ev.getOwnerOrg() != null && !ev.getOwnerOrg().isBlank()
                                ? ev.getOwnerOrg()
                                : (ev.getOwnerName() != null ? ev.getOwnerName() : "-");
                        String schedule = DateUtil.formatTanggalId(ev.getDate())
                                + (ev.getTime() != null && !ev.getTime().isBlank() ? " · " + ev.getTime() : "");

                        eventTableModel.addRow(new Object[]{
                                ev.getId(),
                                ev.getName(),
                                organizer,
                                ev.getCategory() != null ? ev.getCategory() : "-",
                                schedule,
                                ev.getLocation() != null ? ev.getLocation() : "-",
                                CurrencyUtil.formatRupiah(ev.getPrice()),
                                ev.getSlots()
                        });
                    }
                } catch (Exception ex) {
                    ToastNotification.error(AdminDashboardView.this, "Gagal memuat data admin.");
                }
            }
        }.execute();
    }

    private void refreshApplicationsList(List<Application> apps) {
        applicationsContainer.removeAll();

        if (apps == null || apps.isEmpty()) {
            JPanel emptyCard = TiketinTheme.createCardPanel(28, 20);
            emptyCard.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
            emptyCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel emptyLabel = new JLabel("Belum ada pengajuan organizer.");
            emptyLabel.setFont(TiketinTheme.fontRegular(13));
            emptyLabel.setForeground(TiketinTheme.TEXT_MUTED);
            emptyCard.add(emptyLabel);
            applicationsContainer.add(emptyCard);
        } else {
            for (Application app : apps) {
                JPanel card = TiketinTheme.createCardPanel(16, 18);
                card.setLayout(new BorderLayout(0, 10));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Header
                JPanel topRow = new JPanel(new BorderLayout(8, 0));
                topRow.setOpaque(false);

                String orgTitle = app.getOrganization() != null && !app.getOrganization().isBlank()
                        ? app.getOrganization()
                        : "-";
                JLabel orgLabel = new JLabel(orgTitle);
                orgLabel.setFont(TiketinTheme.fontBold(15));
                orgLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
                topRow.add(orgLabel, BorderLayout.WEST);

                String status = app.getStatus() != null ? app.getStatus().toLowerCase() : "pending";
                topRow.add(TiketinTheme.createStatusBadge(status), BorderLayout.EAST);
                card.add(topRow, BorderLayout.NORTH);

                // Details (native wrapping without hardcoded HTML pixel widths)
                JPanel details = new JPanel();
                details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                details.setOpaque(false);

                JLabel applicantLabel = new JLabel(app.getFullName() + " · " + (app.getContact() != null ? app.getContact() : "-"));
                applicantLabel.setFont(TiketinTheme.fontRegular(12));
                applicantLabel.setForeground(TiketinTheme.TEXT_MUTED);
                applicantLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                details.add(applicantLabel);
                details.add(Box.createVerticalStrut(6));

                String eventSummary = app.getEventName()
                        + " (" + app.getEventCategory() + ") · "
                        + DateUtil.formatTanggalId(app.getEventDate())
                        + (app.getEventTime() != null && !app.getEventTime().isBlank() ? " " + app.getEventTime() : "")
                        + (app.getEventLocation() != null && !app.getEventLocation().isBlank() ? " · " + app.getEventLocation() : "")
                        + " · " + CurrencyUtil.formatRupiah(app.getEventPrice())
                        + " · " + app.getEventSlots() + " kuota";

                JTextArea eventLine = TiketinTheme.createWrappingTextLabel(eventSummary, TiketinTheme.fontBold(13), TiketinTheme.TEXT_PRIMARY);
                eventLine.setAlignmentX(Component.LEFT_ALIGNMENT);
                details.add(eventLine);

                if (app.getEventDescription() != null && !app.getEventDescription().isBlank()) {
                    details.add(Box.createVerticalStrut(4));
                    JTextArea descLine = TiketinTheme.createWrappingTextLabel(app.getEventDescription(), TiketinTheme.fontRegular(12), TiketinTheme.TEXT_SECONDARY);
                    descLine.setAlignmentX(Component.LEFT_ALIGNMENT);
                    details.add(descLine);
                }

                if ("rejected".equals(status) && app.getReason() != null && !app.getReason().isBlank()) {
                    details.add(Box.createVerticalStrut(6));
                    JTextArea reasonLine = TiketinTheme.createWrappingTextLabel("Alasan ditolak: " + app.getReason(), TiketinTheme.fontBold(12), TiketinTheme.DANGER_FG);
                    reasonLine.setAlignmentX(Component.LEFT_ALIGNMENT);
                    details.add(reasonLine);
                }

                card.add(details, BorderLayout.CENTER);

                if ("pending".equals(status)) {
                    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                    actions.setOpaque(false);

                    JButton rejectBtn = TiketinTheme.createDangerButton("Tolak", "close");
                    rejectBtn.addActionListener(e -> {
                        String reason = RejectReasonDialog.showDialog(AdminDashboardView.this, app.getOrganization());
                        if (reason != null && !reason.trim().isEmpty()) {
                            try {
                                if (new ApplicationDAO().rejectApplication(app.getId(), reason.trim())) {
                                    ToastNotification.info(AdminDashboardView.this, "Pengajuan ditolak.");
                                    refreshData();
                                }
                            } catch (SQLException ex) {
                                ToastNotification.error(AdminDashboardView.this, "Error: " + ex.getMessage());
                            }
                        }
                    });

                    JButton approveBtn = TiketinTheme.createPrimaryButton("Setujui", "check");
                    approveBtn.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(
                                AdminDashboardView.this,
                                "Setujui pengajuan \"" + app.getOrganization() + "\" dan terbitkan event \"" + app.getEventName() + "\"?",
                                "Setujui Pengajuan",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE
                        );
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                if (new ApplicationDAO().approveApplication(app.getId())) {
                                    ToastNotification.success(AdminDashboardView.this, "Pengajuan disetujui.");
                                    refreshData();
                                }
                            } catch (SQLException ex) {
                                ToastNotification.error(AdminDashboardView.this, "Gagal: " + ex.getMessage());
                            }
                        }
                    });

                    actions.add(rejectBtn);
                    actions.add(approveBtn);
                    card.add(actions, BorderLayout.SOUTH);
                }

                applicationsContainer.add(card);
                applicationsContainer.add(Box.createVerticalStrut(10));
            }
        }

        applicationsContainer.revalidate();
        applicationsContainer.repaint();
    }
}
