package com.mycompany.phase1;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReservationGUI extends JFrame {

    // ===== your existing Client API (must exist in project) =====
    // void   connect(String host, int port) throws IOException
    // String register(String username, String password) throws IOException
    // String show(String type) throws IOException                // "OK AVAILABLE 1,3,5" | "ERR NO_AVAIL"
    // String book(String username, String type, int number) throws IOException // number=slot 1..12
    private final Client client = new Client();

    // simple state
    private String currentUser = null;
    private String selectedType = "STANDARD";
    private int selectedDay = 0;
    private int selectedTimeSlot = 1; // 1..12

    // room types
    private final String[] ROOM_TYPES = {"STANDARD", "PREMIUM", "SUITE"};

    // day labels (7 days)
    private final String[] DAY_LABELS = {
            "Sunday", "Monday", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6"
    };

    // time labels: “12 → 12” (12 values). index 1..12 map to these.
    private final String[] TIME_LABELS = {
            "",          // 0 unused
            "12:00",     // 1
            "1:00",      // 2
            "2:00",    // 3
            "3:00",      // 4
            "4:00",      // 5
            "5:00",      // 6
            "6:00",      // 7
            "7:00",      // 8
            "8:00",      // 9
            "9:00",      // 10
            "10:00",     // 11
            "11:00",     // 12
            // (and back to 12 if you need, but we keep 12 options exactly)
    };

    // cards
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // shared widgets
    private JComboBox<String> cbType;
    private JComboBox<String> cbDay;
    private JComboBox<String> cbTime;

    public ReservationGUI() {
        setTitle("Online Reservation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 340);
        setLocationRelativeTo(null);

        // connect once
        try {
            client.connect("localhost", 9090);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Server not found. Run NewServer first.");
        }

        buildHome();
        buildSignUp();
        buildLogIn();
        buildType();
        buildDayTime();

        add(root, BorderLayout.CENTER);
        showCard("HOME");
    }

    // ===== HOME: choose Sign up or Log in =====
    private void buildHome() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        JLabel title = new JLabel("Online Reservation System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton btnSU = new JButton("Sign up");
        JButton btnLI = new JButton("Log in");

        JPanel actions = new JPanel();
        actions.add(btnSU);
        actions.add(btnLI);

        btnSU.addActionListener(e -> showCard("SIGNUP"));
        btnLI.addActionListener(e -> showCard("LOGIN"));

        p.add(title, BorderLayout.NORTH);
        p.add(new JPanel(), BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);

        root.add(p, "HOME");
    }

    // ===== SIGN UP =====
    private void buildSignUp() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JTextField tfUser = new JTextField(16);
        JPasswordField tfPass = new JPasswordField(16);

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btn = new JButton("Connect & register");
        JButton back = new JButton("Back");
        c.gridx=1; c.gridy=2; c.anchor=GridBagConstraints.CENTER; p.add(btn, c);
        c.gridy=3; p.add(back, c);

        btn.addActionListener(e -> {
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
                    JOptionPane.showMessageDialog(this, "Registered successfully.");
                    showCard("TYPE");
                } else {
                    JOptionPane.showMessageDialog(this, resp == null ? "No response" : resp);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> showCard("HOME"));

        root.add(p, "SIGNUP");
    }

    // ===== LOG IN (Phase-1 placeholder) =====
    private void buildLogIn() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JTextField tfUser = new JTextField(16);
        JPasswordField tfPass = new JPasswordField(16);

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btn = new JButton("Connect & log in");
        JButton back = new JButton("Back");
        c.gridx=1; c.gridy=2; c.anchor=GridBagConstraints.CENTER; p.add(btn, c);
        c.gridy=3; p.add(back, c);

        btn.addActionListener(e -> {
            currentUser = tfUser.getText().trim();
            if (currentUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username.");
                return;
            }
            JOptionPane.showMessageDialog(this, "Logged in successfully.");
            showCard("TYPE");
        });

        back.addActionListener(e -> showCard("HOME"));

        root.add(p, "LOGIN");
    }

    // ===== choose room TYPE =====
    private void buildType() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Choose room type");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cbType = new JComboBox<>(ROOM_TYPES);

        JButton next = new JButton("Next");
        JButton back = new JButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title, c);
        c.gridwidth=1;
        c.gridx=0; c.gridy=1; p.add(new JLabel("Type:"), c);
        c.gridx=1; p.add(cbType, c);
        c.gridx=1; c.gridy=2; p.add(next, c);
        c.gridy=3; p.add(back, c);

        next.addActionListener(e -> {
            selectedType = cbType.getSelectedItem().toString();
            // when going to day/time screen, we will (try to) fetch availability to hide taken times.
            populateDayAndTimeOptions();
            showCard("DAYTIME");
        });

        back.addActionListener(e -> showCard("HOME"));

        root.add(p, "TYPE");
    }

    // ===== choose DAY (0..6) and TIME (12 → 12) =====
    private void buildDayTime() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Choose day and time");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cbDay = new JComboBox<>(DAY_LABELS);
        cbTime = new JComboBox<>();

        JButton book = new JButton("Book");
        JButton back = new JButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title, c);
        c.gridwidth=1;
        c.gridx=0; c.gridy=1; p.add(new JLabel("Day:"), c);
        c.gridx=1; p.add(cbDay, c);
        c.gridx=0; c.gridy=2; p.add(new JLabel("Time:"), c);
        c.gridx=1; p.add(cbTime, c);
        c.gridx=1; c.gridy=3; p.add(book, c);
        c.gridy=4; p.add(back, c);

        // book action
        book.addActionListener(e -> {
            if (currentUser == null || currentUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please sign up or log in first.");
                showCard("HOME");
                return;
            }
            Object sel = cbTime.getSelectedItem();
            if (sel == null || sel.toString().startsWith("No ")) {
                JOptionPane.showMessageDialog(this, "Please select a time.");
                return;
            }
            selectedDay = cbDay.getSelectedIndex(); // collected, but not sent (server API has no day)
            selectedTimeSlot = parseLeadingIndex(sel.toString()); // "3 - 2:00" -> 3

            try {
                // current server API: book(username, type, number)
                String resp = client.book(currentUser, selectedType, selectedTimeSlot);
                if (resp != null && resp.startsWith("OK")) {
                    JOptionPane.showMessageDialog(this, "Your booking has been confirmed.");
                    // re-enter time screen next time will re-fetch availability so this time disappears for others
                    showCard("TYPE");
                } else {
                    JOptionPane.showMessageDialog(this, resp == null ? "No response" : resp);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> showCard("TYPE"));

        root.add(p, "DAYTIME");
    }

    // populate times: try to filter by server availability; if none returned, list ALL 12
    private void populateDayAndTimeOptions() {
        cbTime.removeAllItems();

        List<Integer> available = fetchAvailableSlots(selectedType); // from server if possible
        if (!available.isEmpty()) {
            for (int idx : available) {
                if (idx >= 1 && idx <= 12) {
                    cbTime.addItem(idx + " - " + TIME_LABELS[idx]);
                }
            }
            if (cbTime.getItemCount() == 0) {
                cbTime.addItem("No available times");
            }
        } else {
            // server didn’t provide availability -> show ALL 12 so user can always pick
            for (int i = 1; i <= 12; i++) {
                cbTime.addItem(i + " - " + TIME_LABELS[i]);
            }
        }
    }

    // Try to parse "OK AVAILABLE 1,3,5" → [1,3,5]
    private List<Integer> fetchAvailableSlots(String type) {
        List<Integer> out = new ArrayList<>();
        try {
            String resp = client.show(type);
            if (resp == null) return out;
            resp = resp.trim();
            if (resp.startsWith("OK AVAILABLE")) {
                String csv = resp.substring("OK AVAILABLE".length()).trim();
                if (!csv.isEmpty()) {
                    String[] parts = csv.split(",");
                    for (String p : parts) {
                        try { out.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ignore) {}
        return out;
    }

    private int parseLeadingIndex(String s) {
        int dash = s.indexOf('-');
        if (dash <= 0) {
            try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 1; }
        }
        try { return Integer.parseInt(s.substring(0, dash).trim()); } catch (Exception e) { return 1; }
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        return c;
    }

    private void showCard(String name) { cards.show(root, name); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReservationGUI().setVisible(true));
    }
}
