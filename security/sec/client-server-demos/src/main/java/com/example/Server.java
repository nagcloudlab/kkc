package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(12345);
            System.out.println("Server is listening on port 12345");
            // Server logic would go here
            Socket socket = serverSocket.accept();
            System.out.println("Client connected: " + socket.getInetAddress());

            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            String message = br.readLine();
            System.out.println("Received from client: " + message);

            BufferedWriter bw = new BufferedWriter(new java.io.OutputStreamWriter(socket.getOutputStream()));
            bw.write("Hello from server\n");
            bw.flush();

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}