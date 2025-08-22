package com.example;

import redis.clients.jedis.Jedis;

/**
 * Client3WithM2:
 * A Redis client that prefers Master-2 first (port 6380), falls back to
 * Master-1 (port 6379).
 */
public class Client3WithM2 {
    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "Client3:test:";
        int i = 1;

        System.out.println("🔄 [Client3] Starting failover-aware writer & reader loop...");

        while (true) {
            Jedis jedis = null;
            String masterHost = "❓";
            int masterPort = -1;

            try {
                jedis = tryConnect("localhost", 6380); // 🌐 Try M2
                if (jedis != null) {
                    masterHost = "localhost";
                    masterPort = 6380;
                } else {
                    System.out.println("⚠️ [Client3] Master-2 down! Trying Master-1...");
                    jedis = tryConnect("localhost", 6379); // 🌐 Try M1
                    if (jedis != null) {
                        masterHost = "localhost";
                        masterPort = 6379;
                    }
                }

                if (jedis != null) {
                    String key = keyPrefix + i;
                    String value = "Client1_Val_" + i;

                    // Log master being used
                    System.out.println("🧭 [Client3] Writing to master at " + masterHost + ":" + masterPort);

                    // Write
                    jedis.set(key, value);
                    System.out.println("✍️ [Client3] ➕ Set 🔑 " + key + " = 📦 " + value);

                    // Read back
                    String fetched = jedis.get(key);
                    System.out.println("🔍 [Client3] 🔁 Read 🔑 " + key + " = 📦 " + fetched);

                    i++;
                } else {
                    System.out.println("❌ [Client3] 😭 Both Redis nodes unreachable!");
                }
            } catch (Exception e) {
                System.err.println("💥 [Client3] Unexpected error: " + e.getMessage());
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
            System.out.println("🔌 [Client3] Connected to Redis at " + host + ":" + port);
            return j;
        } catch (Exception e) {
            System.out.println("⛔ [Client3] Connection failed: " + host + ":" + port);
            return null;
        }
    }
}
