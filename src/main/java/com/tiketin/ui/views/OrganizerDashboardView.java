package com.tiketin.ui.views;

import com.tiketin.dao.EventDAO;
import com.tiketin.dao.OrderDAO;
import com.tiketin.dao.PaymentDAO;
import com.tiketin.model.Event;
import com.tiketin.model.Order;
import com.tiketin.model.Payment;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.StatCardPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.dialogs.EventFormDialog;
import com.tiketin.ui.dialogs.ProofViewerDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean organizer workspace: single primary action, short tab titles, consistent tables, fixed custom bank visibility.
 */
public class OrganizerDashboardView extends JPanel {

    private final MainFrame mainFrame;
    private final EventDAO eventDAO = new EventDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private StatCardPanel cardEvents;
    private StatCardPanel cardSlots;
    private StatCardPanel cardOrders;
    private StatCardPanel cardPayments;

    private DefaultTableModel eventTableModel;
    private JTable eventTable;
    private final List<Event> eventList = new ArrayList<>();

    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private final List<Order> orderList = new ArrayList<>();

    private DefaultTableModel paymentTableModel;
    private JTable paymentTable;
    private final List<Payment> paymentList = new ArrayList<>();

    private JComboBox<String> bankCombo;
    private JPanel customBankWrapper;
    private JTextField customBankField;
    private JTextField accNumberField;
    private JTextField accNameField;

    public OrganizerDashboardView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(TiketinTheme.BG_APP);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        PageHeaderPanel header = new PageHeaderPanel("Kelola Event", "Atur acara, pesanan masuk, dan rekening pembayaran.");
        JButton btnRefresh = TiketinTheme.createSecondaryButton("Muat Ulang", "refresh");
        btnRefresh.addActionListener(e -> refreshData());

        JButton btnAddEvent = TiketinTheme.createPrimaryButton("Buat Event", "plus");
        btnAddEvent.addActionListener(e -> {
            User user = SessionManager.getCurrentUser();
            if (user != null && EventFormDialog.showDialog(this, null, user.getId())) {
                refreshData();
            }
        });
        header.addAction(btnRefresh);
        header.addAction(btnAddEvent);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        topContainer.add(header);
        topContainer.add(Box.createVerticalStrut(16));

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardEvents = new StatCardPanel("0", "Total Event");
        cardSlots = new StatCardPanel("0", "Sisa Tiket");
        cardOrders = new StatCardPanel("0", "Pesanan Masuk");
        cardPayments = new StatCardPanel("0", "Rekening Aktif");

        statsRow.add(cardEvents);
        statsRow.add(cardSlots);
        statsRow.add(cardOrders);
        statsRow.add(cardPayments);
        topContainer.add(statsRow);
        topContainer.add(Box.createVerticalStrut(16));

