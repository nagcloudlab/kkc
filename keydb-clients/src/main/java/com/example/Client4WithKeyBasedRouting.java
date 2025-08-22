package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Client4WithKeyBasedRouting:
 * Routes writes to Redis Master based on first character of key.
 * A-M ➡️ M1 (6379), N-Z ➡️ M2 (6380)
 */
public class Client4WithKeyBasedRouting {

    private static final JedisPoolConfig poolConfig = buildPoolConfig();
    private static final JedisPool poolM1 = new JedisPool(poolConfig, "localhost", 6379);
    private static final JedisPool poolM2 = new JedisPool(poolConfig, "localhost", 6380);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🧭 [Client4] Starting key-based routing client (A–M ➡️ M1, N–Z ➡️ M2)...");

        int i = 1;
        while (true) {
            // Simulate different starting characters from A-Z
            char ch = (char) ('A' + (i - 1) % 26);
            String key = "Client4:test:" + ch + "_Key_" + i;
            String value = "Val_" + i;

            char routeChar = Character.toUpperCase(ch);
            String currentMaster = "❓";
            int currentPort = -1;

            JedisPool selectedPool = isAM(routeChar) ? poolM1 : poolM2;
            String preferredMaster = isAM(routeChar) ? "M1" : "M2";
            int preferredPort = isAM(routeChar) ? 6379 : 6380;

            Jedis jedis = null;

            try {
                jedis = tryFromPool(selectedPool);
                if (jedis != null) {
                    currentMaster = preferredMaster;
                    currentPort = preferredPort;
                } else {
                    // Fallback
                    System.out.println("⚠️ [Client4] " + preferredMaster + " unavailable, trying fallback...");
                    selectedPool = (selectedPool == poolM1) ? poolM2 : poolM1;
                    preferredMaster = (preferredMaster.equals("M1")) ? "M2" : "M1";
                    preferredPort = (preferredPort == 6379) ? 6380 : 6379;

                    jedis = tryFromPool(selectedPool);
                    if (jedis != null) {
                        currentMaster = preferredMaster;
                        currentPort = preferredPort;
                    }
                }

                if (jedis != null) {
                    System.out.println(
                            "📝 [Client4] Routing 🔑 " + key + " ➡️ " + currentMaster + " (" + currentPort + ")");
                    jedis.set(key, value);
                    System.out.println("✅ [Client4] Set 🔑 " + key + " = 📦 " + value);

                    String readBack = jedis.get(key);
                    System.out.println("🔍 [Client4] Verified Read 🔑 " + key + " = 📦 " + readBack);

                    logConnectionStats(currentMaster);
                } else {
                    System.out.println("❌ [Client4] Both Redis masters unreachable!");
                }

                i++;
                Thread.sleep(1000);

            } catch (Exception e) {
                System.err.println("💥 [Client4] Unexpected error: " + e.getMessage());
            } finally {
                if (jedis != null) {
                    jedis.close(); // Return to pool
                }
            }
        }
    }

    private static boolean isAM(char c) {
        return c >= 'A' && c <= 'M';
    }

    private static Jedis tryFromPool(JedisPool pool) {
        try {
            Jedis jedis = pool.getResource();
            jedis.ping(); // Validate connection
            return jedis;
        } catch (Exception e) {
            return null;
        }
    }

    private static JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setBlockWhenExhausted(true);
        return config;
    }

    private static void logConnectionStats(String masterName) {
        JedisPool pool = masterName.equals("M1") ? poolM1 : poolM2;
        System.out.println("📊 [Client4] Pool Stats for " + masterName + " ➤ Active: "
                + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
    }
}
