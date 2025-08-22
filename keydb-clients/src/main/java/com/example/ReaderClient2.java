package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.Set;

public class ReaderClient2 {

    public static void main(String[] args) throws InterruptedException {
        String replicaHost = "localhost";
        int replicaPort = 6381;

        Jedis jedis = new Jedis(replicaHost, replicaPort);
        System.out.println("📥 Connected to replica at " + replicaHost + ":" + replicaPort);

        Set<String> seenKeys = new HashSet<>();
        String matchPrefix = "Client3:test:*";
        redis.clients.jedis.params.ScanParams params = new ScanParams().match(matchPrefix).count(100);

        while (true) {
            String cursor = ScanParams.SCAN_POINTER_START;

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                for (String key : scanResult.getResult()) {
                    if (!seenKeys.contains(key)) {
                        String value = jedis.get(key);
                        System.out.println("🔎 [Replica Reader] 🔑 " + key + " = 📦 " + value);
                        seenKeys.add(key);
                    }
                }
                cursor = scanResult.getCursor();
            } while (!cursor.equals(redis.clients.jedis.params.ScanParams.SCAN_POINTER_START));

            Thread.sleep(1000); // Poll every second
        }
    }
}
