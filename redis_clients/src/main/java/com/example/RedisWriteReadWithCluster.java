package com.example;

import redis.clients.jedis.*;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Set;

public class RedisWriteReadWithCluster {
    public static void main(String[] args) {

        Set<HostAndPort> clusterNodes = Set.of(
                new HostAndPort("localhost", 7000),
                new HostAndPort("localhost", 7001),
                new HostAndPort("localhost", 7002));

        // Optional pool tuning
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(64);
        poolConfig.setMaxIdle(32);
        poolConfig.setMinIdle(8);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);

        try (JedisCluster jedis = new JedisCluster(
                clusterNodes,
                poolConfig)) {
            // Simple ops
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                String key = "key" + i;
                String value = "value" + i;
                jedis.set(key, value);
                String retrievedValue = jedis.get(key);
                System.out.println("Set " + key + " to " + value + ", retrieved: " + retrievedValue);
            }
        }
    }
}
