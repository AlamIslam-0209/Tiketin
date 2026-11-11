package com.tiketin.ui.dialogs;

import com.tiketin.dao.EventDAO;
import com.tiketin.model.Event;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Clean modal dialog for creating or editing an event.
 */
public class EventFormDialog extends JDialog {

    private final Event existingEvent;
    private final int ownerId;
    private boolean saved = false;

    private JTextField nameField;
    private JComboBox<String> categoryCombo;
    private JTextField dateField;
    private JTextField timeField;
    private JTextField locationField;
    private JSpinner priceSpinner;
    private JSpinner slotsSpinner;
    private JTextArea descArea;
    private JButton saveBtn;

    public static boolean showDialog(Component parent, Event existingEvent, int defaultOwnerId) {
        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        EventFormDialog dialog = new EventFormDialog(owner, existingEvent, defaultOwnerId);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.saved;
    }

    public EventFormDialog(Window owner, Event existingEvent, int defaultOwnerId) {
        super(owner, existingEvent == null ? "Buat Event" : "Edit Event", ModalityType.APPLICATION_MODAL);
        this.existingEvent = existingEvent;
        this.ownerId = existingEvent != null ? existingEvent.getOwnerId() : defaultOwnerId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 620);
        setResizable(false);
        initUI();
        TiketinTheme.registerEscapeClose(this);
    }

    private void initUI() {
        getContentPane().setBackground(TiketinTheme.BG_APP);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(TiketinTheme.BG_SURFACE);
        headerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TiketinTheme.BORDER),
                new EmptyBorder(16, 22, 16, 22)
        ));
        JLabel titleLabel = new JLabel(existingEvent == null ? "Buat Event Baru" : "Edit Event");
        titleLabel.setFont(TiketinTheme.fontBold(16));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Form Body
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(TiketinTheme.BG_SURFACE);
        formPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        nameField = TiketinTheme.createTextField("Contoh: Konser Musik Senja");
        if (existingEvent != null && existingEvent.getName() != null) {
            nameField.setText(existingEvent.getName());
        }
        formPanel.add(TiketinTheme.createFieldGroup("Nama Event", nameField));
        formPanel.add(Box.createVerticalStrut(12));

        String[] categories = {"Konser", "Wisata", "Seminar", "Lainnya"};
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(TiketinTheme.fontRegular(13));
        if (existingEvent != null && existingEvent.getCategory() != null) {
            categoryCombo.setSelectedItem(existingEvent.getCategory());
        }
        formPanel.add(TiketinTheme.createFieldGroup("Kategori", categoryCombo));
        formPanel.add(Box.createVerticalStrut(12));

        dateField = TiketinTheme.createTextField("YYYY-MM-DD");
        dateField.setText(existingEvent != null && existingEvent.getDate() != null
                ? existingEvent.getDate()
                : LocalDate.now().plusDays(7).toString());

        timeField = TiketinTheme.createTextField("Contoh: 19:00 WIB");
        timeField.setText(existingEvent != null && existingEvent.getTime() != null
                ? existingEvent.getTime()
                : "19:00 WIB");

        JPanel rowDateTime = new JPanel(new GridLayout(1, 2, 14, 0));
        rowDateTime.setOpaque(false);
        rowDateTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowDateTime.add(TiketinTheme.createFieldGroup("Tanggal (YYYY-MM-DD)", dateField));
        rowDateTime.add(TiketinTheme.createFieldGroup("Waktu", timeField));
        formPanel.add(rowDateTime);
        formPanel.add(Box.createVerticalStrut(12));

        locationField = TiketinTheme.createTextField("Contoh: Balai Sarbini, Jakarta");
        if (existingEvent != null && existingEvent.getLocation() != null) {
            locationField.setText(existingEvent.getLocation());
        }
        formPanel.add(TiketinTheme.createFieldGroup("Lokasi", locationField));
        formPanel.add(Box.createVerticalStrut(12));

        int initialPrice = existingEvent != null ? existingEvent.getPrice() : 50000;
        priceSpinner = new JSpinner(new SpinnerNumberModel(initialPrice, 0, 1_000_000_000, 5000));
        priceSpinner.setEditor(new JSpinner.NumberEditor(priceSpinner, "#,##0"));
        priceSpinner.setFont(TiketinTheme.fontRegular(13));

        int initialSlots = existingEvent != null ? existingEvent.getSlots() : 100;
        slotsSpinner = new JSpinner(new SpinnerNumberModel(initialSlots, 0, 500_000, 10));
        slotsSpinner.setFont(TiketinTheme.fontRegular(13));

        JPanel rowPriceSlots = new JPanel(new GridLayout(1, 2, 14, 0));
        rowPriceSlots.setOpaque(false);
        rowPriceSlots.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPriceSlots.add(TiketinTheme.createFieldGroup("Harga Tiket (Rp)", priceSpinner));
        rowPriceSlots.add(TiketinTheme.createFieldGroup("Kuota Tiket", slotsSpinner));
        formPanel.add(rowPriceSlots);
        formPanel.add(Box.createVerticalStrut(12));

        descArea = new JTextArea(existingEvent != null && existingEvent.getDescription() != null
                ? existingEvent.getDescription()
                : "", 4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(TiketinTheme.fontRegular(13));
        descArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(new LineBorder(TiketinTheme.BORDER_STRONG, 1, true));
        formPanel.add(TiketinTheme.createFieldGroup("Deskripsi", descScroll));

        JScrollPane mainScroll = new JScrollPane(formPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        add(mainScroll, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footerPanel.setBackground(TiketinTheme.BG_SURFACE);
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TiketinTheme.BORDER));

        JButton btnCancel = TiketinTheme.createSecondaryButton("Batal");
        btnCancel.addActionListener(e -> dispose());
        footerPanel.add(btnCancel);

        saveBtn = TiketinTheme.createPrimaryButton("Simpan");
        saveBtn.addActionListener(e -> handleSave());
        footerPanel.add(saveBtn);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private void handleSave() {
        String name = nameField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        String date = dateField.getText().trim();
        String time = timeField.getText().trim();
        String location = locationField.getText().trim();
        int price = ((Number) priceSpinner.getValue()).intValue();
        int slots = ((Number) slotsSpinner.getValue()).intValue();
        String desc = descArea.getText().trim();

        if (name.isEmpty()) {
            ToastNotification.error(this, "Nama event wajib diisi.");
            nameField.requestFocus();
            return;
        }
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            ToastNotification.error(this, "Format tanggal harus YYYY-MM-DD.");
            dateField.requestFocus();
            return;
        }
        if (time.isEmpty()) {
            ToastNotification.error(this, "Waktu acara wajib diisi.");
            timeField.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            ToastNotification.error(this, "Lokasi acara wajib diisi.");
            locationField.requestFocus();
            return;
        }

        saveBtn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                EventDAO eventDAO = new EventDAO();
                if (existingEvent == null) {
                    Event newEvent = new Event();
                    newEvent.setOwnerId(ownerId);
                    newEvent.setName(name);
                    newEvent.setCategory(category);
                    newEvent.setDate(date);
                    newEvent.setTime(time);
                    newEvent.setLocation(location);
                    newEvent.setPrice(price);
                    newEvent.setSlots(slots);
                    newEvent.setDescription(desc);
                    return eventDAO.createEvent(newEvent);
                } else {
                    existingEvent.setName(name);
                    existingEvent.setCategory(category);
                    existingEvent.setDate(date);
                    existingEvent.setTime(time);
                    existingEvent.setLocation(location);
                    existingEvent.setPrice(price);
                    existingEvent.setSlots(slots);
                    existingEvent.setDescription(desc);
                    return eventDAO.updateEvent(existingEvent);
                }
            }

            @Override
            protected void done() {
                saveBtn.setEnabled(true);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        saved = true;
                        Component owner = getOwner() != null ? getOwner() : EventFormDialog.this;
                        ToastNotification.success(owner, existingEvent == null ? "Event berhasil dibuat." : "Event berhasil diperbarui.");
                        dispose();
                    } else {
                        ToastNotification.error(EventFormDialog.this, "Gagal menyimpan data event.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(EventFormDialog.this, "Error: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
