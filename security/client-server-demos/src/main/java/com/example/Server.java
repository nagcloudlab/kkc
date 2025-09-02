package com.example;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("Server is running...");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected!");
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String message = in.readLine();
        System.out.println("Client says: " + message);
        out.println("Message received by the server.");

        clientSocket.close();
        serverSocket.close();
    }
}
