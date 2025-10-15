/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.phase1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NewServer {
    private static ArrayList<NewClient> clients = new ArrayList<>();
    private static ServerState state = new ServerState();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("Server running on port 9090");

        while (true) {
            Socket client = serverSocket.accept();
            NewClient clientThread = new NewClient(client, clients, state);
            clients.add(clientThread);
            new Thread(clientThread).start();
        }
    }
}
