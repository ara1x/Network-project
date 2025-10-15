/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.phase1;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class NewClient implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<NewClient> clients;
    private ServerState state;

    public NewClient(Socket socket, ArrayList<NewClient> clients, ServerState state) throws IOException {
        this.client = socket;
        this.clients = clients;
        this.state = state;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] t = line.trim().split("\\s+");
                if (t.length == 0) continue;

                String cmd = t[0].toUpperCase();

                switch (cmd) {
                    case "REGISTER":
                        if (t.length < 3) {
                            out.println("ERR usage: REGISTER username password");
                            break;
                        }
                        boolean ok = state.register(t[1], t[2]);
                        out.println(ok ? "OK REGISTERED" : "ERR USER_EXISTS");
                        break;

                    case "SHOW":
                        if (t.length < 2) {
                            out.println("ERR usage: SHOW type");
                            break;
                        }
                        ArrayList<Room> list = state.showAvailableRooms(t[1]);
                        if (list.isEmpty()) out.println("No rooms found for that type");
                        else {
                            StringBuilder sb = new StringBuilder("Available rooms: ");
                            for (Room r : list) sb.append(r.getId()).append(" ");
                            out.println(sb.toString());
                        }
                        break;

                    case "BOOK":
                        if (t.length < 4) {
                            out.println("ERR usage: BOOK username type number");
                            break;
                        }
                        boolean booked = state.makeReservation(t[1], t[2], Integer.parseInt(t[3]));
                        out.println(booked ? "OK BOOKED" : "ERR ROOM_NOT_FOUND");
                        break;

                    default:
                        out.println("ERR UNKNOWN_COMMAND");
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }
}
