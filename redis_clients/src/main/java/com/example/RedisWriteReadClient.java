package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisWriteReadClient {
    public static void main(String[] args) throws InterruptedException {

        Jedis master1 = new Jedis("localhost", 6379);
        // Jedis replica1 = new Jedis("localhost", 6380);
        // Jedis replica2 = new Jedis("localhost", 6381);

        // for (int i = 0; i < Integer.MAX_VALUE; i++) {
        // String key = "key" + i;
        // String value = "value" + i;

        // // Write to Redis
        // master1.set(key, value);
        // System.out.println("✅ Written: " + key + " = " + value);

        // Read from Redis
        // String readValue = replica1.get(key);
        // System.out.println("🔍 Read from replica1: " + key + " = " + readValue);
        // String readValue2 = replica2.get(key);
        // System.out.println("🔍 Read from replica2: " + key + " = " + readValue2);

        // Sleep for a short duration to simulate workload
        // TimeUnit.MILLISECONDS.sleep(0);

        // Use List data structure , insert 10000 elements
        for (int j = 0; j < 1000; j++) {
            master1.lpush("mylist", "value" + j);
        }

        // Read all elements from the list using LRANGE
        master1.lrange("mylist", 0, -1).forEach(item -> {
            System.out.println("🔍 List item: " + item);
        });

    }
}