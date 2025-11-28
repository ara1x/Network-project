package com.mycompany.phase1;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

public class ReservationGUI extends JFrame {


// ---- networking ----
private final Client client = new Client();
private boolean serverConnected = false;

// ---- session state ----
private String currentUser = null;
private String selectedType = "STANDARD";
private int selectedStartDay = 1;
private int selectedNights = 1;
private String selectedRoomId = null;
private List<String> lastAvailableRooms = new ArrayList<>();

// ---- UI infra ----
private final CardLayout cards = new CardLayout();
private final JPanel root = new JPanel(cards);

private JComboBox<String> cbType;
private JComboBox<String> cbStartDay;
private JComboBox<Integer> cbNights;
private JList<String> listRooms;
private DefaultListModel<String> roomsModel;

// ====== NEW for My Bookings selection & cancel ======
private DefaultListModel<String> bookingsModel;
private JList<String> bookingsList;
private final List<String> bookingResIds = new ArrayList<>();
// ====================================================

private JPanel headerPanel;
private JPanel sidebarPanel;

// ---- constants / colors / fonts ----
private static final String[] ROOM_TYPES = {"STANDARD", "PREMIUM", "SUITE"};
private static final String[] WEEK_DAYS = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
};

// Desert-inspired color palette
private static final Color SAND = Color.decode("#C8A882");
private static final Color DARK_SAND = Color.decode("#A08968");
private static final Color WARM_WHITE = Color.decode("#FAF8F3");
private static final Color DARK_BROWN = Color.decode("#6B4423"); // Rich brown for room selection
private static final Color DARK_ACCENT = Color.decode("#3A4F41");
private static final Color TEXT_DARK = Color.decode("#2C2416");
private static final Color GOLD_ACCENT = Color.decode("#D4AF37");

private static final Font TITLE_FONT = new Font("Serif", Font.BOLD, 36);
private static final Font SUBTITLE_FONT = new Font("Serif", Font.PLAIN, 16);
private static final Font HEADER_FONT = new Font("Sans-Serif", Font.BOLD, 18);
private static final Font NORMAL_FONT = new Font("Sans-Serif", Font.PLAIN, 14);

private JButton navMake, navMyBookings, navLogout;

// Image storage
private BufferedImage homepage;
private BufferedImage logoImage;

public ReservationGUI() {
    setTitle("Sahara Resort - AlUla");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(1200, 750);
    setLocationRelativeTo(null);

    // Load images
    loadImages();

    UIManager.put("Label.font", NORMAL_FONT);
    UIManager.put("Button.font", NORMAL_FONT);
    UIManager.put("ComboBox.font", NORMAL_FONT);
    UIManager.put("List.font", NORMAL_FONT);
    UIManager.put("TextArea.font", NORMAL_FONT);
    UIManager.put("TextField.font", NORMAL_FONT);
    UIManager.put("PasswordField.font", NORMAL_FONT);

    // Connect
    try {
        client.connect("localhost", 9090);
        client.ping();
        serverConnected = true;
    } catch (IOException e) {
        serverConnected = false;
        JOptionPane.showMessageDialog(this,
                "Cannot connect to the server. Start the server first.\n" + e.getMessage());
        System.exit(0);
        return;
    }

    buildFrameShell();

    root.setBackground(WARM_WHITE);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(root, BorderLayout.CENTER);

    buildHome();
    buildSignUp();
    buildLogIn();
    buildMenu();
    buildType();
    buildDuration();
    buildResults();
    buildNoAvail();
    buildMyBookings();

    showCard("HOME");
}

private void loadImages() {
    try {
        ClassLoader classLoader = getClass().getClassLoader();
        homepage = ImageIO.read(classLoader.getResourceAsStream("images/homepage.jpg"));
        
        // Load logo and resize it to fit properly - LARGER SIZE for better visibility
        BufferedImage originalLogo = ImageIO.read(classLoader.getResourceAsStream("images/Logo.png"));
        logoImage = resizeImage(originalLogo, 450, 150); // Increased from 300x100
        
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Error loading images: " + e.getMessage());
        homepage = createPlaceholderImage(1200, 750, SAND);
        logoImage = createLogoImage(); // Fallback to generated logo
    }
}

