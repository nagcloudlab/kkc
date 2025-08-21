package com.example;

import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;

public class RedisWriteReadClient {
    public static void main(String[] args) throws InterruptedException {

        Jedis redisMaster = new Jedis("localhost", 6379);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Redis client...");
            redisMaster.close();
            System.out.println("Redis client closed.");
        }));

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String key = "key" + i;
            String value = "value" + i;
            redisMaster.set(key, value);
            System.out.println("✅Set " + key + " to " + value);
            TimeUnit.MILLISECONDS.sleep(2); // Sleep to simulate some delay in writing
            String readValue = redisMaster.get(key);
            if (readValue != null) {
                System.out.println("🔍Read " + key + ": " + readValue);
            } else {
                System.out.println("❌Failed to read " + key);
            }
        }

    }
}