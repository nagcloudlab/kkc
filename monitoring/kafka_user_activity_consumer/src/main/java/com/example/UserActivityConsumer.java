package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

public class UserActivityConsumer {

    private static final String TOPIC = "user-activities";
    private static final String GROUP_ID = "user_activity_group2";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // reliable, controlled consumption
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    private static Jedis createKeyDBClient() {
        // KeyDB is Redis-compatible; Jedis works out of the box.
        return new Jedis("localhost", 6379);
    }

    public static void main(String[] args) {
        final AtomicBoolean running = new AtomicBoolean(true);

        // Boot: Kafka, KeyDB, Cassandra
        KafkaConsumer<String, String> consumer = createKafkaConsumer();
        Jedis keydb = createKeyDBClient();
        CqlSession cassandra = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("localhost", 9042))
                .withKeyspace("useractivityks")
                .withLocalDatacenter("dc1")
                .build();

        // Prepare once
        final PreparedStatement insertStmt = cassandra.prepare(
                "INSERT INTO useractivities (user_id, activity_timestamp, activity_type) " +
                        "VALUES (?, ?, ?)");

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown requested. Closing consumer...");
            running.set(false);
            consumer.wakeup();
        }));

        System.out.println("🎧 Kafka + 🔑 KeyDB + 🗄️ Cassandra ready. Waiting for messages…");

        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) {
                    continue;
                }
                for (ConsumerRecord<String, String> rec : records) {
                    try {
                        if (rec.value() == null || rec.value().isBlank()) {
                            System.out.printf("⚠️  Skipping empty message at %s-%d@%d%n",
                                    rec.topic(), rec.partition(), rec.offset());
                            continue;
                        }

                        JsonNode root = MAPPER.readTree(rec.value());
                        String userId = text(root, "user_id", "unknown");
                        String type = text(root, "activity_type", "unknown");
                        String tsStr = text(root, "timestamp", String.valueOf(System.currentTimeMillis()));
                        String sessionId = text(root, "session_id", null); // optional
                        String amountStr = text(root, "amount", null); // optional

                        long tsMillis = safeParseLong(tsStr, System.currentTimeMillis());
                        Instant ts = Instant.ofEpochMilli(tsMillis);

                        // ---------- KeyDB: idempotency (SETNX on unique key) ----------
                        String idemKey = String.format("ua:%s:%s:%s", userId, type, tsStr);
                        Long setnx = keydb.setnx(idemKey, "1"); // 1 if new
                        keydb.expire(idemKey, 3600);
                        if (setnx == 0) {
                            System.out.printf("🔁 Duplicate detected, skipping user=%s type=%s ts=%s%n",
                                    userId, type, tsStr);
                            continue;
                        }

                        // ---------- KeyDB: counter per user+type + alert ----------
                        String counterKey = String.format("user:%s:activity:%s", userId, type);
                        long count = keydb.incr(counterKey);
                        keydb.expire(counterKey, 3600);
                        if (count > 100) {
                            keydb.publish("user-activity-alert",
                                    "User " + userId + " has performed " + type + " > " + count + " times!");
                        }

                        // ---------- KeyDB: simple state machine validation ----------
                        // States: NONE -> login -> transfer -> logout -> NONE
                        final String stateKey = String.format("user:%s:state", userId);
                        String cur = keydb.get(stateKey);
                        String nextState = cur == null ? "NONE" : cur;
                        boolean transitionOk = isValidTransition(nextState, type);
                        if (!transitionOk) {
                            System.out.printf(
                                    "🚧 Out-of-order event for user %s: type=%s while in state=%s (offset=%d)%n",
                                    userId, type, nextState, rec.offset());
                        }
                        String newState = advanceState(nextState, type);
                        keydb.setex(stateKey, 7200, newState);

                        // ---------- Console log with emojis ----------
                        String emoji = switch (type) {
                            case "login" -> "🔐";
                            case "transfer" -> "💸";
                            case "logout" -> "🚪";
                            default -> "📝";
                        };
                        System.out.printf(
                                "%s user=%s 👤 session=%s 🧾 type=%s ts=%s ⏱ state=%s→%s %s@%d%n",
                                emoji, userId, orDash(sessionId), type, tsStr, nextState, newState,
                                rec.topic(), rec.offset());

                        // ---------- Cassandra persist ----------
                        BoundStatement bs = insertStmt.bind(
                                userId,
                                ts,
                                type)
                                .setConsistencyLevel(ConsistencyLevel.ONE);

                        cassandra.execute(bs);
                        System.out.printf("🗄️  QUORUM write ok for user=%s (%s)%n", userId, type);

                    } catch (Exception ex) {
                        System.err.printf("❌ Processing error at %s-%d@%d: %s%n",
                                rec.topic(), rec.partition(), rec.offset(), ex.getMessage());
                        // (Optional) send to a DLQ topic or write to an error table here
                    }
                }
                // commit after batch
                try {
                    consumer.commitSync();
                    System.out.println("💾 Offsets committed.");
                } catch (CommitFailedException cfe) {
                    System.err.println("⚠️  Commit failed: " + cfe.getMessage());
                }
            }
        } catch (WakeupException ignored) {
            // expected on shutdown
        } finally {
            try {
                consumer.close();
            } catch (Exception ignored) {
            }
            try {
                keydb.close();
            } catch (Exception ignored) {
            }
            try {
                cassandra.close();
            } catch (Exception ignored) {
            }
            System.out.println("✅ Clean shutdown.");
        }
    }

    // -------- helpers --------

    private static String text(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : def;
    }

    private static long safeParseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String orDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static boolean isValidTransition(String cur, String type) {
        return switch (cur) {
            case "NONE" -> type.equals("login");
            case "LOGGED_IN" -> type.equals("transfer") || type.equals("logout");
            case "TRANSFERED" -> type.equals("logout") || type.equals("transfer");
            default -> true; // unknown state, don't block
        };
    }

    private static String advanceState(String cur, String type) {
        return switch (type) {
            case "login" -> "LOGGED_IN";
            case "transfer" -> "TRANSFERED";
            case "logout" -> "NONE";
            default -> cur;
        };
    }
}