private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
    // Calculate scaling to fit within target dimensions while maintaining aspect ratio
    double widthRatio = (double) targetWidth / original.getWidth();
    double heightRatio = (double) targetHeight / original.getHeight();
    double ratio = Math.min(widthRatio, heightRatio);
    
    int newWidth = (int) (original.getWidth() * ratio);
    int newHeight = (int) (original.getHeight() * ratio);
    
    // Create high-quality scaled image
    BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = resized.createGraphics();
    
    // High-quality rendering settings
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    
    // Center the image in the target dimensions
    int x = (targetWidth - newWidth) / 2;
    int y = (targetHeight - newHeight) / 2;
    
    g.drawImage(original, x, y, newWidth, newHeight, null);
    g.dispose();
    
    return resized;
}

private BufferedImage decodeBase64Image(String base64) throws IOException {
    byte[] imageBytes = Base64.getDecoder().decode(base64);
    return ImageIO.read(new ByteArrayInputStream(imageBytes));
}

private BufferedImage createPlaceholderImage(int w, int h, Color c) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(c);
    g.fillRect(0, 0, w, h);
    g.dispose();
    return img;
}

private BufferedImage createLogoImage() {
    // Fallback logo in case Logo.png fails to load
    BufferedImage img = new BufferedImage(300, 80, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g.setColor(TEXT_DARK);
    g.setFont(new Font("Serif", Font.BOLD, 28));
    g.drawString("SAHARA", 20, 35);
    g.setFont(new Font("Serif", Font.PLAIN, 14));
    g.drawString("RESORT", 20, 52);

    g.dispose();
    return img;
}

private void buildFrameShell() {
    headerPanel = new JPanel(new BorderLayout(12, 12));
    headerPanel.setBackground(WARM_WHITE);
    headerPanel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 2, 0, SAND),
            new EmptyBorder(15, 25, 15, 25)));

    JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

    JPanel profile = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
    profile.setOpaque(false);
    JLabel lblUser = new JLabel("Guest");
    lblUser.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
    lblUser.setForeground(TEXT_DARK);
    profile.add(new JLabel(new SimpleIcon(22, 22, SimpleIcon.Type.ROOF)));
    profile.add(lblUser);

    headerPanel.add(logoLabel, BorderLayout.WEST);
    headerPanel.add(profile, BorderLayout.EAST);

    // Sidebar
    sidebarPanel = new JPanel(new GridBagLayout());
    sidebarPanel.setBackground(SAND);
    sidebarPanel.setBorder(new MatteBorder(0, 0, 0, 2, DARK_SAND));
    sidebarPanel.setPreferredSize(new Dimension(240, getHeight()));

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.insets = new Insets(30, 15, 12, 15);

    navMake = createNavButton("Make Reservation", SimpleIcon.Type.BUILDING);
    navMyBookings = createNavButton("My Bookings", SimpleIcon.Type.BOOKS);
    navLogout = createNavButton("Log Out", SimpleIcon.Type.KEY);

    sidebarPanel.add(navMake, c);
    c.gridy++;
    sidebarPanel.add(navMyBookings, c);
    c.gridy++;
    sidebarPanel.add(navLogout, c);
    c.gridy++;
    c.weighty = 1;
    sidebarPanel.add(Box.createVerticalGlue(), c);

    navMake.addActionListener(e -> {
        highlightNav(navMake);
        showCard("TYPE");
    });
    navMyBookings.addActionListener(e -> {
        highlightNav(navMyBookings);
        refreshMyBookingsView();
        showCard("MY_BOOKINGS");
    });
    navLogout.addActionListener(e -> {
        currentUser = null;
        getContentPane().remove(headerPanel);
        getContentPane().remove(sidebarPanel);
        getContentPane().remove(root);
        getContentPane().add(root, BorderLayout.CENTER);
        highlightNav(null);
        showCard("HOME");
        revalidate();
        repaint();
    });

    highlightNav(navMake);
}

private JButton createNavButton(String text, SimpleIcon.Type iconType) {
    JButton btn = new JButton(text, new SimpleIcon(20, 20, iconType));
    btn.setHorizontalAlignment(SwingConstants.LEFT);
    btn.setForeground(TEXT_DARK);
    btn.setFont(new Font("Sans-Serif", Font.PLAIN, 15));
    btn.setFocusPainted(false);
    btn.setOpaque(true);
    btn.setBackground(SAND);
    btn.setBorder(new EmptyBorder(12, 15, 12, 15));
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btn.setPreferredSize(new Dimension(210, 50));

    btn.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (btn.getBackground().equals(SAND)) {
                btn.setBackground(DARK_SAND);
                btn.setForeground(WARM_WHITE);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (btn.getBackground().equals(DARK_SAND)) {
                btn.setBackground(SAND);
                btn.setForeground(TEXT_DARK);
            }
        }
    });

    return btn;
}

