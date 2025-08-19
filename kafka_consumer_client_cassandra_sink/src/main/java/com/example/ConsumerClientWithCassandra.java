package com.example;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerClientWithCassandra {

    public static void main(String[] args) throws Exception {

        // -------------------------- Cassandra Setup --------------------------
        CqlSession cassandraSession = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("127.0.0.2", 9042))
                .withKeyspace("finance")
                .withLocalDatacenter("datacenter1") // Use your DC name
                .build();

        String insertQuery = """
                INSERT INTO transfer_events (
                    transaction_id, from_account, to_account,
                    amount, currency, transfer_type,
                    timestamp, status, failure_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        PreparedStatement preparedStatement = cassandraSession.prepare(insertQuery);
        ObjectMapper mapper = new ObjectMapper();

        // -------------------------- Kafka Configuration --------------------------
        Properties props = new Properties();
        props.put("client.id", "cassandra-sink-client");
        props.put("group.id", "cassandra-sink-group");
        props.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
        props.put("fetch.min.bytes", "1");
        props.put("fetch.max.bytes", "5242880");
        props.put("max.partition.fetch.bytes", "1048576");
        props.put("max.poll.records", "500");
        props.put("heartbeat.interval.ms", "3000");
        props.put("session.timeout.ms", "45000");
        props.put("max.poll.interval.ms", "300000");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        props.put("client.rack", "hyd-rack1");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // -------------------------- Shutdown Hook --------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutdown initiated...");
            consumer.close();
            cassandraSession.close();
            System.out.println("✅ Consumer & Cassandra closed.");
        }));

        // -------------------------- Topic Subscription --------------------------
        consumer.subscribe(Collections.singletonList("upi-transfer-events"));

        // -------------------------- Polling Loop --------------------------
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode event = mapper.readTree(record.value());
                    String txId = event.get("transaction_id").asText();
                    String from = event.get("from_account").asText();
                    String to = event.get("to_account").asText();
                    double amount = event.get("amount").asDouble();
                    String currency = event.get("currency").asText();
                    String type = event.get("transfer_type").asText();
                    String ts = event.get("timestamp").asText();
                    String status = event.get("status").asText();
                    String reason = event.has("failure_reason") ? event.get("failure_reason").asText() : null;

                    cassandraSession.execute(preparedStatement
                            .bind(txId, from, to, amount, currency, type, ts, status, reason)
                            .setConsistencyLevel(ConsistencyLevel.QUORUM));

                    System.out.printf("✅ Persisted TxID: %s | Status: %s%n", txId, status);

                    // TimeUnit.MILLISECONDS.sleep(3); // Simulate processing
                } catch (Exception e) {
                    System.err.printf("❌ Failed to process message: %s%n", e.getMessage());
                }
            }

            // -------------------------- Commit Offsets --------------------------
            if (!records.isEmpty()) {
                consumer.commitSync();
                System.out.println("🔁 Kafka offsets committed.");
            }
        }
    }
}