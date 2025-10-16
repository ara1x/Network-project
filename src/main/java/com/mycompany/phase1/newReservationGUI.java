package com.mycompany.phase1;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class newReservationGUI extends JFrame {

    private final Client client = new Client();

    // ----------- STATE -----------
    private String currentUser = null;
    private String selectedType = "STANDARD";
    private int selectedStartDay = 1;
    private int selectedNights = 1;
    private String selectedRoomId = null;
    private List<String> lastAvailableRooms = new ArrayList<>();

    // ----------- UI infra -----------
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private JComboBox<String> cbType;
    private JComboBox<Integer> cbStartDay;
    private JComboBox<Integer> cbNights;
    private JList<String> listRooms;
    private DefaultListModel<String> roomsModel;
    private JTextArea myBookingsArea;

    // ----------- Constants -----------
    private static final String[] ROOM_TYPES = {"STANDARD", "PREMIUM", "SUITE"};
    private static final String[] STD_ROOMS = {"S1","S2","S3","S4","S5"};
    private static final String[] PRM_ROOMS = {"P1","P2","P3","P4","P5"};
    private static final String[] STE_ROOMS = {"U1","U2","U3","U4","U5"};

    private final Map<String, List<String>> userBookings = new HashMap<>();

    private final boolean[][][] availability = new boolean[3][5][7];

    public ReservationGUI() {
        setTitle("Online Reservation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(null);

        try {
            client.connect("10.6.207.31", 9090);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Server not found (using local demo grid).");
        }

        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 5; r++) {
                Arrays.fill(availability[c][r], true);
            }
        }

        buildHome();
        buildSignUp();
        buildLogIn();
        buildMenu();
        buildType();
        buildDuration();
        buildResults();
        buildNoAvail();
        buildMyBookings();

        add(root, BorderLayout.CENTER);
        showCard("HOME");
    }

    // ============================ HOME ============================
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

    // ============================ SIGN UP ============================
    private void buildSignUp() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JTextField tfUser = new JTextField(16);
        JPasswordField tfPass = new JPasswordField(16);

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btn = new JButton("Register");
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
                    showCard("MENU");
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

    // ============================ LOG IN ============================
    private void buildLogIn() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JTextField tfUser = new JTextField(16);
        JPasswordField tfPass = new JPasswordField(16);

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; c.anchor=GridBagConstraints.LINE_END; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor=GridBagConstraints.LINE_START; p.add(tfPass, c);

        JButton btn = new JButton("Log in");
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
            showCard("MENU");
        });

        back.addActionListener(e -> showCard("HOME"));
        root.add(p, "LOGIN");
    }

    // ============================ MENU ============================
    private void buildMenu() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("What would you like to do?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton btnMake = new JButton("Make a reservation");
        JButton btnMy = new JButton("My bookings");
        JButton btnExit = new JButton("Exit");
        JButton back = new JButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; c.anchor=GridBagConstraints.CENTER; p.add(title, c);
        c.gridwidth=1;
        c.gridy=1; p.add(btnMake, c);
        c.gridy=2; p.add(btnMy, c);
        c.gridy=3; p.add(btnExit, c);
        c.gridy=4; p.add(back, c);

        btnMake.addActionListener(e -> showCard("TYPE"));
        btnMy.addActionListener(e -> {
            refreshMyBookingsView();
            showCard("MY_BOOKINGS");
        });
        btnExit.addActionListener(e -> System.exit(0));
        back.addActionListener(e -> showCard("HOME"));

        root.add(p, "MENU");
    }

    // ============================ TYPE ============================
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
            showCard("DURATION");
        });

        back.addActionListener(e -> showCard("MENU"));
        root.add(p, "TYPE");
    }

    // ============================ DURATION ============================
    private void buildDuration() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Choose start day (1–7) and nights (1–7)");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        cbStartDay = new JComboBox<>();
        cbNights = new JComboBox<>();
        for (int d = 1; d <= 7; d++) cbStartDay.addItem(d);
        for (int n = 1; n <= 7; n++) cbNights.addItem(n);

        JButton next = new JButton("Find availability");
        JButton back = new JButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title, c);
        c.gridwidth=1;
        c.gridx=0; c.gridy=1; p.add(new JLabel("Start day:"), c);
        c.gridx=1; p.add(cbStartDay, c);
        c.gridx=0; c.gridy=2; p.add(new JLabel("Nights:"), c);
        c.gridx=1; p.add(cbNights, c);

        c.gridx=1; c.gridy=3; p.add(next, c);
        c.gridy=4; p.add(back, c);

        next.addActionListener(e -> {
            selectedStartDay = (Integer) cbStartDay.getSelectedItem();
            selectedNights = (Integer) cbNights.getSelectedItem();
            int endDay = selectedStartDay + selectedNights - 1;
            if (endDay > 7) {
                JOptionPane.showMessageDialog(this, "End day exceeds 7.");
                return;
            }
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

    // ============================ RESULTS ============================
    private void buildResults() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel title = new JLabel("Available rooms");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roomsModel = new DefaultListModel<>();
        listRooms = new JList<>(roomsModel);
        listRooms.setVisibleRowCount(6);
        listRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(listRooms);

        JButton reserve = new JButton("Reserve");
        JButton back = new JButton("Back");

        c.gridx=0; c.gridy=0; c.gridwidth=2; c.fill = GridBagConstraints.HORIZONTAL; p.add(title, c);
        c.gridwidth=2; c.gridy=1; c.weightx=1; c.weighty=1; c.fill = GridBagConstraints.BOTH; p.add(sp, c);
        c.weightx=0; c.weighty=0; c.fill = GridBagConstraints.NONE;

        c.gridwidth=1; c.gridy=2; c.gridx=0; p.add(reserve, c);
        c.gridx=1; p.add(back, c);

        reserve.addActionListener(e -> {
            String sel = listRooms.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Select a room.");
                return;
            }
            selectedRoomId = sel;
            doBook(currentUser, selectedType, selectedRoomId, selectedStartDay, selectedNights);
            userBookings.computeIfAbsent(currentUser, k -> new ArrayList<>())
                        .add(selectedRoomId + " - Day " + selectedStartDay + " x " + selectedNights);
            JOptionPane.showMessageDialog(this, "Booking confirmed for " + selectedRoomId);
            showCard("MENU");
        });

        back.addActionListener(e -> showCard("DURATION"));
        root.add(p, "RESULTS");
    }

    // ============================ NO_AVAIL ============================
    private void buildNoAvail() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JLabel msg = new JLabel("No available rooms for your inputs.");
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JButton changeDur = new JButton("Change duration");
        JButton changeType = new JButton("Change type");

        c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.CENTER; p.add(msg, c);
        c.gridy=1; p.add(changeDur, c);
        c.gridy=2; p.add(changeType, c);

        changeDur.addActionListener(e -> showCard("DURATION"));
        changeType.addActionListener(e -> showCard("TYPE"));
        root.add(p, "NO_AVAIL");
    }

    // ============================ MY_BOOKINGS ============================
    private void buildMyBookings() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        JLabel title = new JLabel("My bookings", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        myBookingsArea = new JTextArea();
        myBookingsArea.setEditable(false);

        JButton btnCancelDemo = new JButton("Cancel reservation");
        btnCancelDemo.setEnabled(false);

        JButton back = new JButton("Back");
        JButton exit = new JButton("Exit");

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        south.add(btnCancelDemo);
        south.add(back);
        south.add(exit);

        back.addActionListener(e -> showCard("MENU"));
        exit.addActionListener(e -> System.exit(0));

        p.add(title, BorderLayout.NORTH);
        p.add(new JScrollPane(myBookingsArea), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        root.add(p, "MY_BOOKINGS");
    }

    private void refreshMyBookingsView() {
        List<String> list = userBookings.getOrDefault(currentUser, Collections.emptyList());
        if (list.isEmpty()) myBookingsArea.setText("No bookings yet.");
        else {
            StringBuilder sb = new StringBuilder();
            for (String s : list) sb.append("• ").append(s).append('\n');
            myBookingsArea.setText(sb.toString());
        }
    }

    // ============================ Helpers ============================
    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        return c;
    }

    private void showCard(String name) { cards.show(root, name); }

    private void refreshResultsList() {
        roomsModel.clear();
        for (String r : lastAvailableRooms) roomsModel.addElement(r);
    }

    private int catIndexOf(String type) {
        switch (type.toUpperCase()) {
            case "STANDARD": return 0;
            case "PREMIUM":  return 1;
            case "SUITE":    return 2;
            default:         return 0;
        }
    }

    private String[] roomsOf(String type) {
        switch (type.toUpperCase()) {
            case "STANDARD": return STD_ROOMS;
            case "PREMIUM":  return PRM_ROOMS;
            case "SUITE":    return STE_ROOMS;
            default:         return STD_ROOMS;
        }
    }

    private List<String> findAvailableRooms(String type, int startDay, int nights) {
        List<String> out = new ArrayList<>();
        int cat = catIndexOf(type);
        String[] ids = roomsOf(type);
        int end = startDay + nights - 1;
        for (int r = 0; r < 5; r++) {
            boolean ok = true;
            for (int d = startDay; d <= end; d++) {
                if (!availability[cat][r][d-1]) { ok = false; break; }
            }
            if (ok) out.add(ids[r]);
        }
        return out;
    }

    private void doBook(String user, String type, String roomId, int startDay, int nights) {
        int cat = catIndexOf(type);
        int roomIdx = roomIndexOf(type, roomId);
        for (int d = startDay; d <= startDay + nights - 1; d++) {
            availability[cat][roomIdx][d-1] = false;
        }
    }

    private int roomIndexOf(String type, String roomId) {
        String[] ids = roomsOf(type);
        for (int i = 0; i < ids.length; i++) if (ids[i].equalsIgnoreCase(roomId)) return i;
        return -1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReservationGUI().setVisible(true));
    }
}