        add(topContainer, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(TiketinTheme.fontBold(13));
        tabbedPane.addTab("Event", createEventsTab());
        tabbedPane.addTab("Pesanan", createOrdersTab());
        tabbedPane.addTab("Rekening", createPaymentsTab());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createEventsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolBar.setOpaque(false);

        JButton btnEdit = TiketinTheme.createSecondaryButton("Edit", "edit");
        btnEdit.addActionListener(e -> editSelectedEvent());
        toolBar.add(btnEdit);

        JButton btnDelete = TiketinTheme.createDangerButton("Hapus", "trash");
        btnDelete.addActionListener(e -> deleteSelectedEvent());
        toolBar.add(btnDelete);

        panel.add(toolBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Nama Event", "Kategori", "Tanggal", "Waktu", "Lokasi", "Harga", "Sisa"};
        eventTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        eventTable = TiketinTheme.createStyledTable(eventTableModel);
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(52);
        eventTable.getColumnModel().getColumn(0).setMaxWidth(60);
        eventTable.getColumnModel().getColumn(1).setPreferredWidth(210);
        eventTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        eventTable.getColumnModel().getColumn(4).setPreferredWidth(92);
        eventTable.getColumnModel().getColumn(5).setPreferredWidth(190);
        eventTable.getColumnModel().getColumn(6).setPreferredWidth(112);
        eventTable.getColumnModel().getColumn(7).setPreferredWidth(60);

        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedEvent();
                }
            }
        });

        panel.add(TiketinTheme.wrapInTableScroll(eventTable), BorderLayout.CENTER);
        return panel;
    }

    private void editSelectedEvent() {
        int row = eventTable.getSelectedRow();
        if (row == -1) {
            ToastNotification.info(this, "Pilih event yang ingin diedit.");
            return;
        }
        Event evt = eventList.get(eventTable.convertRowIndexToModel(row));
        User user = SessionManager.getCurrentUser();
        if (EventFormDialog.showDialog(this, evt, user != null ? user.getId() : evt.getOwnerId())) {
            refreshData();
        }
    }

    private void deleteSelectedEvent() {
        int row = eventTable.getSelectedRow();
        if (row == -1) {
            ToastNotification.info(this, "Pilih event yang ingin dihapus.");
            return;
        }
        Event evt = eventList.get(eventTable.convertRowIndexToModel(row));
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Hapus event \"" + evt.getName() + "\"? Pesanan dan tiket terkait juga akan terhapus.",
                "Hapus Event",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (eventDAO.deleteEvent(evt.getId())) {
                ToastNotification.success(this, "Event dihapus.");
                refreshData();
            } else {
                ToastNotification.error(this, "Gagal menghapus event.");
            }
        }
    }

    private JPanel createOrdersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolBar.setOpaque(false);

        JButton btnViewProof = TiketinTheme.createSecondaryButton("Lihat Bukti", "eye");
        btnViewProof.addActionListener(e -> viewSelectedOrderProof());
        toolBar.add(btnViewProof);

        JButton btnApprove = TiketinTheme.createPrimaryButton("Setujui", "check");
        btnApprove.addActionListener(e -> updateSelectedOrderStatus("berhasil"));
        toolBar.add(btnApprove);

        JButton btnReject = TiketinTheme.createDangerButton("Batalkan", "close");
        btnReject.addActionListener(e -> updateSelectedOrderStatus("dibatalkan"));
        toolBar.add(btnReject);

        panel.add(toolBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Kode Order", "Pembeli", "Event", "Jumlah", "Total", "Metode", "Status"};
        orderTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        orderTable = TiketinTheme.createStyledTable(orderTableModel);
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(44);
        orderTable.getColumnModel().getColumn(0).setMaxWidth(56);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(64);
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        orderTable.getColumnModel().getColumn(6).setPreferredWidth(130);
        orderTable.getColumnModel().getColumn(7).setPreferredWidth(105);

        orderTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
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

        panel.add(TiketinTheme.wrapInTableScroll(orderTable), BorderLayout.CENTER);
        return panel;
    }

    private void viewSelectedOrderProof() {
        int row = orderTable.getSelectedRow();
        if (row == -1) {
            ToastNotification.info(this, "Pilih pesanan terlebih dahulu.");
            return;
        }
        Order ord = orderList.get(orderTable.convertRowIndexToModel(row));
        String proof = ord.getPaymentProof();
        if (proof == null || proof.isBlank() || "null".equalsIgnoreCase(proof.trim())) {
            ToastNotification.info(this, "Pesanan ini tidak memiliki bukti transfer.");
            return;
        }
        ProofViewerDialog.showProof(this, proof);
    }

    private void updateSelectedOrderStatus(String targetStatus) {
        int row = orderTable.getSelectedRow();
        if (row == -1) {
            ToastNotification.info(this, "Pilih pesanan terlebih dahulu.");
            return;
        }
        Order ord = orderList.get(orderTable.convertRowIndexToModel(row));
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        if (targetStatus.equalsIgnoreCase(ord.getStatus())) {
            ToastNotification.info(this, "Status pesanan sudah " + targetStatus + ".");
            return;
        }

        if ("dibatalkan".equalsIgnoreCase(targetStatus)) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Batalkan pesanan " + ord.getOrderCode() + "? Kuota " + ord.getQuantity() + " tiket akan dikembalikan.",
                    "Batalkan Pesanan",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            if (orderDAO.updateOrderStatus(ord.getId(), user.getId(), targetStatus)) {
                ToastNotification.success(this, "berhasil".equals(targetStatus)
                        ? "Pesanan " + ord.getOrderCode() + " disetujui."
                        : "Pesanan " + ord.getOrderCode() + " dibatalkan.");
                refreshData();
            }
        } catch (Exception ex) {
            ToastNotification.error(this, "Gagal memproses pesanan: " + ex.getMessage());
        }
    }

    private JPanel createPaymentsTab() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setBackground(TiketinTheme.BG_APP);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        // Left: Add Payment Form (natural height anchored to NORTH)
        JPanel formCard = TiketinTheme.createCardPanel(18, 20);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("Tambah Rekening");
        formTitle.setFont(TiketinTheme.fontBold(15));
        formTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(14));

        bankCombo = new JComboBox<>(new String[]{"Bank BCA", "Bank BRI", "Bank Mandiri", "Bank BSI", "QRIS", "Lainnya"});
        bankCombo.setFont(TiketinTheme.fontRegular(13));
        formCard.add(TiketinTheme.createFieldGroup("Bank / Metode", bankCombo));
        formCard.add(Box.createVerticalStrut(10));

        customBankField = TiketinTheme.createTextField("Contoh: Bank BNI");
        customBankWrapper = TiketinTheme.createFieldGroup("Nama Bank Lainnya", customBankField);
        customBankWrapper.setVisible(false);
        formCard.add(customBankWrapper);
        formCard.add(Box.createVerticalStrut(10));

        bankCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isLainnya = "Lainnya".equals(bankCombo.getSelectedItem());
                customBankWrapper.setVisible(isLainnya);
                formCard.revalidate();
                formCard.repaint();
            }
        });

        accNumberField = TiketinTheme.createTextField("Nomor rekening atau nomor QRIS");
        accNameField = TiketinTheme.createTextField("Nama pemilik rekening");

        formCard.add(TiketinTheme.createFieldGroup("Nomor Rekening", accNumberField));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(TiketinTheme.createFieldGroup("Atas Nama", accNameField));
        formCard.add(Box.createVerticalStrut(16));

        JButton btnSavePayment = TiketinTheme.createPrimaryButton("Simpan Rekening");
        btnSavePayment.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSavePayment.addActionListener(e -> handleAddPayment());
        formCard.add(btnSavePayment);

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        leftWrap.setPreferredSize(new Dimension(330, 0));
        leftWrap.add(formCard, BorderLayout.NORTH);
        panel.add(leftWrap, BorderLayout.WEST);

        // Right: Active Accounts List
        JPanel listCard = TiketinTheme.createCardPanel(18, 20);
        listCard.setLayout(new BorderLayout(0, 12));

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);

        JLabel listTitle = new JLabel("Rekening Aktif");
        listTitle.setFont(TiketinTheme.fontBold(15));
        listTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        listHeader.add(listTitle, BorderLayout.WEST);

        JButton btnDeletePayment = TiketinTheme.createDangerButton("Hapus", "trash");
        btnDeletePayment.addActionListener(e -> handleDeletePayment());
        listHeader.add(btnDeletePayment, BorderLayout.EAST);
        listCard.add(listHeader, BorderLayout.NORTH);

        String[] cols = {"ID", "Bank / Metode", "Nomor Rekening", "Atas Nama"};
        paymentTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = TiketinTheme.createStyledTable(paymentTableModel);
        paymentTable.getColumnModel().getColumn(0).setPreferredWidth(44);
        paymentTable.getColumnModel().getColumn(0).setMaxWidth(56);
        paymentTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        paymentTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        paymentTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        listCard.add(TiketinTheme.wrapInTableScroll(paymentTable), BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        return panel;
    }

    private void handleAddPayment() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            ToastNotification.error(this, "Sesi login berakhir.");
            return;
        }

        String selectedBank = (String) bankCombo.getSelectedItem();
        String bankName = "Lainnya".equals(selectedBank) ? customBankField.getText().trim() : selectedBank;
        String accNumber = accNumberField.getText().trim();
        String accName = accNameField.getText().trim();

        if (bankName == null || bankName.isEmpty()) {
            ToastNotification.error(this, "Nama bank wajib diisi.");
            return;
        }
        if (accNumber.isEmpty()) {
            ToastNotification.error(this, "Nomor rekening wajib diisi.");
            accNumberField.requestFocus();
            return;
        }
        if (accName.isEmpty()) {
            ToastNotification.error(this, "Nama pemilik rekening wajib diisi.");
            accNameField.requestFocus();
            return;
        }

        try {
            if (paymentDAO.addPayment(user.getId(), bankName, accNumber, accName)) {
                ToastNotification.success(this, "Rekening ditambahkan.");
                accNumberField.setText("");
                accNameField.setText("");
                customBankField.setText("");
                refreshData();
            } else {
                ToastNotification.error(this, "Gagal menambahkan rekening.");
            }
        } catch (Exception ex) {
            ToastNotification.error(this, "Error: " + ex.getMessage());
        }
    }

    private void handleDeletePayment() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) {
            ToastNotification.info(this, "Pilih rekening yang ingin dihapus.");
            return;
        }
        Payment p = paymentList.get(paymentTable.convertRowIndexToModel(row));
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Hapus rekening " + p.getBankName() + " (" + p.getAccountNumber() + ")?",
                "Hapus Rekening",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (paymentDAO.deletePayment(p.getId(), user.getId())) {
                    ToastNotification.success(this, "Rekening dihapus.");
                    refreshData();
                } else {
                    ToastNotification.error(this, "Gagal menghapus rekening.");
                }
            } catch (Exception ex) {
                ToastNotification.error(this, "Error: " + ex.getMessage());
            }
        }
    }

    public void refreshData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        int organizerId = user.getId();

        new SwingWorker<Void, Void>() {
            private List<Event> fetchedEvents = new ArrayList<>();
            private List<Order> fetchedOrders = new ArrayList<>();
            private List<Payment> fetchedPayments = new ArrayList<>();
            private int totalSlots = 0;

            @Override
            protected Void doInBackground() {
                fetchedEvents = eventDAO.getEventsByOwner(organizerId);
                for (Event e : fetchedEvents) {
                    totalSlots += e.getSlots();
                }
                fetchedOrders = orderDAO.getOrdersByOrganizer(organizerId);
                fetchedPayments = paymentDAO.getPaymentsByOrganizer(organizerId);
                return null;
            }

            @Override
            protected void done() {
                cardEvents.setValue(String.valueOf(fetchedEvents.size()));
                cardSlots.setValue(String.valueOf(totalSlots));
                cardOrders.setValue(String.valueOf(fetchedOrders.size()));
                cardPayments.setValue(String.valueOf(fetchedPayments.size()));

                eventList.clear();
                eventList.addAll(fetchedEvents);
                eventTableModel.setRowCount(0);
                for (Event evt : fetchedEvents) {
                    eventTableModel.addRow(new Object[]{
                            evt.getId(),
                            evt.getName(),
                            evt.getCategory(),
                            DateUtil.formatTanggalId(evt.getDate()),
                            evt.getTime(),
                            evt.getLocation(),
                            CurrencyUtil.formatRupiah(evt.getPrice()),
                            evt.getSlots()
                    });
                }

                orderList.clear();
                orderList.addAll(fetchedOrders);
                orderTableModel.setRowCount(0);
                for (Order ord : fetchedOrders) {
                    orderTableModel.addRow(new Object[]{
                            ord.getId(),
                            ord.getOrderCode(),
                            ord.getBuyerName() != null ? ord.getBuyerName() : ord.getBuyerUsername(),
                            ord.getEventName(),
                            ord.getQuantity(),
                            CurrencyUtil.formatRupiah(ord.getTotalPrice()),
                            ord.getPaymentMethod(),
                            ord.getStatus()
                    });
                }

                paymentList.clear();
                paymentList.addAll(fetchedPayments);
                paymentTableModel.setRowCount(0);
                for (Payment p : fetchedPayments) {
                    paymentTableModel.addRow(new Object[]{
                            p.getId(),
                            p.getBankName(),
                            p.getAccountNumber(),
                            p.getAccountName()
                    });
                }
            }
        }.execute();
    }
}
