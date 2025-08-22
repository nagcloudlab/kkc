package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Client1WithM1AndM2:
 * A Redis client that uses connection pooling to write to Master-1 first,
 * and falls back to Master-2 if M1 is down.
 */
public class Client1WithM1AndM2 {

    private static final JedisPoolConfig poolConfig = buildPoolConfig();

    private static final JedisPool poolM1 = new JedisPool(poolConfig, "localhost", 6379);

    private static final JedisPool poolM2 = new JedisPool(poolConfig, "localhost", 6380);

    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "Client1:test:";
        int i = 1;

        System.out.println("🔁 [Client1] Starting failover-aware POOL-based writer & reader...");

        while (true) {
            Jedis jedis = null;
            String currentMaster = "❓";
            int currentPort = -1;

            try {
                jedis = tryFromPool(poolM1);
                if (jedis != null) {
                    currentMaster = "M1";
                    currentPort = 6379;
                } else {
                    System.out.println("⚠️ [Client1] M1 unreachable! Trying M2...");
                    jedis = tryFromPool(poolM2);
                    if (jedis != null) {
                        currentMaster = "M2";
                        currentPort = 6380;
                    }
                }

                if (jedis != null) {
                    String key = keyPrefix + i;
                    String value = "Client1_Val_" + i;

                    System.out.println("🧭 [Client1] Writing to ➡️ " + currentMaster + " (" + currentPort + ")");
                    jedis.set(key, value);
                    System.out.println("✍️ [Client1] ➕ Set 🔑 " + key + " = 📦 " + value);

                    String fetched = jedis.get(key);
                    System.out.println("🔍 [Client1] 🔁 Read 🔑 " + key + " = 📦 " + fetched);

                    logConnectionStats(currentMaster);

                    i++;
                } else {
                    System.out.println("❌ [Client1] 😢 Both Redis masters unreachable!");
                }

            } catch (Exception e) {
                System.err.println("💥 [Client1] Unexpected error: " + e.getMessage());
            } finally {
                if (jedis != null) {
                    jedis.close(); // Return to pool
                }
                Thread.sleep(1000);
            }
        }
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
        config.setMaxTotal(10); // Max 10 total connections
        config.setMaxIdle(5); // Max 5 idle connections
        config.setMinIdle(1); // Min 1 idle connection
        config.setTestOnBorrow(true); // Validate before using
        config.setTestOnReturn(true); // Validate after using
        config.setBlockWhenExhausted(true);
        return config;
    }

    private static void logConnectionStats(String masterName) {
        JedisPool pool = masterName.equals("M1") ? poolM1 : poolM2;
        System.out.println("📊 [Client1] Pool Stats for " + masterName + " - Active: "
                + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
    }
}
