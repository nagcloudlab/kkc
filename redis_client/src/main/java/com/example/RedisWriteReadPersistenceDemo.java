package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * RedisWriteReadPersistenceDemo
 *
 * Usage examples:
 * 1) Continuous write+read with live persistence metrics every 5s:
 * java com.example.RedisWriteReadPersistenceDemo --host 127.0.0.1 --port 6379
 * --mode run --sleepMs 2 --reportSec 5 --prefix demo:key --progressFile
 * progress.txt
 *
 * 2) After a crash+restart of Redis, audit what survived vs last locally saved
 * watermark:
 * java com.example.RedisWriteReadPersistenceDemo --host 127.0.0.1 --port 6379
 * --mode audit --prefix demo:key --progressFile progress.txt
 *
 * TIP: Configure Redis differently between runs to showcase RDB vs AOF:
 * - RDB only (redis.conf): save 10 1; appendonly no
 * - AOF (everysec): appendonly yes; appendfsync everysec
 * - AOF (always): appendonly yes; appendfsync always
 */
public class RedisWriteReadPersistenceDemo {

    // ----------------------------
    // CLI options with sane defaults
    // ----------------------------
    static class Options {
        String host = "127.0.0.1";
        int port = 6379;
        String mode = "run"; // run | audit
        String prefix = "demo:key";
        long sleepMs = 2;
        int reportSec = 5;
        String progressFile = "progress.txt";
        long maxKeys = Long.MAX_VALUE; // optional upper bound for quick demos
    }

    public static void main(String[] args) {
        Options opt = parseArgs(args);
        if (!opt.mode.equals("run") && !opt.mode.equals("audit")) {
            System.err.println("Invalid --mode. Use 'run' or 'audit'.");
            System.exit(2);
        }

        if ("run".equals(opt.mode)) {
            runMode(opt);
        } else {
            auditMode(opt);
        }
    }

