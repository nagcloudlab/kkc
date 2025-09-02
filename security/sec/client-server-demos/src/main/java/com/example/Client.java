package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {

        Socket socket = null;
        try {
            socket = new Socket("localhost", 12345);
            System.out.println("Connected to server at " + socket.getRemoteSocketAddress());

            BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(socket.getOutputStream()));
            writer.write("Hello, Server!\n");
            writer.flush();

            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            String response = reader.readLine();
            System.out.println("Received from server: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
