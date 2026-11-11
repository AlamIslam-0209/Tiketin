package com.tiketin.ui.views;

import com.tiketin.dao.ApplicationDAO;
import com.tiketin.model.Application;
import com.tiketin.model.User;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.PageHeaderPanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Clean 2-column organizer application form without HTML-width hacks or prefilled fake venue data.
 */
public class ApplyOrganizerView extends JPanel {

    private final MainFrame mainFrame;
    private final ApplicationDAO applicationDAO = new ApplicationDAO();

    private JPanel statusBannerPanel;
    private JLabel statusBannerIcon;
    private JLabel statusBannerText;

    private JTextField fullNameField;
    private JTextField contactField;
    private JTextField orgField;

    private JTextField eventNameField;
    private JComboBox<String> categoryCombo;
    private JTextField eventDateField;
    private JTextField eventTimeField;
    private JTextField eventLocationField;
    private JSpinner priceSpinner;
    private JSpinner slotsSpinner;
    private JTextArea descArea;

    private JButton btnSubmit;

    public ApplyOrganizerView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(TiketinTheme.BG_APP);

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(TiketinTheme.BG_APP);
        mainContainer.setBorder(new EmptyBorder(24, 28, 28, 28));

        PageHeaderPanel header = new PageHeaderPanel(
                "Pengajuan Organizer",
                "Lengkapi data penyelenggara dan draf event pertama untuk ditinjau admin."
        );
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContainer.add(header);
        mainContainer.add(Box.createVerticalStrut(16));

        statusBannerPanel = createStatusBanner();
        statusBannerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBannerPanel.setVisible(false);
        mainContainer.add(statusBannerPanel);
        mainContainer.add(Box.createVerticalStrut(16));

        JPanel twoCol = createTwoColumnForm();
        twoCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContainer.add(twoCol);
        mainContainer.add(Box.createVerticalStrut(18));

        JPanel submitBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        submitBar.setOpaque(false);
        submitBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSubmit = TiketinTheme.createPrimaryButton("Kirim Pengajuan");
        btnSubmit.addActionListener(e -> handleSubmit());
        submitBar.add(btnSubmit);
        mainContainer.add(submitBar);

