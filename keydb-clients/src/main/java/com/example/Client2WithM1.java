package com.example;

import redis.clients.jedis.Jedis;

/**
 * Client2WithM1:
 * A lightweight Redis client that writes to Master-1, and falls back to
 * Master-2 if M1 is down.
 */
public class Client2WithM1 {

    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "Client2:test:";
        int i = 1;

        System.out.println("🔄 [Client2] Starting failover-aware writer & reader loop...");

        while (true) {
            Jedis jedis = null;
            String masterHost = "❓";
            int masterPort = -1;

            try {
                jedis = tryConnect("localhost", 6379); // 🌐 Try M1
                if (jedis != null) {
                    masterHost = "localhost";
                    masterPort = 6379;
                } else {
                    System.out.println("⚠️ [Client2] Master-1 down! Trying Master-2...");
                    jedis = tryConnect("localhost", 6380); // 🌐 Try M2
                    if (jedis != null) {
                        masterHost = "localhost";
                        masterPort = 6380;
                    }
                }

                if (jedis != null) {
                    String key = keyPrefix + i;
                    String value = "Client2_Val_" + i;

                    // Log master being used
                    System.out.println("🧭 [Client2] Writing to master at " + masterHost + ":" + masterPort);

                    // Write
                    jedis.set(key, value);
                    System.out.println("✍️ [Client2] ➕ Set 🔑 " + key + " = 📦 " + value);

                    // Read back
                    String fetched = jedis.get(key);
                    System.out.println("🔍 [Client2] 🔁 Read 🔑 " + key + " = 📦 " + fetched);

                    i++;
                } else {
                    System.out.println("❌ [Client2] 😭 Both Redis nodes unreachable!");
                }
            } catch (Exception e) {
                System.err.println("💥 [Client2] Unexpected error: " + e.getMessage());
            } finally {
                if (jedis != null)
                    jedis.close();
                Thread.sleep(1000);
            }
        }
    }

    private static Jedis tryConnect(String host, int port) {
        try {
            Jedis j = new Jedis(host, port);
            j.ping();
            System.out.println("🔌 [Client2] Connected to Redis at " + host + ":" + port);
            return j;
        } catch (Exception e) {
            System.out.println("⛔ [Client2] Connection failed: " + host + ":" + port);
            return null;
        }
    }
}
