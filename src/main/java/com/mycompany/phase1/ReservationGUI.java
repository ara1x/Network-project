
package com.mycompany.phase1;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ReservationGUI - adjusted visuals per request:
 * - Dashboard only: Make a reservation, My bookings, Log out
 * - Sidebar contains only those 3 nav buttons after login
 * - Very dark sage used for selection/confirm (DARK_SAGE = #3A4F41)
 *
 * All networking and business logic preserved unchanged.
 */
public class ReservationGUI extends JFrame {

    // ---- networking (kept unchanged) ----
    private final Client client = new Client();
    private boolean serverConnected = false;

    // ---- session state (kept unchanged) ----
    private String currentUser = null;
    private String selectedType = "STANDARD";
    private int selectedStartDay = 1;  // 1..7 (mapped from day names)
    private int selectedNights = 1;    // 1..7
    private String selectedRoomId = null;
    private List<String> lastAvailableRooms = new ArrayList<>();

    // ---- UI infra ----
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private JComboBox<String> cbType;
    private JComboBox<String> cbStartDay; // now day names
    private JComboBox<Integer> cbNights;
    private JList<String> listRooms;
    private DefaultListModel<String> roomsModel;
    private JTextArea myBookingsArea;

    // header/sidebar panels are created but only added after login
    private JPanel headerPanel;
    private JPanel sidebarPanel;

    // ---- constants / colors / fonts ----
    private static final String[] ROOM_TYPES = {"STANDARD", "PREMIUM", "SUITE"};
    private static final String[] WEEK_DAYS = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};

    private static final Color SAGE = Color.decode("#A8BFAA");
    private static final Color DARK_SAGE = Color.decode("#3A4F41"); // very dark sage per request
    private static final Color BEIGE_BG = Color.decode("#F3EEE6"); // soft beige background for pre-login screens
    private static final Color BEIGE = Color.decode("#E8E3D9"); // previously used
    private static final Color LIGHT_GRAY_BTN = Color.decode("#D9D9D9"); // requested
    private static final Color WHITE = Color.decode("#FFFFFF");
    private static final Color TEXT = Color.decode("#000000"); // black text per request

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // sidebar nav buttons (for highlighting) - now only three
    private JButton navMake, navMyBookings, navLogout;

    public ReservationGUI() {
        setTitle("Hotel Reservation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 680);
        setLocationRelativeTo(null);

        // unify fonts for swing components (fallbacks apply if Segoe UI not present)
        UIManager.put("Label.font", NORMAL_FONT);
        UIManager.put("Button.font", NORMAL_FONT);
        UIManager.put("ComboBox.font", NORMAL_FONT);
        UIManager.put("List.font", NORMAL_FONT);
        UIManager.put("TextArea.font", NORMAL_FONT);
        UIManager.put("TextField.font", NORMAL_FONT);
        UIManager.put("PasswordField.font", NORMAL_FONT);

        // connect (kept exactly as your logic)
        try {
            client.connect("192.168.8.126", 9090);
            client.ping();
            serverConnected = true;
        } catch (IOException e) {
            serverConnected = false;
            JOptionPane.showMessageDialog(this,
                    "Cannot connect to the server. Start the server first.\n" + e.getMessage());
            System.exit(0);
            return;
        }

        // build UI shell (create header/sidebar but don't add them yet)
        buildFrameShell(); // creates headerPanel and sidebarPanel but does NOT add them to the frame

        // root content area: start with soft beige background (pre-login)
        root.setBackground(BEIGE_BG);
        root.setBorder(new EmptyBorder(40, 40, 40, 40));
        getContentPane().setLayout(new BorderLayout());
        // Initially only add root so Home/Login/Signup are plain beige pages
        getContentPane().add(root, BorderLayout.CENTER);

        // Build cards/screens (home/login/signup are plain beige)
        buildHome();
        buildSignUp();
        buildLogIn();
        buildMenu();      // menu (after login) contains only 3 options
        buildType();
        buildDuration();
        buildResults();
        buildNoAvail();
        buildMyBookings();

        showCard("HOME");
    }

    // ----------------- layout shell (create header/sidebar but don't add yet) -----------------
    private void buildFrameShell() {
        // header panel (will be added only after login)
        headerPanel = new JPanel(new BorderLayout(12, 12));
        headerPanel.setBackground(BEIGE);
        headerPanel.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,new Color(210,210,210)), new EmptyBorder(12, 20, 12, 20)));

        JLabel title = new JLabel("Hotel Reservation System");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT);
        title.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel profile = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        profile.setOpaque(false);
        profile.add(new JLabel(new SimpleIcon(24, 24, SimpleIcon.Type.ROOF)));
        JLabel lblUser = new JLabel("Guest");
        lblUser.setFont(HEADER_FONT);
        lblUser.setForeground(TEXT);
        profile.add(lblUser);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(profile, BorderLayout.EAST);

        // sidebar panel (will be added only after login) - now only three nav buttons
        sidebarPanel = new JPanel(new GridBagLayout());
        sidebarPanel.setBackground(SAGE);
        sidebarPanel.setBorder(new MatteBorder(0,0,0,1,new Color(200,200,200)));
        sidebarPanel.setPreferredSize(new Dimension(220, getHeight()));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.NORTHWEST; c.insets = new Insets(20, 12, 8, 12);

        // Only these three nav buttons in sidebar
        navMake = createNavButton("Make reservation", SimpleIcon.Type.BUILDING);
        navMyBookings = createNavButton("My bookings", SimpleIcon.Type.BOOKS);
        navLogout = createNavButton("Log out", SimpleIcon.Type.KEY);

        sidebarPanel.add(navMake, c); c.gridy++;
        sidebarPanel.add(navMyBookings, c); c.gridy++;
        sidebarPanel.add(navLogout, c);
        c.gridy++; c.weighty = 1; sidebarPanel.add(Box.createVerticalGlue(), c);

        // wire nav buttons
        navMake.addActionListener(e -> { highlightNav(navMake); showCard("TYPE"); });
        navMyBookings.addActionListener(e -> { highlightNav(navMyBookings); refreshMyBookingsView(); showCard("MY_BOOKINGS"); });
        navLogout.addActionListener(e -> {
            // same behavior as Log out: remove header/sidebar and go to Home
            currentUser = null;
            getContentPane().remove(headerPanel);
            getContentPane().remove(sidebarPanel);
            getContentPane().remove(root);
            root.setBackground(BEIGE_BG);
            getContentPane().add(root, BorderLayout.CENTER);
            highlightNav(null);
            showCard("HOME");
            revalidate();
            repaint();
        });

        highlightNav(navMake);
    }

    private JButton createNavButton(String text, SimpleIcon.Type iconType) {
        JButton btn = new JButton(text, new SimpleIcon(18, 18, iconType));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setForeground(Color.white);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBackground(SAGE);
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 44));
        return btn;
    }

    private void highlightNav(JButton active) {
        JButton[] all = {navMake, navMyBookings, navLogout};
        for (JButton b : all) {
            if (b == null) continue;
            if (b == active) {
                b.setBackground(WHITE);
                b.setForeground(TEXT);
                b.setBorder(new CompoundBorder(new LineBorder(DARK_SAGE,1,true), new EmptyBorder(8,12,8,12)));
            } else {
                b.setBackground(SAGE);
                b.setForeground(Color.white);
                b.setBorder(new EmptyBorder(8,12,8,12));
            }
        }
    }

    // Call this after successful login/registration to show header + sidebar
    private void enableDashboardUI() {
        // If header already present, do nothing
        if (((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH) == headerPanel) return;

        // remove root and re-add header and sidebar around it
        getContentPane().remove(root);
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(sidebarPanel, BorderLayout.WEST);

        // make root look like card area (white)
        root.setBackground(WHITE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        getContentPane().add(root, BorderLayout.CENTER);

        // revalidate / repaint so layout updates
        revalidate();
        repaint();
    }

    // ----------------- cards (visuals only) -----------------

    // HOME: title + login/signup centered, plain beige background (no header/sidebar)
    private void buildHome() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BEIGE_BG);
        GridBagConstraints c = gbc();
        c.insets = new Insets(12,12,12,12);

        JLabel lbl = new JLabel("Hotel Reservation System");
        lbl.setFont(TITLE_FONT);
        lbl.setForeground(DARK_SAGE); // title in dark sage per your earlier request

        JButton btnLogin = styledLightButton("Log in");
        JButton btnSignUp = styledLightButton("Sign up");

        btnLogin.addActionListener(e -> showCard("LOGIN"));
        btnSignUp.addActionListener(e -> showCard("SIGNUP"));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        actions.setOpaque(false);
        actions.add(btnLogin);
        actions.add(btnSignUp);

        c.gridy=0; p.add(lbl, c);
        c.gridy=1; p.add(Box.createVerticalStrut(8), c);
        c.gridy=2; p.add(actions, c);

        root.add(p, "HOME");
    }

    // SIGN UP (plain beige)
    private void buildSignUp() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BEIGE_BG);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Create an account");
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        JTextField tfUser = new JTextField(18);
        JPasswordField tfPass = new JPasswordField(18);

        c.gridx=0; c.gridy=0; c.anchor = GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor = GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btnRegister = styledLightButton("Register");
        JButton btnBack = styledLightButton("Back");

        c.gridx=1; c.gridy=2; c.anchor = GridBagConstraints.CENTER; p.add(btnRegister, c);
        c.gridy=3; p.add(btnBack, c);

        // original logic preserved; but enable dashboard UI on success
        btnRegister.addActionListener(e -> {
            if (!ensureConnected()) return;
            String u = tfUser.getText().trim();
            String pw = new String(tfPass.getPassword());
            if (u.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill username and password.");
                return;
            }
            try {
                String resp = client.register(u, pw);          // "OK REGISTERED" | "ERR USER_EXISTS"
                if (resp != null && resp.startsWith("OK")) {
                    currentUser = u;
                    JOptionPane.showMessageDialog(this, "Registered successfully.");
                    enableDashboardUI();
                    showCard("MENU");
                } else {
                    JOptionPane.showMessageDialog(this, resp == null ? "No response" : resp);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        btnBack.addActionListener(e -> showCard("HOME"));

        root.add(p, "SIGNUP");
    }

    // LOGIN (plain beige)
    private void buildLogIn() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BEIGE_BG);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Welcome back");
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        JTextField tfUser = new JTextField(18);
        JPasswordField tfPass = new JPasswordField(18);

        c.gridx=0; c.gridy=0; c.anchor = GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor = GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btnLogin = styledLightButton("Log in");
        JButton btnBack = styledLightButton("Back");

        c.gridx=1; c.gridy=2; c.anchor = GridBagConstraints.CENTER; p.add(btnLogin, c);
        c.gridy=3; p.add(btnBack, c);

        btnLogin.addActionListener(e -> {
            if (!ensureConnected()) return;
            String u = tfUser.getText().trim();
            String pw = new String(tfPass.getPassword());
            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username.");
                return;
            }
            try {
                String resp = client.login(u, pw);             // "OK LOGIN" | "ERR NO_SUCH_USER" | "ERR BAD_CREDENTIALS"
                if (resp.startsWith("OK")) {
                    currentUser = u;
                    JOptionPane.showMessageDialog(this, "Logged in successfully.");
                    // only now enable header/sidebar
                    enableDashboardUI();
                    showCard("MENU");
                } else if (resp.contains("NO_SUCH_USER")) {
                    JOptionPane.showMessageDialog(this, "No account found. Please Sign up first.");
                    showCard("SIGNUP");
                } else if (resp.contains("BAD_CREDENTIALS")) {
                    JOptionPane.showMessageDialog(this, "Incorrect password.");
                } else {
                    JOptionPane.showMessageDialog(this, resp);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        btnBack.addActionListener(e -> showCard("HOME"));

        root.add(p, "LOGIN");
    }

    // MENU (shown after login; wrapped) - only 3 options
    private void buildMenu() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("What would you like to do?");
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        JButton btnMake = styledLightButton("Make a reservation");
        JButton btnMy = styledLightButton("My bookings");
        JButton btnLogout = styledLightButton("Log out");

        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title, c);
        c.gridwidth=1;
        c.gridy=1; p.add(btnMake, c);
        c.gridy=2; p.add(btnMy, c);
        c.gridy=3; p.add(btnLogout, c);

        btnMake.addActionListener(e -> {
            if (!ensureConnected()) return;
            showCard("TYPE");
        });
        btnMy.addActionListener(e -> {
            if (!ensureConnected()) return;
            refreshMyBookingsView();
            showCard("MY_BOOKINGS");
        });
        btnLogout.addActionListener(e -> {
            currentUser = null;
            // remove header/sidebar and revert to plain beige home
            getContentPane().remove(headerPanel);
            getContentPane().remove(sidebarPanel);
            getContentPane().remove(root);
            root.setBackground(BEIGE_BG);
            getContentPane().add(root, BorderLayout.CENTER);
            highlightNav(null);
            showCard("HOME");
            revalidate();
            repaint();
        });

        root.add(wrapCard(p), "MENU");
    }

    // TYPE
    private void buildType() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Choose room type");
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        cbType = new JComboBox<>(ROOM_TYPES);

        JButton next = styledLightButton("Next");
        JButton back = styledLightButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title, c);
        c.gridwidth=1;
        c.gridx=0; c.gridy=1; p.add(new JLabel("Type:"), c);
        c.gridx=1; p.add(cbType, c);
        c.gridx=1; c.gridy=2; p.add(next, c);
        c.gridy=3; p.add(back, c);

        next.addActionListener(e -> {
            if (!ensureConnected()) return;
            selectedType = cbType.getSelectedItem().toString();
            showCard("DURATION");
        });

        back.addActionListener(e -> showCard("MENU"));
        root.add(wrapCard(p), "TYPE");
    }

    // DURATION (Start day uses day names starting Sunday)
   // ============================ DURATION ============================
