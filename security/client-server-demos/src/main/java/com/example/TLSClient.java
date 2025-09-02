package com.example;

import javax.net.ssl.*;
import java.io.*;

public class TLSClient {
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", "/Users/nag/kkc/security/serverkeystore.p12");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("Hello securely!");
        String response = in.readLine();
        System.out.println("Server says: " + response);

        socket.close();
    }
}
