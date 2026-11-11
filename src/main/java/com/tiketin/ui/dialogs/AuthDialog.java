package com.tiketin.ui.dialogs;

import com.tiketin.dao.UserDAO;
import com.tiketin.model.User;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Clean tabbed login and registration modal with non-blocking auth calls.
 */
public class AuthDialog extends JDialog {

    private final UserDAO userDAO = new UserDAO();
    private JButton loginSubmitBtn;
    private JButton registerSubmitBtn;

    public static boolean showLoginDialog(Component parent) {
        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        AuthDialog dialog = new AuthDialog(owner);
        dialog.setVisible(true);
        return SessionManager.isLoggedIn();
    }

    public AuthDialog(Window owner) {
        super(owner, "Masuk — Tiketin", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 480);
        setResizable(false);
        setLocationRelativeTo(owner);

        initUI();
        TiketinTheme.registerEscapeClose(this);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(TiketinTheme.BG_APP);
        root.setBorder(new EmptyBorder(12, 16, 16, 16));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(TiketinTheme.fontBold(13));
        tabbedPane.addTab("Masuk", createLoginPanel());
        tabbedPane.addTab("Daftar", createRegisterPanel());

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0 && loginSubmitBtn != null) {
                getRootPane().setDefaultButton(loginSubmitBtn);
            } else if (tabbedPane.getSelectedIndex() == 1 && registerSubmitBtn != null) {
                getRootPane().setDefaultButton(registerSubmitBtn);
            }
        });
        getRootPane().setDefaultButton(loginSubmitBtn);

        root.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createLoginPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(TiketinTheme.BG_SURFACE);
        outer.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(20, 20, 16, 20)
        ));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JTextField userField = TiketinTheme.createTextField("Masukkan username");
        JPasswordField passField = new JPasswordField();
        passField.setFont(TiketinTheme.fontRegular(13));
        passField.putClientProperty("JTextField.placeholderText", "Masukkan password");

        form.add(TiketinTheme.createFieldGroup("Username", userField));
        form.add(Box.createVerticalStrut(12));
        form.add(TiketinTheme.createFieldGroup("Password", passField));
        form.add(Box.createVerticalStrut(18));

        loginSubmitBtn = TiketinTheme.createPrimaryButton("Masuk");
        loginSubmitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginSubmitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginSubmitBtn.addActionListener(e -> doLogin(userField.getText().trim(), new String(passField.getPassword()).trim()));
        form.add(loginSubmitBtn);

        outer.add(form, BorderLayout.CENTER);

        // Compact Demo Quick-Login Strip
        JPanel demoBox = new JPanel(new BorderLayout(0, 8));
        demoBox.setBackground(TiketinTheme.BG_SUBTLE);
        demoBox.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel demoTitle = new JLabel("Akun demo:");
        demoTitle.setFont(TiketinTheme.fontRegular(11));
        demoTitle.setForeground(TiketinTheme.TEXT_MUTED);
        demoBox.add(demoTitle, BorderLayout.NORTH);

        JPanel demoBtns = new JPanel(new GridLayout(1, 3, 8, 0));
        demoBtns.setOpaque(false);

        JButton btnAdmin = createDemoButton("Admin");
        btnAdmin.addActionListener(e -> {
            userField.setText("admin");
            passField.setText("admin123");
            doLogin("admin", "admin123");
        });

        JButton btnOrg = createDemoButton("Organizer");
        btnOrg.addActionListener(e -> {
            userField.setText("senjasuara");
            passField.setText("organizer123");
            doLogin("senjasuara", "organizer123");
        });

        JButton btnUser = createDemoButton("User");
        btnUser.addActionListener(e -> doDemoUserLogin(userField, passField));

        demoBtns.add(btnAdmin);
        demoBtns.add(btnOrg);
        demoBtns.add(btnUser);
        demoBox.add(demoBtns, BorderLayout.CENTER);

        outer.add(demoBox, BorderLayout.SOUTH);
        return outer;
    }

    private JButton createDemoButton(String label) {
        JButton btn = TiketinTheme.createSecondaryButton(label);
        btn.setFont(TiketinTheme.fontBold(11));
        btn.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER_STRONG, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return btn;
    }

    private JPanel createRegisterPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(TiketinTheme.BG_SURFACE);
        outer.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JTextField nameField = TiketinTheme.createTextField("Nama lengkap Anda");
        JTextField userField = TiketinTheme.createTextField("Minimal 3 karakter");
        JPasswordField passField = new JPasswordField();
        passField.setFont(TiketinTheme.fontRegular(13));
        passField.putClientProperty("JTextField.placeholderText", "Minimal 6 karakter");

        form.add(TiketinTheme.createFieldGroup("Nama Lengkap", nameField));
        form.add(Box.createVerticalStrut(12));
        form.add(TiketinTheme.createFieldGroup("Username", userField));
        form.add(Box.createVerticalStrut(12));
        form.add(TiketinTheme.createFieldGroup("Password", passField));
        form.add(Box.createVerticalStrut(20));

        registerSubmitBtn = TiketinTheme.createPrimaryButton("Buat Akun");
        registerSubmitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerSubmitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        registerSubmitBtn.addActionListener(e -> handleRegister(
                nameField.getText().trim(),
                userField.getText().trim(),
                new String(passField.getPassword()).trim()
        ));
        form.add(registerSubmitBtn);

        outer.add(form, BorderLayout.NORTH);
        return outer;
    }

    private void doDemoUserLogin(JTextField userField, JPasswordField passField) {
        loginSubmitBtn.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                User u = userDAO.login("pengunjung", "user123");
                if (u == null) {
                    u = userDAO.login("ilyas", "user123");
                }
                return u;
            }

            @Override
            protected void done() {
                loginSubmitBtn.setEnabled(true);
                try {
                    User user = get();
                    if (user != null) {
                        userField.setText(user.getUsername());
                        passField.setText("user123");
                        SessionManager.login(user);
                        ToastNotification.success(getOwner(), "Masuk sebagai " + user.getFullName());
                        dispose();
                    } else {
                        ToastNotification.error(AuthDialog.this, "Akun demo user tidak ditemukan.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(AuthDialog.this, "Gagal login demo: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void doLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            ToastNotification.error(this, "Username dan password wajib diisi.");
            return;
        }

        loginSubmitBtn.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return userDAO.login(username, password);
            }

            @Override
            protected void done() {
                loginSubmitBtn.setEnabled(true);
                try {
                    User user = get();
                    if (user != null) {
                        SessionManager.login(user);
                        ToastNotification.success(getOwner(), "Selamat datang, " + user.getFullName() + ".");
                        dispose();
                    } else {
                        ToastNotification.error(AuthDialog.this, "Username atau password salah.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(AuthDialog.this, "Gagal login: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleRegister(String fullName, String username, String password) {
        if (fullName.isEmpty()) {
            ToastNotification.error(this, "Nama lengkap wajib diisi.");
            return;
        }
        if (username.length() < 3) {
            ToastNotification.error(this, "Username minimal 3 karakter.");
            return;
        }
        if (password.length() < 6) {
            ToastNotification.error(this, "Password minimal 6 karakter.");
            return;
        }

        registerSubmitBtn.setEnabled(false);
        new SwingWorker<User, Void>() {
            private String errorMsg;

            @Override
            protected User doInBackground() {
                try {
                    return userDAO.register(username, password, fullName);
                } catch (SQLException ex) {
                    errorMsg = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                registerSubmitBtn.setEnabled(true);
                try {
                    User newUser = get();
                    if (newUser != null) {
                        SessionManager.login(newUser);
                        ToastNotification.success(getOwner(), "Akun dibuat. Selamat datang, " + newUser.getFullName() + ".");
                        dispose();
                    } else {
                        ToastNotification.error(AuthDialog.this, errorMsg != null ? errorMsg : "Gagal mendaftarkan akun.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(AuthDialog.this, "Error: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
