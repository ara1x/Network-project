/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.phase1;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class NewClient implements Runnable {
    private final Socket client;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<NewClient> clients;
    private final ServerState state;

    public NewClient(Socket socket, ArrayList<NewClient> clients, ServerState state) throws IOException {
        this.client = socket;
        this.clients = clients;
        this.state = state;
        this.in  = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
        this.out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] t = line.split("\\s+");
                String cmd = t[0].toUpperCase();

                switch (cmd) {
                    case "PING":
                        out.println("OK PONG");
                        break;

                    case "REGISTER": // REGISTER username password
                        if (t.length < 3) { out.println("ERR usage: REGISTER username password"); break; }
                        out.println(state.register(t[1], t[2]) ? "OK REGISTERED" : "ERR USER_EXISTS");
                        break;

                    case "LOGIN": // LOGIN username password
                        if (t.length < 3) { out.println("ERR usage: LOGIN username password"); break; }
                        if (state.login(t[1], t[2])) out.println("OK LOGIN");
                        else if (state.userExists(t[1])) out.println("ERR BAD_CREDENTIALS");
                        else out.println("ERR NO_SUCH_USER");
                        break;

                    // LIST_AVAIL <CATEGORY> <STARTDAY 1..7> <NIGHTS 1..7>
                    case "LIST_AVAIL":
                        if (t.length < 4) { out.println("ERR usage: LIST_AVAIL CAT START NIGHTS"); break; }
                        String cat = t[1];
                        int start = Integer.parseInt(t[2]);
                        int nights = Integer.parseInt(t[3]);
                        String csv = state.listAvailableRoomsCsv(cat, start, nights); // e.g., "S1,S3,S5"
                        out.println("OK ROOMS " + csv);
                        break;

                    // BOOK2 username category roomId start nights
                    case "BOOK2":
                        if (t.length < 6) { out.println("ERR usage: BOOK2 user cat room start nights"); break; }
                        String user = t[1], category = t[2], roomId = t[3];
                        int s = Integer.parseInt(t[4]), n = Integer.parseInt(t[5]);
                        String resId = state.reserve(user, category, roomId, s, n);
                        out.println(resId != null ? ("OK CONFIRMED " + resId) : "ERR NO_AVAIL");
                        break;

                    // MY_RES username
                    case "MY_RES":
                        if (t.length < 2) { out.println("ERR usage: MY_RES username"); break; }
                        out.println("OK RES " + state.reservationsCsvFor(t[1])); // "S1@3x2,P2@1x1"
                        break;

                    // kept old SHOW/BOOK for compatibility (optional)
                    case "SHOW":
                        if (t.length < 2) { out.println("ERR usage: SHOW type"); break; }
                        out.println("OK ROOMS " + state.listAvailableRoomsCsv(t[1], 1, 1));
                        break;

                    case "BOOK": // old style (not used by GUI now)
                        out.println("ERR USE_BOOK2");
                        break;

                    default:
                        out.println("ERR UNKNOWN_COMMAND");
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}
