package com.example;

import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Client6LoadAwareWriterReader:
 * Routes to Redis master with lower active connection count.
 */
public class Client6LoadAwareWriterReader {

    private static final JedisPoolConfig poolConfig = buildPoolConfig();
    private static final JedisPool poolM1 = new JedisPool(poolConfig, "localhost", 6379);
    private static final JedisPool poolM2 = new JedisPool(poolConfig, "localhost", 6380);

    // Track usage manually
    private static final AtomicInteger m1UsageCount = new AtomicInteger(0);
    private static final AtomicInteger m2UsageCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "Client6:test:";
        int i = 1;

        System.out.println("🔁 [Client6] Starting request-count based Redis writer & reader...");

        while (true) {
            Jedis jedis = null;
            String key = keyPrefix + i;
            String value = "Val_" + i;

            JedisPool selectedPool;
            String selectedMaster;
            int selectedPort;

            // Use request count as routing logic
            if (m1UsageCount.get() <= m2UsageCount.get()) {
                selectedPool = poolM1;
                selectedMaster = "M1";
                selectedPort = 6379;
            } else {
                selectedPool = poolM2;
                selectedMaster = "M2";
                selectedPort = 6380;
            }

            try {
                jedis = tryFromPool(selectedPool);
                if (jedis == null) {
                    System.out.println("⚠️ [Client6] Selected " + selectedMaster + " unreachable! Trying fallback...");
                    selectedPool = selectedPool == poolM1 ? poolM2 : poolM1;
                    selectedMaster = selectedMaster.equals("M1") ? "M2" : "M1";
                    selectedPort = selectedPort == 6379 ? 6380 : 6379;
                    jedis = tryFromPool(selectedPool);
                }

                if (jedis != null) {
                    if (selectedMaster.equals("M1")) {
                        m1UsageCount.incrementAndGet();
                    } else {
                        m2UsageCount.incrementAndGet();
                    }

                    System.out.println("⚖️ [Client6] Routing to ➡️ " + selectedMaster + " (" + selectedPort + ")");
                    jedis.set(key, value);
                    System.out.println("✍️ [Client6] Set 🔑 " + key + " = 📦 " + value);

                    String fetched = jedis.get(key);
                    System.out.println("🔍 [Client6] Read 🔑 " + key + " = 📦 " + fetched);

                    logConnectionStats();
                    i++;
                } else {
                    System.out.println("❌ [Client6] 😢 Both Redis masters are unreachable!");
                }

            } catch (Exception e) {
                System.err.println("💥 [Client6] Error: " + e.getMessage());
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

    private static void logConnectionStats() {
        // Log usage counts and active connections for both pools
        // M1
        System.out.println(
                "📊 [Client6] Pool Usage ➤ M1: " + m1UsageCount.get() + " ops | M2: " + m2UsageCount.get() + " ops");
        System.out.println("🔗 [Client6] Active Connections ➤ M1: " + poolM1.getNumActive() +
                " | M2: " + poolM2.getNumActive());
        System.out.println("🛌 [Client6] Idle Connections ➤ M1 : " + poolM1.getNumIdle() +
                " | M2: " + poolM2.getNumIdle());
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