private void buildDuration() {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints c = gbc();

    JLabel title = new JLabel("Choose start day (Sunday–Saturday) and nights (1–7)");
    title.setFont(new Font("Segoe UI", Font.PLAIN, 14));

    // English day names
    String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    JComboBox<String> cbStartDayNames = new JComboBox<>(dayNames);

    cbNights = new JComboBox<>();
    for (int n = 1; n <= 7; n++) cbNights.addItem(n);

    JButton next = new JButton("Find availability");
    JButton back = new JButton("Back");

    c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(title, c);
    c.gridwidth = 1;
    c.gridx = 0; c.gridy = 1; p.add(new JLabel("Start day:"), c);
    c.gridx = 1; p.add(cbStartDayNames, c);
    c.gridx = 0; c.gridy = 2; p.add(new JLabel("Nights:"), c);
    c.gridx = 1; p.add(cbNights, c);

    c.gridx = 1; c.gridy = 3; p.add(next, c);
    c.gridy = 4; p.add(back, c);

    next.addActionListener(e -> {
        if (!ensureConnected()) return;
        selectedStartDay = cbStartDayNames.getSelectedIndex() + 1; // 1..7
        selectedNights = (Integer) cbNights.getSelectedItem();

        // calculate wrapped end day
        int endDay = ((selectedStartDay - 1 + selectedNights - 1) % 7) + 1;
        String startName = dayNames[selectedStartDay - 1];
        String endName = dayNames[endDay - 1];

        JOptionPane.showMessageDialog(this,
            "Booking from " + startName + " to " + endName + " (" + selectedNights + " nights)"
        );

        lastAvailableRooms = findAvailableRooms(selectedType, selectedStartDay, selectedNights);
        if (lastAvailableRooms.isEmpty()) showCard("NO_AVAIL");
        else {
            refreshResultsList();
            showCard("RESULTS");
        }
    });

    back.addActionListener(e -> showCard("TYPE"));
    root.add(p, "DURATION");
}


    // RESULTS
    private void buildResults() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Available rooms");
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        roomsModel = new DefaultListModel<>();
        listRooms = new JList<>(roomsModel);
        listRooms.setVisibleRowCount(8);
        listRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listRooms.setFixedCellHeight(44);
        // selection background now dark sage and selection text white
        listRooms.setSelectionBackground(DARK_SAGE);
        listRooms.setSelectionForeground(WHITE);

        // custom renderer to make selection clearer (white text on dark sage and slight padding)
        listRooms.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(8, 8, 8, 8));
                if (isSelected) {
                    lbl.setBackground(DARK_SAGE);
                    lbl.setForeground(WHITE);
                    lbl.setOpaque(true);
                } else {
                    lbl.setBackground(WHITE);
                    lbl.setForeground(TEXT);
                    lbl.setOpaque(true);
                }
                return lbl;
            }
        });

        JScrollPane sp = new JScrollPane(listRooms);
        sp.setPreferredSize(new Dimension(560, 320));
        sp.setBorder(new CompoundBorder(new EmptyBorder(8,8,8,8), sp.getBorder()));

        // Reserve button now dark sage (confirm color), back remains light
        JButton reserve = styledConfirmButton("Reserve");
        JButton back = styledLightButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; c.fill = GridBagConstraints.HORIZONTAL; p.add(title, c);
        c.gridwidth=2; c.gridy=1; c.weightx=1; c.weighty=1; c.fill = GridBagConstraints.BOTH; p.add(sp, c);
        c.weightx=0; c.weighty=0; c.fill = GridBagConstraints.NONE;

        c.gridwidth=1; c.gridy=2; c.gridx=0; p.add(reserve, c);
        c.gridx=1; p.add(back, c);

        reserve.addActionListener(e -> {
            if (!ensureConnected()) return;
            String sel = listRooms.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Select a room.");
                return;
            }
            selectedRoomId = sel;
            try {
                String resp = client.bookRoom(
                        currentUser, selectedType, selectedRoomId, selectedStartDay, selectedNights);
                if (resp.startsWith("OK CONFIRMED")) {
                    JOptionPane.showMessageDialog(this, "Booking confirmed for " + selectedRoomId);
                    showCard("MENU");
                } else {
                    JOptionPane.showMessageDialog(this, resp);
                    // refresh availability after failure (room may have been taken)
                    lastAvailableRooms = findAvailableRooms(selectedType, selectedStartDay, selectedNights);
                    if (lastAvailableRooms.isEmpty()) showCard("NO_AVAIL");
                    else refreshResultsList();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> showCard("DURATION"));
        root.add(wrapCard(p), "RESULTS");
    }

    // NO_AVAIL
    private void buildNoAvail() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = gbc();

        JLabel msg = new JLabel("No available rooms for your inputs.");
        msg.setFont(HEADER_FONT);
        msg.setForeground(TEXT);
        JButton changeDur = styledLightButton("Change duration");
        JButton changeType = styledLightButton("Change type");

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.CENTER; p.add(msg, c);
        c.gridy=1; p.add(changeDur, c);
        c.gridy=2; p.add(changeType, c);

        changeDur.addActionListener(e -> showCard("DURATION"));
        changeType.addActionListener(e -> showCard("TYPE"));
        root.add(wrapCard(p), "NO_AVAIL");
    }

    // MY_BOOKINGS
    private void buildMyBookings() {
        JPanel p = new JPanel(new BorderLayout(12,12));
        p.setBackground(WHITE);

        JLabel title = new JLabel("My bookings", SwingConstants.CENTER);
        title.setFont(HEADER_FONT);
        title.setForeground(TEXT);

        myBookingsArea = new JTextArea();
        myBookingsArea.setEditable(false);
        myBookingsArea.setLineWrap(true);
        myBookingsArea.setWrapStyleWord(true);
        myBookingsArea.setBorder(new LineBorder(new Color(230,230,230),1,true));
        myBookingsArea.setBackground(new Color(250,250,250));
        myBookingsArea.setPreferredSize(new Dimension(560,320));

        JButton btnBack = styledLightButton("Back");

        btnBack.addActionListener(e -> showCard("MENU"));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(btnBack);

        p.add(title, BorderLayout.NORTH);
        p.add(new JScrollPane(myBookingsArea), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);

        root.add(wrapCard(p), "MY_BOOKINGS");
    }

    // ----------------- helpers & preserved logic -----------------

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10,10,10,10);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private JPanel wrapCard(JPanel inner) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(230,230,230),1,true), new EmptyBorder(18,18,18,18)));
        card.add(inner, BorderLayout.CENTER);
        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    // Light gray rounded button with black text and hover/press effects
    private JButton styledLightButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setBackground(LIGHT_GRAY_BTN);
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(170,170,170), 1, true)); // rounded border
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(160, 42));
        // add hover & press effects
        b.addMouseListener(new MouseAdapter() {
            Color base = LIGHT_GRAY_BTN;
            Color hover = base.darker();
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(hover);
                // subtle lift (layout-managed; harmless attempt)
                b.setLocation(b.getX(), Math.max(0, b.getY()-1));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(base);
                b.setLocation(b.getX(), b.getY()+1);
            }
            @Override public void mousePressed(MouseEvent e) {
                b.setBackground(hover.darker());
            }
            @Override public void mouseReleased(MouseEvent e) {
                b.setBackground(hover);
            }
        });
        return b;
    }

    // Confirm / Reserve button styled in dark sage green with white text
    private JButton styledConfirmButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(DARK_SAGE);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(DARK_SAGE.darker(), 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(160, 42));
        // hover/press – slightly lighter on hover
        b.addMouseListener(new MouseAdapter() {
            Color base = DARK_SAGE;
            Color hover = new Color(Math.max(0, base.getRed()-10), Math.max(0, base.getGreen()-8), Math.max(0, base.getBlue()-6));
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(hover);
                b.setLocation(b.getX(), Math.max(0, b.getY()-1));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(base);
                b.setLocation(b.getX(), b.getY()+1);
            }
            @Override public void mousePressed(MouseEvent e) {
                b.setBackground(hover.darker());
            }
            @Override public void mouseReleased(MouseEvent e) {
                b.setBackground(hover);
            }
        });
        return b;
    }

    private void showCard(String name) { cards.show(root, name); }

    private void refreshResultsList() {
        roomsModel.clear();
        for (String r : lastAvailableRooms) roomsModel.addElement(r);
    }

    private boolean ensureConnected() {
        if (!serverConnected) {
            JOptionPane.showMessageDialog(this, "Server is disconnected. Please start the server.");
            showCard("HOME");
            return false;
        }
        return true;
    }

    // preserved exactly as provided
    private List<String> findAvailableRooms(String type, int startDay, int nights) {
        List<String> out = new ArrayList<>();
        if (!ensureConnected()) return out;
        try {
            String resp = client.listAvail(type, startDay, nights); // "OK ROOMS S1,S3" or "OK ROOMS"
            if (resp.startsWith("OK ROOMS")) {
                String csv = resp.substring("OK ROOMS".length()).trim();
                if (!csv.isEmpty()) {
                    for (String s : csv.split(",")) {
                        String id = s.trim();
                        if (!id.isEmpty()) out.add(id);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, resp);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage());
        }
        return out;
    }

    // preserved exactly as provided
    private void refreshMyBookingsView() {
        if (currentUser == null || currentUser.isEmpty()) {
            myBookingsArea.setText("Please log in first.");
            return;
        }
        if (!ensureConnected()) return;
        try {
            String resp = client.myReservations(currentUser);     // "OK RES S1@3x2,P2@1x1" or "OK RES"
            if (resp.startsWith("OK RES")) {
                String csv = resp.substring("OK RES".length()).trim();
                if (csv.isEmpty()) { myBookingsArea.setText("No bookings yet."); return; }
                StringBuilder sb = new StringBuilder();
                for (String item : csv.split(",")) {
                    item = item.trim();
                    if (!item.isEmpty()) sb.append("• ").append(item).append('\n');
                }
                myBookingsArea.setText(sb.toString());
            } else {
                myBookingsArea.setText(resp);
            }
        } catch (Exception ex) {
            myBookingsArea.setText("Connection error: " + ex.getMessage());
        }
    }

    // helper: map day name to 1..7 (Sunday=1)
    private int dayNameToIndex(String dayName) {
        for (int i = 0; i < WEEK_DAYS.length; i++) {
            if (WEEK_DAYS[i].equalsIgnoreCase(dayName)) return i + 1;
        }
        return 1;
    }

    // ----------------- SimpleIcon - internal vector icons (monochrome flat) -----------------
    // No external assets needed.
    private static class SimpleIcon implements Icon {
        enum Type { HOME, ADD_USER, KEY, BUILDING, BOOKS, ROOF }
        private final int w, h;
        private final Type type;
        private final Color color = new Color(70,70,70); // monochrome

        SimpleIcon(int w, int h, Type t) { this.w = w; this.h = h; this.type = t; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(color);
            switch (type) {
                case HOME:
                    Path2D.Double home = new Path2D.Double();
                    home.moveTo(x + w*0.1, y + h*0.6);
                    home.lineTo(x + w*0.5, y + h*0.15);
                    home.lineTo(x + w*0.9, y + h*0.6);
                    home.lineTo(x + w*0.9, y + h*0.9);
                    home.lineTo(x + w*0.6, y + h*0.9);
                    home.lineTo(x + w*0.6, y + h*0.65);
                    home.lineTo(x + w*0.4, y + h*0.65);
                    home.lineTo(x + w*0.4, y + h*0.9);
                    home.lineTo(x + w*0.1, y + h*0.9);
                    home.closePath();
                    g2.fill(home);
                    break;
                case ADD_USER:
                    g2.fill(new Ellipse2D.Double(x + w*0.15, y + h*0.08, w*0.7, h*0.5));
                    g2.fill(new RoundRectangle2D.Double(x + w*0.05, y + h*0.6, w*0.9, h*0.3, 6, 6));
                    break;
                case KEY:
                    g2.fill(new Ellipse2D.Double(x + w*0.05, y + h*0.25, w*0.5, h*0.5));
                    g2.fill(new Rectangle2D.Double(x + w*0.55, y + h*0.45, w*0.35, h*0.15));
                    break;
                case BUILDING:
                    g2.fill(new Rectangle2D.Double(x + w*0.15, y + h*0.15, w*0.7, h*0.7));
                    g2.setPaint(WHITE);
                    g2.fill(new Rectangle2D.Double(x + w*0.28, y + h*0.28, w*0.12, h*0.12));
                    g2.fill(new Rectangle2D.Double(x + w*0.5, y + h*0.28, w*0.12, h*0.12));
                    g2.fill(new Rectangle2D.Double(x + w*0.28, y + h*0.52, w*0.12, h*0.12));
                    break;
                case BOOKS:
                    g2.fill(new RoundRectangle2D.Double(x + w*0.12, y + h*0.2, w*0.75, h*0.6, 4, 4));
                    g2.setPaint(WHITE);
                    g2.fill(new Rectangle2D.Double(x + w*0.18, y + h*0.25, w*0.45, h*0.05));
                    g2.fill(new Rectangle2D.Double(x + w*0.18, y + h*0.4, w*0.45, h*0.05));
                    break;
                case ROOF:
                    Path2D.Double roof = new Path2D.Double();
                    roof.moveTo(x + w*0.1, y + h*0.6);
                    roof.lineTo(x + w*0.5, y + h*0.2);
                    roof.lineTo(x + w*0.9, y + h*0.6);
                    roof.closePath();
                    g2.fill(roof);
                    break;
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    // ----------------- main -----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReservationGUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
