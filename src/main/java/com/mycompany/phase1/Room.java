
package com.mycompany.phase1;

public class Room {
    private String id;
    private String type;   // STANDARD, PREMIUM, SUITE
    private int number;

    public Room(String id, String type, int number) {
        this.id = id;
        this.type = type;
        this.number = number;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public int getNumber() { return number; }
}

