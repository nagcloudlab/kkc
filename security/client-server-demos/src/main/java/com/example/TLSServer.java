package com.example;

import javax.net.ssl.*;
import java.io.*;

public class TLSServer {
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.keyStore", "/Users/nag/kkc/security/serverkeystore.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(8443);
        System.out.println("TLS Server running...");
        SSLSocket socket = (SSLSocket) serverSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String message = in.readLine();
        System.out.println("Client says: " + message);
        out.println("Message received securely!");

        socket.close();
        serverSocket.close();
    }
}
