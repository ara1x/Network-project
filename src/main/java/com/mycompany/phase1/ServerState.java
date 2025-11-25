package com.mycompany.phase1;

import java.io.*;
import java.util.*;

public class ServerState {

    private final List<User> users = new ArrayList<>();
    private final Map<String, String> creds = new HashMap<>();           // username -> password
    private final Map<String, List<Reservation>> byUser = new HashMap<>(); // username(lower) -> reservations

    // [category][roomIndex][dayIndex]
    private final boolean[][][] availability = new boolean[3][5][7];

    private static final String[] CATS = {"STANDARD","PREMIUM","SUITE"};
    private static final String[][] ROOM_IDS = {
            {"Wadi Room 1","Wadi Room 2","Wadi Room 3","Wadi Room 4","Wadi Room 5"},
            {"Oasis Room 1","Oasis Room 2","Oasis Room 3","Oasis Room 4","Oasis Room 5"},
            {"Mountain Suite 1","Mountain Suite 2","Mountain Suite 3","Mountain Suite 4","Mountain Suite 5"}
    };

    public ServerState() {
        // كل الغرف متاحة بالبداية
        for (int c = 0; c < 3; c++)
            for (int r = 0; r < 5; r++)
                Arrays.fill(availability[c][r], true);

        loadUsersFromFile();
        loadReservationsFromFile();
    }

    // ---------- users ----------
    private void loadUsersFromFile() {
        File f = new File("users.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    users.add(new User(parts[0], parts[1]));
                    creds.put(parts[0].toLowerCase(), parts[1]);
                }
            }
        } catch (IOException ignored) {}
    }

    public synchronized boolean userExists(String username) {
        return creds.containsKey(username.toLowerCase());
    }

    public synchronized boolean register(String username, String password) {
        String key = username.toLowerCase();
        if (creds.containsKey(key)) return false;
        creds.put(key, password);
        users.add(new User(username, password));
        try (FileWriter fw = new FileWriter("users.txt", true)) {
            fw.write(username + "," + password + "\n");
        } catch (IOException ignored) {}
        return true;
    }

    public synchronized boolean login(String username, String password) {
        String key = username.toLowerCase();
        return creds.containsKey(key) && Objects.equals(creds.get(key), password);
    }

    // ---------- helpers ----------
    private int catIndex(String cat) {
        String x = cat.toUpperCase();
        if (x.equals("STANDARD")) return 0;
        if (x.equals("PREMIUM"))  return 1;
        if (x.equals("SUITE"))    return 2;
        return 0;
    }

    private int roomIndexFromId(int cat, String roomId) {
        for (int i = 0; i < 5; i++) {
            if (ROOM_IDS[cat][i].equalsIgnoreCase(roomId)) return i;
        }
        return -1;
    }

    // ---------- availability ----------
    // يرجع CSV بالغرف المتاحة لنوع معيّن وفترة معيّنة
    public synchronized String listAvailableRoomsCsv(String category, int startDay, int nights) {
        int cat = catIndex(category);
        if (startDay < 1 || nights < 1) return "";

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 5; r++) {
            boolean ok = true;
            for (int i = 0; i < nights; i++) {
                int dIdx = (startDay - 1 + i) % 7;  // wrap على 7 أيام
                if (!availability[cat][r][dIdx]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                if (sb.length() > 0) sb.append(",");
                sb.append(ROOM_IDS[cat][r]);
            }
        }
        return sb.toString();
    }

    // حجز فعلي
    public synchronized String reserve(String username, String category,
                                       String roomId, int startDay, int nights) {
        int cat = catIndex(category);
        int roomIdx = roomIndexFromId(cat, roomId);
        if (roomIdx < 0) return null;
        if (startDay < 1 || nights < 1) return null;

        // تأكد أن كل الليالي فاضية
        for (int i = 0; i < nights; i++) {
            int dIdx = (startDay - 1 + i) % 7;
            if (!availability[cat][roomIdx][dIdx]) return null;
        }

        // علّمها محجوزة
        for (int i = 0; i < nights; i++) {
            int dIdx = (startDay - 1 + i) % 7;
            availability[cat][roomIdx][dIdx] = false;
        }

        String resId = "R" + System.nanoTime();
        Reservation res = new Reservation(
                resId, username, new Room(roomId, category, roomIdx + 1),
                startDay, nights);

        String key = username.toLowerCase();
        byUser.computeIfAbsent(key, k -> new ArrayList<>()).add(res);
        saveReservation(res);
        return resId;
    }

    // CSV لكل حجوزات مستخدم:  resId|roomId@dayxNights, ...
    public synchronized String reservationsCsvFor(String username) {
        String key = username.toLowerCase();
        List<Reservation> rs = byUser.getOrDefault(key, Collections.emptyList());
        if (rs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Reservation r : rs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(r.getId())
              .append("|")
              .append(r.getRoom().getId())
              .append("@")
              .append(r.getDay())
              .append("x")
              .append(r.getNights());
        }
        return sb.toString();
    }

    // ---------- الإلغاء ----------
    public synchronized boolean cancelReservation(String username, String resId) {
        String key = username.toLowerCase();
        List<Reservation> list = byUser.get(key);
        if (list == null) return false;

        Iterator<Reservation> it = list.iterator();
        while (it.hasNext()) {
            Reservation r = it.next();
            if (!r.getId().equals(resId)) continue;

            int cat = catIndex(r.getRoom().getType());
            int roomIdx = roomIndexFromId(cat, r.getRoom().getId());
            if (roomIdx >= 0) {
                for (int i = 0; i < r.getNights(); i++) {
                    int dIdx = (r.getDay() - 1 + i) % 7;
                    availability[cat][roomIdx][dIdx] = true; // رجّعها متاحة
                }
            }

            it.remove();
            rewriteReservationsFile();
            return true;
        }
        return false;
    }

    // ---------- persistence ----------
    private void saveReservation(Reservation r) {
        r.saveToFile();
    }

    private void rewriteReservationsFile() {
        try (FileWriter fw = new FileWriter("reservations.txt", false)) {
            for (List<Reservation> list : byUser.values()) {
                for (Reservation r : list) {
                    fw.write(r.getId() + "," +
                             r.getUsername() + "," +
                             r.getRoom().getType() + "," +
                             r.getRoom().getNumber() + "," +
                             r.getDay() + "," +
                             r.getNights() + "\n");
                }
            }
        } catch (IOException ignored) {}
    }

    private void loadReservationsFromFile() {
        File f = new File("reservations.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                // id,username,type,number,day,nights
                String[] p = line.split(",", 6);
                if (p.length == 6) {
                    String id = p[0], user = p[1], type = p[2];
                    int number = Integer.parseInt(p[3]);
                    int day    = Integer.parseInt(p[4]);
                    int nights = Integer.parseInt(p[5]);

                    int cat = catIndex(type);
                    String roomId = ROOM_IDS[cat][number - 1];

                    // علّم الأيام محجوزة
                    for (int i = 0; i < nights; i++) {
                        int dIdx = (day - 1 + i) % 7;
                        availability[cat][number - 1][dIdx] = false;
                    }

                    Reservation r = new Reservation(id, user,
                            new Room(roomId, type, number), day, nights);
                    byUser.computeIfAbsent(user.toLowerCase(),
                            k -> new ArrayList<>()).add(r);
                }
            }
        } catch (Exception ignored) {}
    }
}
