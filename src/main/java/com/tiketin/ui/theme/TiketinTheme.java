package com.tiketin.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralized Swiss-minimalist design system for Tiketin Desktop v2.
 */
public final class TiketinTheme {

    // Surface & Text Tokens
    public static final Color BG_APP = new Color(0xF7, 0xF7, 0xF5);
    public static final Color BG_SURFACE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color BG_SUBTLE = new Color(0xF1, 0xF1, 0xEE);

    public static final Color TEXT_PRIMARY = new Color(0x18, 0x18, 0x1B);
    public static final Color TEXT_SECONDARY = new Color(0x52, 0x52, 0x5B);
    public static final Color TEXT_MUTED = new Color(0x71, 0x71, 0x7A);

    public static final Color BORDER = new Color(0xE4, 0xE4, 0xE0);
    public static final Color BORDER_STRONG = new Color(0xD4, 0xD4, 0xCE);

    // Brand Accent (Terracotta-Coral, WCAG AA >= 4.5:1 on white text)
    public static final Color ACCENT = new Color(0xC2, 0x41, 0x2D);
    public static final Color ACCENT_HOVER = new Color(0x9A, 0x34, 0x12);
    public static final Color ACCENT_SOFT = new Color(0xFF, 0xF3, 0xEF);
    public static final Color ACCENT_TEXT = new Color(0x9A, 0x34, 0x12);

    // Semantic Status Tokens
    public static final Color SUCCESS_FG = new Color(0x15, 0x80, 0x3D);
    public static final Color SUCCESS_BG = new Color(0xDC, 0xFC, 0xE7);
    public static final Color WARNING_FG = new Color(0xA1, 0x62, 0x07);
    public static final Color WARNING_BG = new Color(0xFE, 0xF9, 0xC3);
    public static final Color DANGER_FG = new Color(0xB9, 0x1C, 0x1C);
    public static final Color DANGER_BG = new Color(0xFE, 0xE2, 0xE2);
    public static final Color NEUTRAL_FG = new Color(0x3F, 0x3F, 0x46);
    public static final Color NEUTRAL_BG = new Color(0xF4, 0xF4, 0xF5);

    private static final String SANS_FAMILY = resolveFontFamily(
            new String[]{"Inter", ".AppleSystemUIFont", "SF Pro Text", "Segoe UI"},
            Font.SANS_SERIF
    );
    private static final String MONO_FAMILY = resolveFontFamily(
            new String[]{"JetBrains Mono", "SF Mono", "Menlo", "Consolas"},
            Font.MONOSPACED
    );

    private TiketinTheme() {}