        JPanel northWrap = TiketinTheme.createScrollableVerticalPanel();
        northWrap.setLayout(new BorderLayout());
        northWrap.setBackground(TiketinTheme.BG_APP);
        northWrap.add(mainContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(northWrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(TiketinTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatusBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.WARNING_FG, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        banner.setBackground(TiketinTheme.WARNING_BG);

        statusBannerIcon = new JLabel(TiketinTheme.getIcon("clock", 18, TiketinTheme.WARNING_FG));
        banner.add(statusBannerIcon, BorderLayout.WEST);

        statusBannerText = new JLabel("");
        statusBannerText.setFont(TiketinTheme.fontBold(12));
        statusBannerText.setForeground(TiketinTheme.TEXT_PRIMARY);
        banner.add(statusBannerText, BorderLayout.CENTER);
        return banner;
    }

    private JPanel createTwoColumnForm() {
        JPanel container = new JPanel(new GridLayout(1, 2, 20, 0));
        container.setOpaque(false);

        // Left: Organizer Info
        JPanel leftCard = TiketinTheme.createCardPanel(18, 20);
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));

        JLabel leftTitle = new JLabel("Data Penyelenggara");
        leftTitle.setFont(TiketinTheme.fontBold(15));
        leftTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        leftTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftCard.add(leftTitle);
        leftCard.add(Box.createVerticalStrut(14));

        fullNameField = TiketinTheme.createTextField("");
        fullNameField.setEditable(false);
        leftCard.add(TiketinTheme.createFieldGroup("Nama Lengkap", fullNameField));
        leftCard.add(Box.createVerticalStrut(12));

        contactField = TiketinTheme.createTextField("Contoh: 081234567890");
        leftCard.add(TiketinTheme.createFieldGroup("No. WhatsApp / HP", contactField));
        leftCard.add(Box.createVerticalStrut(12));

        orgField = TiketinTheme.createTextField("Contoh: Senja Suara Production");
        leftCard.add(TiketinTheme.createFieldGroup("Nama Organisasi", orgField));
        leftCard.add(Box.createVerticalGlue());

        container.add(leftCard);

        // Right: First Event Draft
        JPanel rightCard = TiketinTheme.createCardPanel(18, 20);
        rightCard.setLayout(new BoxLayout(rightCard, BoxLayout.Y_AXIS));

        JLabel rightTitle = new JLabel("Draf Event Pertama");
        rightTitle.setFont(TiketinTheme.fontBold(15));
        rightTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        rightTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCard.add(rightTitle);
        rightCard.add(Box.createVerticalStrut(14));

        eventNameField = TiketinTheme.createTextField("Contoh: Malam Akustik Nusantara");
        rightCard.add(TiketinTheme.createFieldGroup("Nama Event", eventNameField));
        rightCard.add(Box.createVerticalStrut(12));

        categoryCombo = new JComboBox<>(new String[]{"Konser", "Wisata", "Seminar", "Lainnya"});
        categoryCombo.setFont(TiketinTheme.fontRegular(13));
        rightCard.add(TiketinTheme.createFieldGroup("Kategori", categoryCombo));
        rightCard.add(Box.createVerticalStrut(12));

        eventDateField = TiketinTheme.createTextField("YYYY-MM-DD");
        eventDateField.setText(LocalDate.now().plusDays(14).toString());
        eventTimeField = TiketinTheme.createTextField("19:00 WIB");
        eventTimeField.setText("19:00 WIB");

        JPanel dateTimeRow = new JPanel(new GridLayout(1, 2, 12, 0));
        dateTimeRow.setOpaque(false);
        dateTimeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateTimeRow.add(TiketinTheme.createFieldGroup("Tanggal (YYYY-MM-DD)", eventDateField));
        dateTimeRow.add(TiketinTheme.createFieldGroup("Waktu", eventTimeField));
        rightCard.add(dateTimeRow);
        rightCard.add(Box.createVerticalStrut(12));

        eventLocationField = TiketinTheme.createTextField("Contoh: Balai Sarbini, Jakarta");
        rightCard.add(TiketinTheme.createFieldGroup("Lokasi", eventLocationField));
        rightCard.add(Box.createVerticalStrut(12));

        priceSpinner = new JSpinner(new SpinnerNumberModel(50000, 0, 1_000_000_000, 5000));
        priceSpinner.setEditor(new JSpinner.NumberEditor(priceSpinner, "#,##0"));
        priceSpinner.setFont(TiketinTheme.fontRegular(13));

        slotsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 500_000, 10));
        slotsSpinner.setFont(TiketinTheme.fontRegular(13));

        JPanel priceSlotsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        priceSlotsRow.setOpaque(false);
        priceSlotsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceSlotsRow.add(TiketinTheme.createFieldGroup("Harga Tiket (Rp)", priceSpinner));
        priceSlotsRow.add(TiketinTheme.createFieldGroup("Kuota", slotsSpinner));
        rightCard.add(priceSlotsRow);
        rightCard.add(Box.createVerticalStrut(12));

        descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(TiketinTheme.fontRegular(13));
        descArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(new LineBorder(TiketinTheme.BORDER_STRONG, 1, true));
        rightCard.add(TiketinTheme.createFieldGroup("Deskripsi", descScroll));

        container.add(rightCard);
        return container;
    }

    private void setFormEnabled(boolean enabled) {
        contactField.setEnabled(enabled);
        orgField.setEnabled(enabled);
        eventNameField.setEnabled(enabled);
        categoryCombo.setEnabled(enabled);
        eventDateField.setEnabled(enabled);
        eventTimeField.setEnabled(enabled);
        eventLocationField.setEnabled(enabled);
        priceSpinner.setEnabled(enabled);
        slotsSpinner.setEnabled(enabled);
        descArea.setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
    }

    private void handleSubmit() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            ToastNotification.error(this, "Silakan masuk terlebih dahulu.");
            return;
        }

        String contact = contactField.getText().trim();
        String org = orgField.getText().trim();
        String evtName = eventNameField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        String evtDate = eventDateField.getText().trim();
        String evtTime = eventTimeField.getText().trim();
        String evtLoc = eventLocationField.getText().trim();
        int price = ((Number) priceSpinner.getValue()).intValue();
        int slots = ((Number) slotsSpinner.getValue()).intValue();
        String desc = descArea.getText().trim();

        if (contact.isEmpty()) {
            ToastNotification.error(this, "No. WhatsApp / HP wajib diisi.");
            contactField.requestFocus();
            return;
        }
        if (org.isEmpty()) {
            ToastNotification.error(this, "Nama organisasi wajib diisi.");
            orgField.requestFocus();
            return;
        }
        if (evtName.isEmpty()) {
            ToastNotification.error(this, "Nama event wajib diisi.");
            eventNameField.requestFocus();
            return;
        }
        try {
            LocalDate.parse(evtDate);
        } catch (DateTimeParseException e) {
            ToastNotification.error(this, "Format tanggal harus YYYY-MM-DD.");
            eventDateField.requestFocus();
            return;
        }
        if (evtTime.isEmpty()) {
            ToastNotification.error(this, "Waktu acara wajib diisi.");
            eventTimeField.requestFocus();
            return;
        }
        if (evtLoc.isEmpty()) {
            ToastNotification.error(this, "Lokasi acara wajib diisi.");
            eventLocationField.requestFocus();
            return;
        }

        Application app = new Application();
        app.setUserId(user.getId());
        app.setFullName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        app.setContact(contact);
        app.setOrganization(org);
        app.setEventName(evtName);
        app.setEventDate(evtDate);
        app.setEventTime(evtTime);
        app.setEventLocation(evtLoc);
        app.setEventPrice(price);
        app.setEventSlots(slots);
        app.setEventCategory(category);
        app.setEventDescription(desc);

        btnSubmit.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            private String errorMsg;

            @Override
            protected Boolean doInBackground() {
                try {
                    return applicationDAO.submitApplication(app);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                btnSubmit.setEnabled(true);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        ToastNotification.success(ApplyOrganizerView.this, "Pengajuan berhasil dikirim.");
                        refreshData();
                        if (mainFrame != null && mainFrame.getSidebarPanel() != null) {
                            mainFrame.getSidebarPanel().refreshSessionState();
                        }
                    } else {
                        ToastNotification.error(ApplyOrganizerView.this,
                                errorMsg != null ? errorMsg : "Gagal mengirim pengajuan.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(ApplyOrganizerView.this, "Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void refreshData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        fullNameField.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        if (user.getOrganization() != null && !user.getOrganization().isBlank() && orgField.getText().isBlank()) {
            orgField.setText(user.getOrganization());
        }

        new SwingWorker<Application, Void>() {
            @Override
            protected Application doInBackground() {
                return applicationDAO.getLatestApplicationByUser(user.getId());
            }

            @Override
            protected void done() {
                try {
                    Application latest = get();
                    if (latest == null) {
                        statusBannerPanel.setVisible(false);
                        setFormEnabled(true);
                    } else if ("pending".equalsIgnoreCase(latest.getStatus())) {
                        applyBannerStyle(TiketinTheme.WARNING_BG, TiketinTheme.WARNING_FG, "clock",
                                "Pengajuan sedang ditinjau admin. Status akun akan otomatis diperbarui setelah disetujui.");
                        fillFromApplication(latest);
                        setFormEnabled(false);
                    } else if ("rejected".equalsIgnoreCase(latest.getStatus())) {
                        String reason = latest.getReason() != null && !latest.getReason().isBlank()
                                ? latest.getReason()
                                : "Data belum lengkap";
                        applyBannerStyle(TiketinTheme.DANGER_BG, TiketinTheme.DANGER_FG, "alert",
                                "Pengajuan ditolak: " + reason + ". Perbaiki data di bawah untuk mengirim ulang.");
                        fillFromApplication(latest);
                        setFormEnabled(true);
                    } else if ("approved".equalsIgnoreCase(latest.getStatus())) {
                        applyBannerStyle(TiketinTheme.SUCCESS_BG, TiketinTheme.SUCCESS_FG, "check",
                                "Pengajuan disetujui. Akun Anda kini terdaftar sebagai organizer.");
                        setFormEnabled(false);
                    }
                    revalidate();
                    repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void applyBannerStyle(Color bg, Color fg, String iconName, String message) {
        statusBannerPanel.setVisible(true);
        statusBannerPanel.setBackground(bg);
        statusBannerPanel.setBorder(new CompoundBorder(
                new LineBorder(fg, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        statusBannerIcon.setIcon(TiketinTheme.getIcon(iconName, 18, fg));
        String escaped = message == null ? "" : message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        statusBannerText.setText("<html>" + escaped + "</html>");
        statusBannerText.setForeground(TiketinTheme.TEXT_PRIMARY);
        statusBannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
    }

    private void fillFromApplication(Application latest) {
        contactField.setText(latest.getContact());
        orgField.setText(latest.getOrganization());
        eventNameField.setText(latest.getEventName());
        categoryCombo.setSelectedItem(latest.getEventCategory());
        eventDateField.setText(latest.getEventDate());
        eventTimeField.setText(latest.getEventTime());
        eventLocationField.setText(latest.getEventLocation());
        priceSpinner.setValue(latest.getEventPrice());
        slotsSpinner.setValue(Math.max(1, latest.getEventSlots()));
        descArea.setText(latest.getEventDescription());
    }
}
