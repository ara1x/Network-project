package com.mycompany.phase1;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in  = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        System.out.println("Connected to server");
    }

    public void close() throws IOException {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in  != null) in.close();  } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; in = null; out = null;
    }

    private String rpc(String line) throws IOException {
        if (out == null || in == null) throw new IOException("Not connected to server");
        out.println(line);
        String resp = in.readLine();
        if (resp == null) throw new IOException("Server closed connection");
        return resp.trim();
    }

    // ---- existing (used by GUI) ----
    public String register(String u, String p) throws IOException {
        return rpc("REGISTER " + u + " " + p);
    }

    public String login(String u, String p) throws IOException {
        return rpc("LOGIN " + u + " " + p);
    }

    public String listAvail(String category, int start, int nights) throws IOException {
        return rpc("LIST_AVAIL " + category + " " + start + " " + nights);
    }

    public String bookRoom(String username, String category,
                           String roomId, int start, int nights) throws IOException {
        return rpc("BOOK2 " + username + " " + category + " " + roomId + " " + start + " " + nights);
    }

    public String myReservations(String username) throws IOException {
        return rpc("MY_RES " + username);
    }

    // NEW: cancel reservation
    public String cancelReservation(String username, String resId) throws IOException {
        return rpc("CANCEL " + username + " " + resId);
    }

    public void ping() throws IOException {
        String r = rpc("PING");
        if (!r.startsWith("OK PONG")) throw new IOException("Bad PING response: " + r);
    }
}