    // ----------------------------
    // RUN MODE: write+read with live metrics, plus local progress file
    // ----------------------------
    private static void runMode(Options opt) {
        final AtomicLong lastWritten = new AtomicLong(-1);
        final AtomicLong lastAck = new AtomicLong(-1);

        // Reporter thread has its *own* Jedis handle
        Thread reporter = new Thread(() -> metricsReporterLoop(opt, lastAck), "metrics-reporter");
        reporter.setDaemon(true);
        reporter.start();

        Jedis redis = null;
        try {
            // Graceful shutdown: persist progress and close connection
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down demo client...");
                persistProgress(opt.progressFile, lastAck.get());
                System.out.println("Client closed.");
            }));

            long i = 0;
            while (i < opt.maxKeys) {
                try {
                    redis = ensureConn(redis, opt.host, opt.port);

                    String key = opt.prefix + ":" + i;
                    String value = "value" + i;

                    // Write
                    redis.set(key, value);
                    lastWritten.set(i);

                    // Read back to "ack"
                    String readValue = redis.get(key);
                    if (value.equals(readValue)) {
                        lastAck.set(i);
                        if (i % 1000 == 0) {
                            // Persist every 1000 acks (tune as you like)
                            persistProgress(opt.progressFile, lastAck.get());
                        }
                        System.out.println("✅ Set+Read " + key);
                    } else {
                        System.out.println("❌ Mismatch on " + key + " (read=" + readValue + ")");
                    }

                    // tiny delay to simulate interval
                    TimeUnit.MILLISECONDS.sleep(opt.sleepMs);
                    i++;

                } catch (JedisConnectionException jce) {
                    System.err.println("🔌 Redis connection lost: " + jce.getMessage() + " — retrying...");
                    sleepSilently(500);
                    // will reconnect in next loop via ensureConn
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Unexpected error: " + e.getMessage());
                    sleepSilently(200);
                }
            }
        } finally {
            // final persist on exit path
            persistProgress(opt.progressFile, lastAck.get());
            if (redis != null) {
                try {
                    redis.close();
                } catch (Exception ignore) {
                }
            }
        }

        System.out.println("Done. LastAck=" + lastAck.get());
    }

    // Reporter loop (separate Jedis, no try-with-resources)
    private static void metricsReporterLoop(Options opt, AtomicLong lastAck) {
        Jedis j = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    j = ensureConn(j, opt.host, opt.port);
                    printMetrics(j, opt.prefix, lastAck.get());
                } catch (Exception e) {
                    System.err.println("⚠️ metrics error: " + e.getMessage());
                } finally {
                    sleepSilently(opt.reportSec * 1000L);
                }
            }
        } finally {
            if (j != null) {
                try {
                    j.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // ----------------------------
    // AUDIT MODE: compare last local watermark vs keys currently in Redis
    // ----------------------------
    private static void auditMode(Options opt) {
        long expected = readProgress(opt.progressFile);
        if (expected < 0) {
            System.out.println("No local progress found at " + opt.progressFile + ". Nothing to audit.");
            return;
        }

        System.out.println("Local last acknowledged key index (expected): " + expected);

        Jedis j = null;
        try {
            j = ensureConn(null, opt.host, opt.port);
            long count = countKeysByScan(j, opt.prefix + ":*");
            long maxIndex = findMaxIndexByScan(j, opt.prefix + ":*");

            System.out.println("Redis currently holds keys with prefix '" + opt.prefix + "': " + count);
            System.out.println("Highest present index: " + (maxIndex >= 0 ? maxIndex : "none"));

            // Since we wrote exactly one key per index from 0..expected, the expected total
            // is (expected+1)
            long expectedTotal = expected + 1;
            long lost = Math.max(0, expectedTotal - count);
            System.out.println("==== AUDIT RESULT ====");
            System.out.println("Expected total keys (0.." + expected + "): " + expectedTotal);
            System.out.println("Actual keys present: " + count);
            System.out.println("Estimated keys lost in crash window: " + lost);
        } finally {
            if (j != null) {
                try {
                    j.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private static Jedis ensureConn(Jedis j, String host, int port) {
        if (j == null) {
            j = new Jedis(host, port);
        } else if (!"PONG".equalsIgnoreCase(safePing(j))) {
            try {
                j.close();
            } catch (Exception ignore) {
            }
            j = new Jedis(host, port);
        }
        return j;
    }

    private static String safePing(Jedis j) {
        try {
            return j.ping();
        } catch (Exception e) {
            return "ERR";
        }
    }

    private static void persistProgress(String file, long lastAck) {
        if (lastAck < 0)
            return;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, false));
            bw.write(Long.toString(lastAck));
            bw.flush();
        } catch (Exception e) {
            System.err.println("⚠️ Failed to write progress file: " + e.getMessage());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static long readProgress(String file) {
        File f = new File(file);
        if (!f.exists())
            return -1;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String s = br.readLine();
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to read progress file: " + e.getMessage());
            return -1;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static void printMetrics(Jedis j, String prefix, long lastAck) {
        long dbsize = j.dbSize();
        String info = j.info("persistence");
        Map<String, String> persist = parseInfo(info);

        boolean aofEnabled = "1".equals(persist.getOrDefault("aof_enabled", "0"));
        String rdbChanges = persist.getOrDefault("rdb_changes_since_last_save", "?");
        String rdbLastSaveTs = persist.getOrDefault("rdb_last_save_time", "0");

        String aofRewrite = persist.getOrDefault("aof_rewrite_in_progress", "0");
        String rdbBgsave = persist.getOrDefault("rdb_bgsave_in_progress", "0");

        // CONFIG peek (appendonly/appendfsync/save)
        List<String> appendonlyCfg = j.configGet("appendonly");
        List<String> appendfsyncCfg = j.configGet("appendfsync");
        List<String> saveCfg = j.configGet("save");

        String appendonlyVal = valueFromConfig(appendonlyCfg);
        String appendfsyncVal = valueFromConfig(appendfsyncCfg);
        String saveVal = valueFromConfig(saveCfg);

        String lastSaveHuman = formatEpoch(rdbLastSaveTs);

        System.out.println("\n===== Redis Persistence Metrics =====");
        System.out.println("DBSIZE: " + dbsize);
        System.out.println("Last acknowledged index (client): " + (lastAck >= 0 ? lastAck : "none"));
        System.out.println("RDB: changes_since_last_save (≈keys lost if crash now, RDB-only): " + rdbChanges);
        System.out.println("RDB: last_save_time: " + lastSaveHuman);
        System.out.println("RDB: bgsave_in_progress: " + rdbBgsave);
        System.out.println("AOF: enabled: " + aofEnabled);
        System.out.println("AOF: rewrite_in_progress: " + aofRewrite);
        System.out.println("CONFIG save: " + saveVal);
        System.out.println("CONFIG appendonly: " + appendonlyVal);
        System.out.println("CONFIG appendfsync: " + appendfsyncVal + "  (always|everysec|no)");
        System.out.println("====================================\n");
    }

    private static Map<String, String> parseInfo(String infoBlock) {
        Map<String, String> m = new HashMap<>();
        String[] lines = infoBlock.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("#") || !line.contains(":"))
                continue;
            int idx = line.indexOf(':');
            m.put(line.substring(0, idx), line.substring(idx + 1));
        }
        return m;
    }

    private static String valueFromConfig(List<String> cfgReturn) {
        // Jedis returns [key, value]
        if (cfgReturn == null || cfgReturn.size() < 2)
            return "?";
        return cfgReturn.get(1);
    }

    private static String formatEpoch(String epochStr) {
        try {
            long epoch = Long.parseLong(epochStr);
            Instant instant = Instant.ofEpochSecond(epoch);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(instant) + " (" + epoch + ")";
        } catch (Exception e) {
            return "?";
        }
    }

    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Count keys with SCAN. Good enough for demo sizes.
     */
    private static long countKeysByScan(Jedis j, String matchPattern) {
        String cursor = ScanParams.SCAN_POINTER_START;
        long count = 0;
        ScanParams params = new ScanParams().match(matchPattern).count(1000);
        do {
            ScanResult<String> res = j.scan(cursor, params);
            cursor = res.getCursor();
            count += res.getResult().size();
        } while (!"0".equals(cursor));
        return count;
    }

    /**
     * Find the highest numeric suffix among keys like prefix:{i}
     */
    private static long findMaxIndexByScan(Jedis j, String matchPattern) {
        String cursor = ScanParams.SCAN_POINTER_START;
        long max = -1;
        ScanParams params = new ScanParams().match(matchPattern).count(1000);
        do {
            ScanResult<String> res = j.scan(cursor, params);
            cursor = res.getCursor();
            for (String k : res.getResult()) {
                int idx = k.lastIndexOf(':');
                if (idx > -1 && idx + 1 < k.length()) {
                    String tail = k.substring(idx + 1);
                    try {
                        long n = Long.parseLong(tail);
                        if (n > max)
                            max = n;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        } while (!"0".equals(cursor));
        return max;
    }

    private static Options parseArgs(String[] args) {
        Options o = new Options();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host":
                    o.host = args[++i];
                    break;
                case "--port":
                    o.port = Integer.parseInt(args[++i]);
                    break;
                case "--mode":
                    o.mode = args[++i];
                    break;
                case "--prefix":
                    o.prefix = args[++i];
                    break;
                case "--sleepMs":
                    o.sleepMs = Long.parseLong(args[++i]);
                    break;
                case "--reportSec":
                    o.reportSec = Integer.parseInt(args[++i]);
                    break;
                case "--progressFile":
                    o.progressFile = args[++i];
                    break;
                case "--maxKeys":
                    o.maxKeys = Long.parseLong(args[++i]);
                    break;
                default:
                    System.err.println("Unknown arg: " + args[i]);
            }
        }
        return o;
    }
}
