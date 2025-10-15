
package com.mycompany.phase1;

import java.io.*;
import java.util.*;

public class ServerState {
    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<>();

    public ServerState() {
        initRooms();
        loadUsersFromFile();
    }

    // إنشاء الغرف (3 أنواع × 5 غرف)
    private void initRooms() {
        String[] types = {"STANDARD", "PREMIUM", "SUITE"};
        for (String type : types) {
            for (int i = 1; i <= 5; i++) {
                String id = type.substring(0, 3) + "-" + i;
                rooms.add(new Room(id, type, i));
            }
        }
    }

    // تحميل المستخدمين من ملف
    private void loadUsersFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.add(new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            // ignore if first time
        }
    }

    // تسجيل مستخدم جديد
    public synchronized boolean register(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return false; // موجود
            }
        }

        users.add(new User(username, password));

        try (FileWriter fw = new FileWriter("users.txt", true)) {
            fw.write(username + "," + password + "\n");
        } catch (IOException e) {
            System.out.println("Error saving user: " + e.getMessage());
        }

        return true;
    }

    // عرض الغرف المتاحة لنوع معين
    public synchronized ArrayList<Room> showAvailableRooms(String type) {
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getType().equalsIgnoreCase(type)) {
                result.add(r);
            }
        }
        return result;
    }

    // تنفيذ حجز وتخزينه في ملف
    public synchronized boolean makeReservation(String username, String type, int roomNumber) {
        for (Room r : rooms) {
            if (r.getType().equalsIgnoreCase(type) && r.getNumber() == roomNumber) {
                Reservation res = new Reservation(UUID.randomUUID().toString(), username, r, 1, 1);
                res.saveToFile();
                return true;
            }
        }
        return false;
    }
}