private void highlightNav(JButton active) {
    JButton[] all = {navMake, navMyBookings, navLogout};
    for (JButton b : all) {
        if (b == null) continue;
        if (b == active) {
            b.setBackground(WARM_WHITE);
            b.setForeground(TEXT_DARK);
            b.setBorder(new CompoundBorder(
                    new LineBorder(GOLD_ACCENT, 2, true),
                    new EmptyBorder(10, 13, 10, 13)));
        } else {
            b.setBackground(SAND);
            b.setForeground(TEXT_DARK);
            b.setBorder(new EmptyBorder(12, 15, 12, 15));
        }
    }
}

private void enableDashboardUI() {
    if (((BorderLayout) getContentPane().getLayout())
            .getLayoutComponent(BorderLayout.NORTH) == headerPanel)
        return;

    getContentPane().remove(root);
    getContentPane().add(headerPanel, BorderLayout.NORTH);
    getContentPane().add(sidebarPanel, BorderLayout.WEST);

    root.setBackground(WARM_WHITE);
    root.setBorder(new EmptyBorder(25, 25, 25, 25));
    getContentPane().add(root, BorderLayout.CENTER);

    revalidate();
    repaint();
}

// HOME - with background image
private void buildHome() {
    JPanel p = new BackgroundImagePanel(homepage);
    p.setLayout(new GridBagLayout());
    GridBagConstraints c = gbc();
    c.insets = new Insets(20, 20, 20, 20);

    // Semi-transparent overlay panel
    JPanel overlay = new JPanel(new GridBagLayout());
    overlay.setBackground(new Color(250, 248, 243, 220));
    overlay.setBorder(new CompoundBorder(
            new LineBorder(GOLD_ACCENT, 3, true),
            new EmptyBorder(40, 50, 40, 50)));

    overlay.setPreferredSize(new Dimension(520, 360));
    GridBagConstraints oc = gbc();

    JLabel logo = new JLabel(new ImageIcon(logoImage));
    logo.setBorder(new EmptyBorder(0, 0, 20, 0));

    JLabel subtitle = new JLabel("Experience luxury in the heart of AlUla");
    subtitle.setFont(SUBTITLE_FONT);
    subtitle.setForeground(DARK_SAND);

    JButton btnLogin = styledDesertButton("Log In");
    JButton btnSignUp = styledDesertButton("Sign Up");

    btnLogin.addActionListener(e -> showCard("LOGIN"));
    btnSignUp.addActionListener(e -> showCard("SIGNUP"));

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
    actions.setOpaque(false);
    actions.add(btnLogin);
    actions.add(btnSignUp);

    oc.gridy = 0;
    overlay.add(logo, oc);
    oc.gridy = 1;
    overlay.add(subtitle, oc);
    oc.gridy = 2;
    overlay.add(Box.createVerticalStrut(15), oc);
    oc.gridy = 3;
    overlay.add(actions, oc);

    p.add(overlay, c);

    root.add(p, "HOME");
}