    private static String resolveFontFamily(String[] preferred, String fallback) {
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Set<String> set = new HashSet<>(Arrays.asList(available));
            for (String candidate : preferred) {
                if (set.contains(candidate)) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    public static void setup() {
        FlatLightLaf.setup();

        UIManager.put("defaultFont", fontRegular(13));
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Component.focusColor", new Color(0xC2, 0x41, 0x2D, 70));
        UIManager.put("Component.borderColor", BORDER_STRONG);
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);

        UIManager.put("Panel.background", BG_APP);
        UIManager.put("ScrollPane.background", BG_APP);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.showButtons", false);

        // Clean underlined tabs
        UIManager.put("TabbedPane.tabType", "underlined");
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.foreground", TEXT_MUTED);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.underlineHeight", 2);
        UIManager.put("TabbedPane.contentSeparatorColor", BORDER);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.hoverColor", BG_SUBTLE);

        // Clean horizontal-only table rhythm
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.background", BG_SURFACE);
        UIManager.put("Table.alternateRowColor", BG_SURFACE);
        UIManager.put("Table.selectionBackground", ACCENT_SOFT);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", BG_SUBTLE);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TableHeader.separatorColor", BORDER);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        UIManager.put("TableHeader.height", 36);
    }

    public static Font fontRegular(int size) {
        return new Font(SANS_FAMILY, Font.PLAIN, size);
    }

    public static Font fontBold(int size) {
        return new Font(SANS_FAMILY, Font.BOLD, size);
    }

    public static Font fontMono(int size, int style) {
        return new Font(MONO_FAMILY, style, size);
    }

    public static Icon getIcon(String name, int size, Color color) {
        try {
            FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
            if (!icon.hasFound()) {
                return null;
            }
            if (color != null) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
            }
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Panel that tracks JScrollPane viewport width so children wrap instead of overflowing horizontally.
     */
    public static JPanel createScrollableVerticalPanel() {
        return new ScrollableVerticalPanel();
    }

    private static final class ScrollableVerticalPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public static JButton createPrimaryButton(String text) {
        return createPrimaryButton(text, null);
    }

    public static JButton createPrimaryButton(String text, String iconName) {
        JButton btn = new JButton(text);
        btn.setFont(fontBold(13));
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        if (iconName != null) {
            btn.setIcon(getIcon(iconName, 15, Color.WHITE));
            btn.setIconTextGap(8);
        }
        installHover(btn, ACCENT, ACCENT_HOVER);
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        return createSecondaryButton(text, null);
    }

    public static JButton createSecondaryButton(String text, String iconName) {
        JButton btn = new JButton(text);
        btn.setFont(fontBold(13));
        btn.setBackground(BG_SURFACE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(BORDER_STRONG, 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        if (iconName != null) {
            btn.setIcon(getIcon(iconName, 15, TEXT_SECONDARY));
            btn.setIconTextGap(8);
        }
        installHover(btn, BG_SURFACE, BG_SUBTLE);
        return btn;
    }

    public static JButton createDangerButton(String text) {
        return createDangerButton(text, null);
    }

    public static JButton createDangerButton(String text, String iconName) {
        JButton btn = new JButton(text);
        btn.setFont(fontBold(13));
        btn.setBackground(BG_SURFACE);
        btn.setForeground(DANGER_FG);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFC, 0xA5, 0xA5), 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        if (iconName != null) {
            btn.setIcon(getIcon(iconName, 15, DANGER_FG));
            btn.setIconTextGap(8);
        }
        installHover(btn, BG_SURFACE, DANGER_BG);
        return btn;
    }

    private static void installHover(JButton btn, Color normalBg, Color hoverBg) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(normalBg);
                }
            }
        });
    }

    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(fontRegular(13));
        field.setForeground(TEXT_PRIMARY);
        if (placeholder != null && !placeholder.isBlank()) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
        return field;
    }

    public static JPanel createFieldGroup(String labelText, JComponent input) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(fontBold(12));
        label.setForeground(TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(5));

        if (!(input instanceof JScrollPane)) {
            input.setPreferredSize(new Dimension(220, 36));
            input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(input);
        return wrapper;
    }

    public static JPanel createCardPanel() {
        return createCardPanel(18, 20);
    }

    public static JPanel createCardPanel(int padV, int padH) {
        JPanel panel = new JPanel();
        panel.setBackground(BG_SURFACE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(padV, padH, padV, padH)
        ));
        return panel;
    }

    public static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setFont(fontBold(11));
        badge.setBorder(new EmptyBorder(3, 9, 3, 9));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        return badge;
    }

    public static JLabel createStatusBadge(String rawStatus) {
        String s = rawStatus == null ? "" : rawStatus.trim().toLowerCase();
        switch (s) {
            case "berhasil":
            case "approved":
                return createBadge("approved".equals(s) ? "Disetujui" : "Berhasil", SUCCESS_BG, SUCCESS_FG);
            case "dibatalkan":
            case "rejected":
                return createBadge("rejected".equals(s) ? "Ditolak" : "Dibatalkan", DANGER_BG, DANGER_FG);
            case "menunggu":
            case "pending":
                return createBadge("Menunggu", WARNING_BG, WARNING_FG);
            case "admin":
                return createBadge("Admin", DANGER_BG, DANGER_FG);
            case "organizer":
                return createBadge("Organizer", ACCENT_SOFT, ACCENT_TEXT);
            case "user":
                return createBadge("User", NEUTRAL_BG, NEUTRAL_FG);
            default:
                String label = rawStatus == null || rawStatus.isBlank() ? "-" : rawStatus;
                return createBadge(label, NEUTRAL_BG, NEUTRAL_FG);
        }
    }

    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFont(fontRegular(13));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_SURFACE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setGridColor(BORDER);
        table.getTableHeader().setFont(fontBold(12));
        table.getTableHeader().setBackground(BG_SUBTLE);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer cellPadding = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean selected, boolean focus, int r, int c) {
                super.getTableCellRendererComponent(tbl, val, selected, focus, r, c);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellPadding);
        return table;
    }

    public static JScrollPane wrapInTableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(BG_SURFACE);
        return scroll;
    }

    public static JTextArea createWrappingTextLabel(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text != null ? text : "");
        area.setFont(font);
        area.setForeground(color);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setBorder(null);
        return area;
    }

    public static void registerEscapeClose(JDialog dialog) {
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}
