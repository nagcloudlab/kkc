package com.example;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;

public class RedisWriteReadClientWithSentinels {
    public static void main(String[] args) throws InterruptedException {

        // 1. Sentinel Config
        Set<String> sentinels = new HashSet<>();
        sentinels.add("localhost:5000");
        sentinels.add("localhost:5001");
        sentinels.add("localhost:5002");

        // 2. Create Sentinel Pool (monitored master name = "mymaster")
        JedisSentinelPool sentinelPool = new JedisSentinelPool("mymaster", sentinels);

        // 3. Loop to continuously write to master
        int counter = 1;
        while (true) {
            try (Jedis jedis = sentinelPool.getResource()) {
                HostAndPort currentHostMaster = sentinelPool.getCurrentHostMaster();
                System.out.println(
                        "✅ Connected to master: " + currentHostMaster.getHost() + ":" + currentHostMaster.getPort());
                String key = "key" + counter;
                String value = "value" + counter;
                jedis.set(key, value);
                jedis.waitReplicas(2, 100); // wait for replicas to catch up
                System.out.println("📝 Written: " + key + " = " + value);

                // String read = jedis.get(key);
                // System.out.println("🔁 Read-back: " + key + " = " + read);

                counter++;
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("❌ Write failed: " + e.getMessage());
                Thread.sleep(2000); // wait before retry
            }
        }
    }
}
