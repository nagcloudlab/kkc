package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserActivityConsumer {

    // ======== Config ========
    private static final String KAFKA_BOOTSTRAP = "localhost:9094";
    private static final String KAFKA_GROUP_ID = "activity_group";
    private static final String KAFKA_TOPIC = "user-activities";

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    private static final String CASS_HOST = "127.0.0.1";
    private static final int CASS_PORT = 9042;
    private static final String CASS_DC = "datacenter1"; // change if different
    private static final String CASS_KS = "useractivityks"; // your keyspace

    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    // ======== Kafka Setup ========
    private static KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(KAFKA_TOPIC));
        return consumer;
    }

    // ======== Redis Setup ========
    private static Jedis createRedisClient() {
        return new Jedis(REDIS_HOST, REDIS_PORT);
    }

    // ======== Cassandra Setup ========
    private static CqlSession createCassandraSession() {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(CASS_HOST, CASS_PORT))
                .withLocalDatacenter(CASS_DC)
                .withKeyspace(CASS_KS)
                .build();
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RUNNING.set(false);
            System.out.println("Shutdown signal received. Closing resources...");
        }));

        KafkaConsumer<String, String> consumer = createKafkaConsumer();
        try (Jedis redisClient = createRedisClient();
                CqlSession cassandraSession = createCassandraSession()) {

            ObjectMapper objectMapper = new ObjectMapper();

            // Prepare INSERT for: useractivityks.useractivities(user_id,
            // activity_timestamp, activity_type)
            PreparedStatement insertStatement = cassandraSession.prepare(
                    "INSERT INTO useractivities (user_id, activity_timestamp, activity_type) VALUES (?, ?, ?)");

            System.out.println("Kafka, Redis, and Cassandra initialized. Waiting for messages...");

            while (RUNNING.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        if (record.value() == null || record.value().isEmpty()) {
                            System.err.println("Skipping empty record");
                            continue;
                        }

                        JsonNode root = objectMapper.readTree(record.value());

                        JsonNode userIdNode = root.get("user_id");
                        JsonNode typeNode = root.get("activity_type");
                        JsonNode tsNode = root.get("timestamp");

                        if (userIdNode == null || typeNode == null || tsNode == null ||
                                userIdNode.isNull() || typeNode.isNull() || tsNode.isNull()) {
                            System.err.println("Skipping record due to missing required fields: " + record.value());
                            continue;
                        }

                        String userId = userIdNode.asText();
                        String activityType = typeNode.asText();
                        Instant activityTimestamp = parseFlexibleTimestamp(tsNode);

                        // -------- Redis: increment counter with TTL --------
                        String redisKey = "user:" + userId + ":activity:" + activityType;
                        long count = redisClient.incr(redisKey);
                        redisClient.expire(redisKey, 3600); // 1 hour

                        if (count > 100) {
                            redisClient.publish("user-activity-alert",
                                    "User " + userId + " has performed " + activityType + " > " + count + " times!");
                        }

                        // -------- Cassandra: insert activity row --------
                        BoundStatement bound = insertStatement.bind(userId, activityTimestamp, activityType);
                        cassandraSession.execute(bound);

                        System.out.printf("Processed activity user=%s type=%s ts=%s%n",
                                userId, activityType, activityTimestamp.toString());

                    } catch (Exception e) {
                        System.err.println("Error processing record: " + e.getMessage());
                        e.printStackTrace(System.err);
                        // continue to next record
                    }
                }
            }
        } catch (Exception fatal) {
            System.err.println("Fatal error, exiting: " + fatal.getMessage());
            fatal.printStackTrace(System.err);
        } finally {
            try {
                consumer.wakeup(); // interrupt any ongoing poll
            } catch (Exception ignored) {
            }
            try {
                consumer.close(Duration.ofSeconds(2));
            } catch (Exception ignored) {
            }
            System.out.println("Consumer closed.");
        }
    }

    /**
     * Parses timestamp from JSON that may be:
     * - number of epoch seconds (e.g., 1754902849)
     * - number of epoch milliseconds (e.g., 1754902849492)
     * - ISO-8601 text (e.g., "2025-08-11T10:15:30Z")
     */
    private static Instant parseFlexibleTimestamp(JsonNode tsNode) {
        if (tsNode.isNumber()) {
            long v = tsNode.asLong();
            return (v > 3_000_000_000L) ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
        } else {
            String ts = tsNode.asText().trim();
            if (ts.matches("\\d+")) {
                long v = Long.parseLong(ts);
                return (v > 3_000_000_000L) ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
            } else {
                // ISO-8601 fallback
                return Instant.parse(ts);
            }
        }
    }
}
