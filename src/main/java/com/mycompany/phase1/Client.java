
package com.mycompany.phase1;

import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server");
    }

    public String register(String u, String p) throws IOException {
        out.println("REGISTER " + u + " " + p);
        return in.readLine();
    }

    public String show(String type) throws IOException {
        out.println("SHOW " + type);
        return in.readLine();
    }

    public String book(String username, String type, int number) throws IOException {
        out.println("BOOK " + username + " " + type + " " + number);
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }
}
