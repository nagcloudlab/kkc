package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;

import java.time.Duration;
import java.util.Properties;

public class StreamsClient {

    public static void main(String[] args) {

        // -------------------------- Streams Configuration --------------------------
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "transfer-streams-app"); // consumer group ID
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094");

        // Default SerDes (key and value are plain strings)
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // -------------------------- Performance & Reliability
        // --------------------------
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3); // parallelism
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000); // commit every second

        // Enable exactly-once processing (EOS v2)
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        // -------------------------- Monitoring (Optional) --------------------------
        // props.put("metrics.recording.level", "DEBUG");
        // props.put("metric.reporters", "org.apache.kafka.common.metrics.JmxReporter");

        // -------------------------- Topology Definition --------------------------
        StreamsBuilder builder = new StreamsBuilder();

        // 1. Source: Read from 'transfer-events' topic
        KStream<String, String> inputStream = builder.stream("transfer-events");

        // 2. Branch the stream by transfer type (key)
        @SuppressWarnings("unchecked")
        KStream<String, String>[] branches = inputStream.branch(
                (key, value) -> "NEFT".equalsIgnoreCase(key),
                (key, value) -> "IMPS".equalsIgnoreCase(key),
                (key, value) -> "UPI".equalsIgnoreCase(key),
                (key, value) -> "RTGS".equalsIgnoreCase(key),
                (key, value) -> true // fallback
        );

        // 3. Sink: Write to topic based on branch
        branches[0].to("neft-transfer-events");
        branches[1].to("imps-transfer-events");
        branches[2].to("upi-transfer-events");
        branches[3].to("rtgs-transfer-events");
        branches[4].to("unknown-transfer-events"); // fallback

        // -------------------------- Streams App Lifecycle --------------------------
        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        // Optional: Clean up local state before starting
        // Uncomment only for development
        // streams.cleanUp();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Streams app shutting down...");
            streams.close(Duration.ofSeconds(10));
            System.out.println("✅ Streams app closed.");
        }));

        // -------------------------- Start Stream Processing --------------------------
        try {
            streams.start();
            System.out.println("🚀 Streams app started and processing transfer-events.");
        } catch (Throwable t) {
            System.err.println("❌ Streams startup failed: " + t.getMessage());
            System.exit(1);
        }
    }
}