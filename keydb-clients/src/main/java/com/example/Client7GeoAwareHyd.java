package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * ClientGeoAwareHyd:
 * A geo-aware Redis client located in Hyderabad DC.
 * Prefers M2 (Hyderabad) for performance, falls back to M1 (Chennai).
 */
public class Client7GeoAwareHyd {

    private static final JedisPoolConfig poolConfig = buildPoolConfig();

    // Assume M2 = Hyderabad, M1 = Chennai
    private static final JedisPool poolM2 = new JedisPool(poolConfig, "localhost", 6380); // Hyderabad
    private static final JedisPool poolM1 = new JedisPool(poolConfig, "localhost", 6379); // Chennai

    public static void main(String[] args) throws InterruptedException {
        String keyPrefix = "GeoHyd:test:";
        int i = 1;

        System.out.println("🌐 [GeoClient] HYDERABAD DC – Prefer M2 (local) ➡️ fallback to M1 (remote)...");

        while (true) {
            Jedis jedis = null;
            String currentMaster = null;
            int currentPort = -1;

            try {
                // Prefer local DC first: M2
                jedis = tryFromPool(poolM2);
                if (jedis != null) {
                    currentMaster = "M2 (Hyderabad)";
                    currentPort = 6380;
                } else {
                    System.out.println("⚠️ [GeoClient] M2 unavailable, trying M1 (Chennai)...");
                    jedis = tryFromPool(poolM1);
                    if (jedis != null) {
                        currentMaster = "M1 (Chennai)";
                        currentPort = 6379;
                    }
                }

                if (jedis != null) {
                    String key = keyPrefix + i;
                    String value = "GeoHyd_Val_" + i;

                    System.out.println("✍️ [GeoClient] Writing to ➡️ " + currentMaster + " (" + currentPort + ")");
                    jedis.set(key, value);
                    System.out.println("✅ [GeoClient] SET " + key + " = " + value);

                    String fetched = jedis.get(key);
                    System.out.println("🔍 [GeoClient] GET " + key + " = " + fetched);

                    logConnectionStats(currentMaster.contains("M2") ? "M2" : "M1");
                    i++;
                } else {
                    System.out.println("❌ [GeoClient] Both Redis masters unreachable from Hyderabad DC!");
                }

            } catch (Exception e) {
                System.err.println("💥 [GeoClient] Unexpected error: " + e.getMessage());
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
                Thread.sleep(1000);
            }
        }
    }

    private static Jedis tryFromPool(JedisPool pool) {
        try {
            Jedis jedis = pool.getResource();
            jedis.ping(); // Test connection
            return jedis;
        } catch (Exception e) {
            return null;
        }
    }

    private static void logConnectionStats(String masterName) {
        JedisPool pool = masterName.equals("M2") ? poolM2 : poolM1;
        System.out.println("📊 [GeoClient] Pool Stats for " + masterName +
                " ➡️ Active: " + pool.getNumActive() +
                ", Idle: " + pool.getNumIdle() +
                ", Waiting: " + pool.getNumWaiters());
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
