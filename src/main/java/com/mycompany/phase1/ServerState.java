package com.mycompany.phase1;

import java.io.*;
import java.util.*;


public class ServerState {

    private final List<User> users = new ArrayList<>();
    private final Map<String, String> creds = new HashMap<>(); 
    private final Map<String, List<Reservation>> byUser = new HashMap<>();

   
    private final boolean[][][] availability = new boolean[3][5][7]; 

    private static final String[] CATS = {"STANDARD","PREMIUM","SUITE"};
    private static final String[][] ROOM_IDS = {
            {"S1","S2","S3","S4","S5"},
            {"P1","P2","P3","P4","P5"},
            {"U1","U2","U3","U4","U5"}
    };

    public ServerState() {
        
        for (int c = 0; c < 3; c++) for (int r = 0; r < 5; r++) Arrays.fill(availability[c][r], true);
        loadUsersFromFile();
        loadReservationsFromFile(); 
    }

    
    private void loadUsersFromFile() {
        File f = new File("users.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    users.add(new User(parts[0], parts[1]));
                    creds.put(parts[0], parts[1]);
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

   
    private int catIndex(String cat) {
        String x = cat.toUpperCase();
        if (x.equals("STANDARD")) return 0;
        if (x.equals("PREMIUM"))  return 1;
        if (x.equals("SUITE"))    return 2;
        return 0;
    }

    private int roomIndexFromId(int cat, String roomId) {
        for (int i = 0; i < 5; i++) if (ROOM_IDS[cat][i].equalsIgnoreCase(roomId)) return i;
        return -1;
    }

public synchronized String listAvailableRoomsCsv(String category, int startDay, int nights) {
    int cat = catIndex(category);
    if (startDay < 1 || nights < 1) return "";

    StringBuilder sb = new StringBuilder();
    for (int r = 0; r < 5; r++) {
        boolean ok = true;
      
        for (int i = 0; i < nights; i++) {
            int dIdx = ((startDay - 1 + i) % 7); 
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

public synchronized String reserve(String username, String category, String roomId, int startDay, int nights) {
    int cat = catIndex(category);
    int roomIdx = roomIndexFromId(cat, roomId);
    if (roomIdx < 0) return null;
    if (startDay < 1 || nights < 1) return null;

    
    for (int i = 0; i < nights; i++) {
        int dIdx = ((startDay - 1 + i) % 7);
        if (!availability[cat][roomIdx][dIdx]) return null;
    }

   
    for (int i = 0; i < nights; i++) {
        int dIdx = ((startDay - 1 + i) % 7);
        availability[cat][roomIdx][dIdx] = false;
    }

    String resId = "R" + System.nanoTime();
    Reservation res = new Reservation(resId, username, new Room(roomId, category, roomIdx + 1), startDay, nights);
    byUser.computeIfAbsent(username.toLowerCase(), k -> new ArrayList<>()).add(res);
    saveReservation(res);
    return resId;
}


  
    public synchronized String reservationsCsvFor(String username) {
        List<Reservation> rs = byUser.getOrDefault(username.toLowerCase(), Collections.emptyList());
        if (rs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Reservation r : rs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(r.getRoom().getId()).append("@").append(r.getDay()).append("x").append(r.getNights());
        }
        return sb.toString();
    }

    private void saveReservation(Reservation r) {
        r.saveToFile();
    }

    private void loadReservationsFromFile() {
        File f = new File("reservations.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
               
                String[] p = line.split(",", 6);
                if (p.length == 6) {
                    String id = p[0], user = p[1], type = p[2];
                    int number = Integer.parseInt(p[3]);
                    int day    = Integer.parseInt(p[4]);
                    int nights = Integer.parseInt(p[5]);
                    int cat = catIndex(type);
                    String roomId = ROOM_IDS[cat][number-1];
                 
                    for (int d = day; d <= day + nights - 1; d++) availability[cat][number-1][d-1] = false;

                    Reservation r = new Reservation(id, user, new Room(roomId, type, number), day, nights);
                    byUser.computeIfAbsent(user.toLowerCase(), k -> new ArrayList<>()).add(r);
                }
            }
        } catch (Exception ignored) {}
    }
}