// SIGN UP
private void buildSignUp() {
    JPanel p = new BackgroundImagePanel(homepage); // Use background image
    p.setLayout(new GridBagLayout());
    GridBagConstraints c = gbc();
    c.insets = new Insets(20, 20, 20, 20);

    // Create card with semi-transparent background
    JPanel card = new JPanel(new GridBagLayout());
    card.setBackground(new Color(250, 248, 243, 240)); // More opaque than homepage
    card.setBorder(new CompoundBorder(
            new LineBorder(GOLD_ACCENT, 2, true),
            new EmptyBorder(30, 40, 30, 40)));

    GridBagConstraints cc = gbc();

    JLabel title = new JLabel("Create Your Account");
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);

    JTextField tfUser = styledTextField(20);
    JPasswordField tfPass = styledPasswordField(20);

    cc.gridx = 0;
    cc.gridy = 0;
    cc.gridwidth = 2;
    cc.anchor = GridBagConstraints.CENTER;
    card.add(title, cc);
    cc.gridwidth = 1;
    cc.anchor = GridBagConstraints.LINE_END;
    cc.gridx = 0;
    cc.gridy = 1;
    card.add(new JLabel("Username:"), cc);
    cc.gridx = 1;
    cc.anchor = GridBagConstraints.LINE_START;
    card.add(tfUser, cc);
    cc.gridx = 0;
    cc.gridy = 2;
    cc.anchor = GridBagConstraints.LINE_END;
    card.add(new JLabel("Password:"), cc);
    cc.gridx = 1;
    cc.anchor = GridBagConstraints.LINE_START;
    card.add(tfPass, cc);

    JButton btnRegister = styledDesertButton("Register");
    JButton btnBack = styledLightButton("Back");

    cc.gridx = 1;
    cc.gridy = 3;
    cc.anchor = GridBagConstraints.CENTER;
    card.add(btnRegister, cc);
    cc.gridy = 4;
    card.add(btnBack, cc);

    btnRegister.addActionListener(e -> {
        if (!ensureConnected()) return;
        String u = tfUser.getText().trim();
        String pw = new String(tfPass.getPassword());
        if (u.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill username and password.");
            return;
        }
        try {
            String resp = client.register(u, pw);
            if (resp != null && resp.startsWith("OK")) {
                currentUser = u;
                JOptionPane.showMessageDialog(this, "Welcome to Sahara Resort!");
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

    p.add(card, c);
    root.add(p, "SIGNUP");
}

// LOGIN
private void buildLogIn() {
    JPanel p = new BackgroundImagePanel(homepage); // Use background image
    p.setLayout(new GridBagLayout());
    GridBagConstraints c = gbc();
    c.insets = new Insets(20, 20, 20, 20);

    // Create card with semi-transparent background
    JPanel card = new JPanel(new GridBagLayout());
    card.setBackground(new Color(250, 248, 243, 240)); // More opaque than homepage
    card.setBorder(new CompoundBorder(
            new LineBorder(GOLD_ACCENT, 2, true),
            new EmptyBorder(30, 40, 30, 40)));

    GridBagConstraints cc = gbc();

    JLabel title = new JLabel("Welcome Back");
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);

    JTextField tfUser = styledTextField(20);
    JPasswordField tfPass = styledPasswordField(20);

    cc.gridx = 0;
    cc.gridy = 0;
    cc.gridwidth = 2;
    cc.anchor = GridBagConstraints.CENTER;
    card.add(title, cc);
    cc.gridwidth = 1;
    cc.anchor = GridBagConstraints.LINE_END;
    cc.gridx = 0;
    cc.gridy = 1;
    card.add(new JLabel("Username:"), cc);
    cc.gridx = 1;
    cc.anchor = GridBagConstraints.LINE_START;
    card.add(tfUser, cc);
    cc.gridx = 0;
    cc.gridy = 2;
    cc.anchor = GridBagConstraints.LINE_END;
    card.add(new JLabel("Password:"), cc);
    cc.gridx = 1;
    cc.anchor = GridBagConstraints.LINE_START;
    card.add(tfPass, cc);

    JButton btnLogin = styledDesertButton("Log In");
    JButton btnBack = styledLightButton("Back");

    cc.gridx = 1;
    cc.gridy = 3;
    cc.anchor = GridBagConstraints.CENTER;
    card.add(btnLogin, cc);
    cc.gridy = 4;
    card.add(btnBack, cc);

    btnLogin.addActionListener(e -> {
        if (!ensureConnected()) return;
        String u = tfUser.getText().trim();
        String pw = new String(tfPass.getPassword());
        if (u.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username.");
            return;
        }
        try {
            String resp = client.login(u, pw);
            if (resp.startsWith("OK")) {
                currentUser = u;
                JOptionPane.showMessageDialog(this, "Welcome back to Sahara Resort!");
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

    p.add(card, c);
    root.add(p, "LOGIN");
}

// MENU
private void buildMenu() {
    JPanel p = new JPanel(new GridBagLayout());
    p.setBackground(WARM_WHITE);
    GridBagConstraints c = gbc();

    JLabel title = new JLabel("What would you like to do?");
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);

    JButton btnMake = styledDesertButton("Make a Reservation");
    JButton btnMy = styledDesertButton("View My Bookings");
    JButton btnLogout = styledLightButton("Log Out");

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.CENTER;
    title.setHorizontalAlignment(SwingConstants.CENTER);
    p.add(title, c);
    c.gridwidth = 1;
    c.gridy = 1;
    p.add(btnMake, c);
    c.gridy = 2;
    p.add(btnMy, c);
    c.gridy = 3;
    p.add(btnLogout, c);

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
        getContentPane().remove(headerPanel);
        getContentPane().remove(sidebarPanel);
        getContentPane().remove(root);
        root.setBackground(WARM_WHITE);
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
    p.setBackground(WARM_WHITE);
    GridBagConstraints c = gbc();

    JLabel title = new JLabel("Choose Room Type");
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);

    cbType = new JComboBox<>(ROOM_TYPES);
    cbType.setPreferredSize(new Dimension(200, 35));

    JButton next = styledDesertButton("Next");
    JButton back = styledLightButton("Back");

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    p.add(title, c);
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    p.add(new JLabel("Room Type:"), c);
    c.gridx = 1;
    p.add(cbType, c);
    c.gridx = 1;
    c.gridy = 2;
    p.add(next, c);
    c.gridy = 3;
    p.add(back, c);

    next.addActionListener(e -> {
        if (!ensureConnected()) return;
        selectedType = cbType.getSelectedItem().toString();
        showCard("DURATION");
    });

    back.addActionListener(e -> showCard("MENU"));
    root.add(wrapCard(p), "TYPE");
}

// DURATION
private void buildDuration() {
    JPanel p = new JPanel(new GridBagLayout());
    p.setBackground(WARM_WHITE);
    GridBagConstraints c = gbc();

    JLabel title = new JLabel("Choose Dates");
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);

    cbStartDay = new JComboBox<>();
    cbNights = new JComboBox<>();
    for (String d : WEEK_DAYS) cbStartDay.addItem(d);
    for (int n = 1; n <= 7; n++) cbNights.addItem(n);

    cbStartDay.setPreferredSize(new Dimension(200, 35));
    cbNights.setPreferredSize(new Dimension(200, 35));

    JButton next = styledDesertButton("Find Rooms");
    JButton back = styledLightButton("Back");

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    p.add(title, c);
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    p.add(new JLabel("Check-in Day:"), c);
    c.gridx = 1;
    p.add(cbStartDay, c);
    c.gridx = 0;
    c.gridy = 2;
    p.add(new JLabel("Number of Nights:"), c);
    c.gridx = 1;
    p.add(cbNights, c);

    c.gridx = 1;
    c.gridy = 3;
    p.add(next, c);
    c.gridy = 4;
    p.add(back, c);

    next.addActionListener(e -> {
        if (!ensureConnected()) return;
        String dayName = (String) cbStartDay.getSelectedItem();
        selectedStartDay = dayNameToIndex(dayName);
        selectedNights = (Integer) cbNights.getSelectedItem();

        int endDay = ((selectedStartDay - 1 + selectedNights - 1) % 7) + 1;
        String startName = dayName;
        String endName = WEEK_DAYS[endDay - 1];

        JOptionPane.showMessageDialog(this,
                "Searching for rooms from " + startName + " to " + endName +
                        " (" + selectedNights + " nights)");

        lastAvailableRooms = findAvailableRooms(selectedType, selectedStartDay, selectedNights);
        if (lastAvailableRooms.isEmpty()) showCard("NO_AVAIL");
        else {
            refreshResultsList();
            showCard("RESULTS");
        }
    });

    back.addActionListener(e -> showCard("TYPE"));
    root.add(wrapCard(p), "DURATION");
}

// RESULTS
private void buildResults() {
    JPanel p = new JPanel(new BorderLayout(15, 15));
    p.setBackground(WARM_WHITE);

    JLabel title = new JLabel("Available Rooms", SwingConstants.CENTER);
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);
    title.setBorder(new EmptyBorder(0, 0, 15, 0));

    roomsModel = new DefaultListModel<>();
    listRooms = new JList<>(roomsModel);
    listRooms.setVisibleRowCount(6);
    listRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listRooms.setFixedCellHeight(60);
    listRooms.setSelectionBackground(DARK_BROWN);
    listRooms.setSelectionForeground(WARM_WHITE);
    listRooms.setBackground(new Color(250, 250, 250));

    listRooms.setCellRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            lbl.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    new EmptyBorder(12, 15, 12, 15)));
            lbl.setFont(new Font("Sans-Serif", Font.PLAIN, 15));
            if (isSelected) {
                lbl.setBackground(DARK_BROWN);
                lbl.setForeground(WARM_WHITE);
            } else {
                lbl.setBackground(new Color(250, 250, 250));
                lbl.setForeground(TEXT_DARK);
            }
            lbl.setOpaque(true);
            return lbl;
        }
    });

    JScrollPane sp = new JScrollPane(listRooms);
    sp.setPreferredSize(new Dimension(600, 320));
    sp.setBorder(new LineBorder(SAND, 2, true));

    JButton reserve = styledDesertButton("Reserve Room");
    JButton back = styledLightButton("Back");

    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    btnPanel.setOpaque(false);
    btnPanel.add(reserve);
    btnPanel.add(back);

    p.add(title, BorderLayout.NORTH);
    p.add(sp, BorderLayout.CENTER);
    p.add(btnPanel, BorderLayout.SOUTH);

    reserve.addActionListener(e -> {
        if (!ensureConnected()) return;
        String sel = listRooms.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Please select a room.");
            return;
        }
        selectedRoomId = sel;
        try {
            String resp = client.bookRoom(
                    currentUser, selectedType, selectedRoomId,
                    selectedStartDay, selectedNights);
            if (resp.startsWith("OK CONFIRMED")) {
                JOptionPane.showMessageDialog(this,
                        "Reservation confirmed for " + selectedRoomId + "!");
                showCard("MENU");
            } else {
                JOptionPane.showMessageDialog(this, resp);
                lastAvailableRooms = findAvailableRooms(
                        selectedType, selectedStartDay, selectedNights);
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
    p.setBackground(WARM_WHITE);
    GridBagConstraints c = gbc();

    JLabel msg = new JLabel("No available rooms for your selection.");
    msg.setFont(HEADER_FONT);
    msg.setForeground(TEXT_DARK);

    JButton changeDur = styledDesertButton("Change Dates");
    JButton changeType = styledDesertButton("Change Room Type");

    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.CENTER;
    p.add(msg, c);
    c.gridy = 1;
    p.add(changeDur, c);
    c.gridy = 2;
    p.add(changeType, c);

    changeDur.addActionListener(e -> showCard("DURATION"));
    changeType.addActionListener(e -> showCard("TYPE"));
    root.add(wrapCard(p), "NO_AVAIL");
}

// MY_BOOKINGS
private void buildMyBookings() {
    JPanel p = new JPanel(new BorderLayout(12, 12));
    p.setBackground(WARM_WHITE);

    JLabel title = new JLabel("My Reservations", SwingConstants.CENTER);
    title.setFont(HEADER_FONT);
    title.setForeground(TEXT_DARK);
    title.setBorder(new EmptyBorder(0, 0, 15, 0));

    bookingsModel = new DefaultListModel<>();
    bookingsList = new JList<>(bookingsModel);
    bookingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    bookingsList.setVisibleRowCount(8);
    bookingsList.setFixedCellHeight(30);
    bookingsList.setBackground(new Color(250, 250, 250));
    bookingsList.setSelectionBackground(DARK_BROWN);
    bookingsList.setSelectionForeground(WARM_WHITE);
    bookingsList.setFont(NORMAL_FONT);

    JScrollPane sp = new JScrollPane(bookingsList);
    sp.setBorder(new LineBorder(SAND, 2, true));

    JButton btnCancel = styledDesertButton("Cancel Reservation");
    JButton btnBack = styledLightButton("Back");

    JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    south.setOpaque(false);
    south.add(btnCancel);
    south.add(btnBack);

    btnBack.addActionListener(e -> showCard("MENU"));

    btnCancel.addActionListener(e -> {
        if (!ensureConnected()) return;
        int idx = bookingsList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a reservation to cancel.");
            return;
        }
        if (idx >= bookingResIds.size()) {
            JOptionPane.showMessageDialog(this,
                    "Internal error: reservation id not found.");
            return;
        }
        String resId = bookingResIds.get(idx);
        if (resId == null || resId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot cancel this entry.");
            return;
        }
        try {
            String resp = client.cancelReservation(currentUser, resId);
            if (resp.startsWith("OK CANCELED")) {
                JOptionPane.showMessageDialog(this, "Reservation canceled.");
                refreshMyBookingsView();
            } else {
                JOptionPane.showMessageDialog(this, resp);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Connection error: " + ex.getMessage());
        }
    });

    p.add(title, BorderLayout.NORTH);
    p.add(sp, BorderLayout.CENTER);
    p.add(south, BorderLayout.SOUTH);

    root.add(wrapCard(p), "MY_BOOKINGS");
}

private GridBagConstraints gbc() {
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(12, 12, 12, 12);
    c.fill = GridBagConstraints.HORIZONTAL;
    return c;
}

private JPanel wrapCard(JPanel inner) {
    JPanel outer = new JPanel(new BorderLayout());
    outer.setOpaque(false);
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(WARM_WHITE);
    card.setBorder(new CompoundBorder(
            new LineBorder(SAND, 2, true),
            new EmptyBorder(25, 25, 25, 25)));
    card.add(inner, BorderLayout.CENTER);
    outer.add(card, BorderLayout.CENTER);
    return outer;
}

private JTextField styledTextField(int cols) {
    JTextField tf = new JTextField(cols);
    tf.setFont(NORMAL_FONT);
    tf.setBorder(new CompoundBorder(
            new LineBorder(SAND, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
    tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 38));
    return tf;
}

private JPasswordField styledPasswordField(int cols) {
    JPasswordField pf = new JPasswordField(cols);
    pf.setFont(NORMAL_FONT);
    pf.setBorder(new CompoundBorder(
            new LineBorder(SAND, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
    pf.setPreferredSize(new Dimension(pf.getPreferredSize().width, 38));
    return pf;
}

private JButton styledDesertButton(String text) {
    JButton b = new JButton(text);
    b.setFont(new Font("Sans-Serif", Font.BOLD, 14));
    b.setBackground(DARK_SAND);
    b.setForeground(WARM_WHITE);
    b.setFocusPainted(false);
    b.setBorder(new CompoundBorder(
            new LineBorder(DARK_SAND, 1, true),
            new EmptyBorder(10, 20, 10, 20)));
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.setOpaque(true);

    b.addMouseListener(new MouseAdapter() {
        Color base = DARK_SAND;
        Color hover = GOLD_ACCENT;

        @Override
        public void mouseEntered(MouseEvent e) {
            b.setBackground(hover);
            b.setForeground(TEXT_DARK);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            b.setBackground(base);
            b.setForeground(WARM_WHITE);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            b.setBackground(hover.darker());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            b.setBackground(hover);
        }
    });
    return b;
}

private JButton styledLightButton(String text) {
    JButton b = new JButton(text);
    b.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
    b.setBackground(WARM_WHITE);
    b.setForeground(TEXT_DARK);
    b.setFocusPainted(false);
    b.setBorder(new CompoundBorder(
            new LineBorder(SAND, 2, true),
            new EmptyBorder(10, 20, 10, 20)));
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.setOpaque(true);
    b.setPreferredSize(new Dimension(180, 44));

    b.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            b.setBackground(SAND);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            b.setBackground(WARM_WHITE);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            b.setBackground(DARK_SAND);
            b.setForeground(WARM_WHITE);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            b.setBackground(SAND);
            b.setForeground(TEXT_DARK);
        }
    });
    return b;
}

private void showCard(String name) {
    cards.show(root, name);
}

private void refreshResultsList() {
    roomsModel.clear();
    for (String r : lastAvailableRooms)
        roomsModel.addElement(r);
}

private boolean ensureConnected() {
    if (!serverConnected) {
        JOptionPane.showMessageDialog(this,
                "Server is disconnected. Please start the server.");
        showCard("HOME");
        return false;
    }
    return true;
}

private List<String> findAvailableRooms(String type, int startDay, int nights) {
    List<String> out = new ArrayList<>();
    if (!ensureConnected())
        return out;
    try {
        String resp = client.listAvail(type, startDay, nights);
        if (resp.startsWith("OK ROOMS")) {
            String csv = resp.substring("OK ROOMS".length()).trim();
            if (!csv.isEmpty()) {
                for (String s : csv.split(",")) {
                    String id = s.trim();
                    if (!id.isEmpty())
                        out.add(id);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, resp);
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
                "Connection error: " + ex.getMessage());
    }
    return out;
}

private void refreshMyBookingsView() {
    bookingsModel.clear();
    bookingResIds.clear();

    if (currentUser == null || currentUser.isEmpty()) {
        bookingsModel.addElement("Please log in first.");
        return;
    }
    if (!ensureConnected())
        return;
    try {
        String resp = client.myReservations(currentUser);
        if (resp.startsWith("OK RES")) {
            String csv = resp.substring("OK RES".length()).trim();
            if (csv.isEmpty()) {
                bookingsModel.addElement("No reservations yet.");
                return;
            }
            String[] items = csv.split(",");
            for (int i = 0; i < items.length; i++) {
                String item = items[i].trim();
                if (item.isEmpty()) continue;

                String resId = "";
                String info = item;
                int pipePos = item.indexOf('|');
                if (pipePos >= 0) {
                    resId = item.substring(0, pipePos).trim();
                    info = item.substring(pipePos + 1).trim();
                }

                String display = info;
                String roomId = info;
                int day = -1, nights = -1;
                int atPos = info.indexOf('@');
                if (atPos >= 0) {
                    roomId = info.substring(0, atPos);
                    String rest = info.substring(atPos + 1);
                    int xPos = rest.indexOf('x');
                    if (xPos >= 0) {
                        try {
                            day = Integer.parseInt(rest.substring(0, xPos));
                            nights = Integer.parseInt(rest.substring(xPos + 1));
                            String dayName = WEEK_DAYS[day - 1];
                            display = roomId + " (" + dayName + ", " + nights + " nights)";
                        } catch (NumberFormatException ignored) {
                            display = info;
                        }
                    } else {
                        display = info;
                    }
                }

                bookingsModel.addElement((i + 1) + ") " + display);
                bookingResIds.add(resId);
            }
        } else {
            bookingsModel.addElement(resp);
        }
    } catch (Exception ex) {
        bookingsModel.addElement("Connection error: " + ex.getMessage());
    }
}

private int dayNameToIndex(String dayName) {
    for (int i = 0; i < WEEK_DAYS.length; i++) {
        if (WEEK_DAYS[i].equalsIgnoreCase(dayName))
            return i + 1;
    }
    return 1;
}

private static class BackgroundImagePanel extends JPanel {
    private final BufferedImage bgImage;

    public BackgroundImagePanel(BufferedImage img) {
        this.bgImage = img;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }
}

private static class SimpleIcon implements Icon {
    enum Type { HOME, ADD_USER, KEY, BUILDING, BOOKS, ROOF }

    private final int w, h;
    private final Type type;
    private Color color = new Color(70, 70, 70);

    SimpleIcon(int w, int h, Type t) {
        this.w = w;
        this.h = h;
        this.type = t;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(color);
        switch (type) {
            case HOME:
                Path2D.Double home = new Path2D.Double();
                home.moveTo(x + w * 0.1, y + h * 0.6);
                home.lineTo(x + w * 0.5, y + h * 0.15);
                home.lineTo(x + w * 0.9, y + h * 0.6);
                home.lineTo(x + w * 0.9, y + h * 0.9);
                home.lineTo(x + w * 0.6, y + h * 0.9);
                home.lineTo(x + w * 0.6, y + h * 0.65);
                home.lineTo(x + w * 0.4, y + h * 0.65);
                home.lineTo(x + w * 0.4, y + h * 0.9);
                home.lineTo(x + w * 0.1, y + h * 0.9);
                home.closePath();
                g2.fill(home);
                break;
            case ADD_USER:
                g2.fill(new Ellipse2D.Double(x + w * 0.15, y + h * 0.08,
                        w * 0.7, h * 0.5));
                g2.fill(new RoundRectangle2D.Double(x + w * 0.05, y + h * 0.6,
                        w * 0.9, h * 0.3, 6, 6));
                break;
            case KEY:
                g2.fill(new Ellipse2D.Double(x + w * 0.05, y + h * 0.25,
                        w * 0.5, h * 0.5));
                g2.fill(new Rectangle2D.Double(x + w * 0.55, y + h * 0.45,
                        w * 0.35, h * 0.15));
                break;
            case BUILDING:
                g2.fill(new Rectangle2D.Double(x + w * 0.15, y + h * 0.15,
                        w * 0.7, h * 0.7));
                g2.setPaint(Color.WHITE);
                g2.fill(new Rectangle2D.Double(x + w * 0.28, y + h * 0.28,
                        w * 0.12, h * 0.12));
                g2.fill(new Rectangle2D.Double(x + w * 0.5, y + h * 0.28,
                        w * 0.12, h * 0.12));
                g2.fill(new Rectangle2D.Double(x + w * 0.28, y + h * 0.52,
                        w * 0.12, h * 0.12));
                break;
            case BOOKS:
                g2.fill(new RoundRectangle2D.Double(x + w * 0.12, y + h * 0.2,
                        w * 0.75, h * 0.6, 4, 4));
                g2.setPaint(Color.WHITE);
                g2.fill(new Rectangle2D.Double(x + w * 0.18, y + h * 0.25,
                        w * 0.45, h * 0.05));
                g2.fill(new Rectangle2D.Double(x + w * 0.18, y + h * 0.4,
                        w * 0.45, h * 0.05));
                break;
            case ROOF:
                Path2D.Double roof = new Path2D.Double();
                roof.moveTo(x + w * 0.1, y + h * 0.6);
                roof.lineTo(x + w * 0.5, y + h * 0.2);
                roof.lineTo(x + w * 0.9, y + h * 0.6);
                roof.closePath();
                g2.fill(roof);
                break;
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() { return w; }
    @Override
    public int getIconHeight() { return h; }
}

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new ReservationGUI().setVisible(true));
}

}
