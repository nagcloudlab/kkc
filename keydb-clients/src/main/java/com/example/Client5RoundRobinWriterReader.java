package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Client5RoundRobinWriterReader:
 * Alternates Redis writes between M1 and M2 using round-robin.
 */
public class Client5RoundRobinWriterReader {

    private static final JedisPoolConfig poolConfig = buildPoolConfig();
    private static final JedisPool poolM1 = new JedisPool(poolConfig, "localhost", 6379);
    private static final JedisPool poolM2 = new JedisPool(poolConfig, "localhost", 6380);

    private static boolean useM1 = true; // Round-robin flag

    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "Client5:test:";
        int i = 1;

        System.out.println("🔁 [Client5] Starting round-robin writer & reader...");

        while (true) {
            Jedis jedis = null;
            String key = keyPrefix + i;
            String value = "Val_" + i;

            JedisPool selectedPool = useM1 ? poolM1 : poolM2;
            String preferredMaster = useM1 ? "M1" : "M2";
            int preferredPort = useM1 ? 6379 : 6380;

            // Toggle for next operation
            useM1 = !useM1;

            try {
                jedis = tryFromPool(selectedPool);
                if (jedis == null) {
                    System.out.println("⚠️ [Client5] " + preferredMaster + " unreachable! Trying fallback...");

                    // Fallback to the other pool
                    selectedPool = (selectedPool == poolM1) ? poolM2 : poolM1;
                    preferredMaster = preferredMaster.equals("M1") ? "M2" : "M1";
                    preferredPort = preferredPort == 6379 ? 6380 : 6379;

                    jedis = tryFromPool(selectedPool);
                }

                if (jedis != null) {
                    System.out.println("🔁 [Client5] Using ➡️ " + preferredMaster + " (" + preferredPort + ")");
                    jedis.set(key, value);
                    System.out.println("✍️ [Client5] Set 🔑 " + key + " = 📦 " + value);

                    String fetched = jedis.get(key);
                    System.out.println("🔍 [Client5] Read 🔑 " + key + " = 📦 " + fetched);

                    logConnectionStats(preferredMaster);
                    i++;
                } else {
                    System.out.println("❌ [Client5] 😢 Both Redis masters unreachable!");
                }

            } catch (Exception e) {
                System.err.println("💥 [Client5] Error: " + e.getMessage());
            } finally {
                if (jedis != null)
                    jedis.close();
                Thread.sleep(1000);
            }
        }
    }

    private static Jedis tryFromPool(JedisPool pool) {
        try {
            Jedis jedis = pool.getResource();
            jedis.ping();
            return jedis;
        } catch (Exception e) {
            return null;
        }
    }

    private static void logConnectionStats(String masterName) {
        JedisPool pool = masterName.equals("M1") ? poolM1 : poolM2;
        System.out.println("📊 [Client5] Pool Stats " + masterName + " ➤ Active: "
                + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
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
}
