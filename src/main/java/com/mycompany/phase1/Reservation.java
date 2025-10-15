
package com.mycompany.phase1;

import java.io.FileWriter;
import java.io.IOException;

public class Reservation {
    private String id;
    private String username;
    private Room room;
    private int day;
    private int nights;

    public Reservation(String id, String username, Room room, int day, int nights) {
        this.id = id;
        this.username = username;
        this.room = room;
        this.day = day;
        this.nights = nights;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public Room getRoom() { return room; }
    public int getDay() { return day; }
    public int getNights() { return nights; }

    public void saveToFile() {
        try (FileWriter fw = new FileWriter("reservations.txt", true)) {
            fw.write(id + "," + username + "," + room.getType() + "," + room.getNumber() + "," + day + "," + nights + "\n");
        } catch (IOException e) {
            System.out.println("Error saving reservation: " + e.getMessage());
        }
    }
}

