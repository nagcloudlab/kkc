package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class ProducerClient_With_Tracking {

    private static final Random RANDOM = new Random();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true); // Volatile | Atomic

    // -------------------------- Tracking --------------------------
    private static final ConcurrentMap<String, Long> INFLIGHT = new ConcurrentHashMap<>(); // messageId -> startNanos
    private static final LongAdder SENT = new LongAdder();
    private static final LongAdder ACKED = new LongAdder();
    private static final LongAdder FAILED = new LongAdder();
    private static final LongAdder TOTAL_LATENCY_MS = new LongAdder(); // sum of ack latencies (ms)
    private static final AtomicLong MAX_LATENCY_MS = new AtomicLong(0); // track max latency
    private static final CopyOnWriteArrayList<String> LAST_FAILURES = new CopyOnWriteArrayList<>(); // recent failure
                                                                                                    // messages

    private static void recordLatency(long ms) {
        TOTAL_LATENCY_MS.add(ms);

        // Update max latency if this value is bigger
        MAX_LATENCY_MS.updateAndGet(prev -> Math.max(prev, ms));
    }

    public static void main(String[] args) {

        Properties properties = new Properties();

        // Client Id
        // -----------------------------------------
        properties.put("client.id", "producer-client-1");

        // Bootstrap Servers -> To get the metadata of the cluster
        // -----------------------------------------
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");

        // Serializer
        // -----------------------------------------
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // Buffering
        // -----------------------------------------
        properties.put("buffer.memory", "33554432"); // 32 MB
        properties.put("max.block.ms", "60000"); // 60 seconds

        // Partitioner
        // -----------------------------------------
        // default partitioner is used, which uses the key to determine the partition
        // - if partition specified in the record, it will use that partition
        // - if key is null, it will use the default partitioner (round-robin)
        // - if key is not null, it will use hash(key)
        properties.put("partitioner.class", "com.example.MyCustomPartitioner");

        // batching
        // -----------------------------------------
        properties.put("batch.size", "16384"); // 16 KB
        properties.put("linger.ms", "5"); // 5 milliseconds

        // Compression
        // -----------------------------------------
        properties.put("compression.type", "none"); // options: none, gzip, snappy, lz4, zstd

        // Max-In-Flight Requests Per Connection
        // -----------------------------------------
        properties.put("max.in.flight.requests.per.connection", "5"); // 5 requests per connection

        // Request Size
        // -----------------------------------------
        properties.put("max.request.size", "1048576"); // 1 MB

        // Acknowledgements
        // -----------------------------------------
        properties.put("acks", "all"); // wait for all ISR

        // Request Timeout
        // -----------------------------------------
        properties.put("request.timeout.ms", "30000"); // 30 seconds

        // Retry Configuration ( transient errors )
        // -----------------------------------------
        properties.put("retries", Integer.MAX_VALUE);
        properties.put("retry.backoff.ms", "100");

        // Delivery Timeout
        // -----------------------------------------
        properties.put("delivery.timeout.ms", "120000"); // 120 seconds

        // Idempotence
        // -----------------------------------------
        properties.put("enable.idempotence", "true"); // dedupe guarantees

        // interceptors
        // -----------------------------------------
        properties.put("interceptor.classes", "com.example.MyCustomInterceptor");

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // -------------------------- Periodic Stats --------------------------
        ScheduledExecutorService stats = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "producer-stats");
            t.setDaemon(true);
            return t;
        });
        stats.scheduleAtFixedRate(() -> {
            long sent = SENT.sum();
            long ack = ACKED.sum();
            long fail = FAILED.sum();
            long inflight = INFLIGHT.size();
            long totalLatency = TOTAL_LATENCY_MS.sum();
            long maxLatency = MAX_LATENCY_MS.get();
            long avgLatency = ack > 0 ? totalLatency / ack : 0;
            System.out.printf("[STATS] sent=%d acked=%d failed=%d inflight=%d avgLatencyMs=%d maxLatencyMs=%d%n",
                    sent, ack, fail, inflight, avgLatency, maxLatency);
        }, 2, 2, TimeUnit.SECONDS);

        // -------------------------- Shutdown Hook --------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Ctrl+C detected → stopping new sends, flushing, and closing...");
            RUNNING.set(false); // stop producing
            try {
                producer.flush();
                producer.close(Duration.ofSeconds(30));
                stats.shutdownNow();
            } catch (Exception e) {
                System.err.println("[SHUTDOWN] Error during flush/close: " + e.getMessage());
            } finally {
                printFinalSummary();
            }
        }, "producer-shutdown-hook"));

        // Transfer Event Generator
        // ---------------------------------------------------
        List<String> transferTypes = List.of("IMPS", "RTGS", "NEFT", "UPI");
        List<String> statusTypes = List.of("PENDING", "SUCCESS", "FAILED");

        String topic = "transfer-events";

        // -------------------------- Produce Loop --------------------------
        try {
            for (int i = 0; i < Integer.MAX_VALUE && RUNNING.get(); i++) {
                if (!RUNNING.get())
                    break;

                String transferType = transferTypes.get(RANDOM.nextInt(transferTypes.size()));
                String status = statusTypes.get(RANDOM.nextInt(statusTypes.size()));

                String transactionId = UUID.randomUUID().toString();
                String fromAccount = "ACC" + (100000 + RANDOM.nextInt(900000));
                String toAccount = "ACC" + (100000 + RANDOM.nextInt(900000));
                double amount = Math.round((100 + RANDOM.nextDouble() * 9900) * 100.0) / 100.0;
                String currency = "INR";
                String timestamp = Instant.now().toString();
                String failureReason = "FAILED".equals(status) ? "Insufficient Balance" : null;

                String value = String.format("""
                        {
                          "transaction_id": "%s",
                          "from_account": "%s",
                          "to_account": "%s",
                          "amount": %.2f,
                          "currency": "%s",
                          "transfer_type": "%s",
                          "timestamp": "%s",
                          "status": "%s"%s
                        }
                        """,
                        transactionId,
                        fromAccount,
                        toAccount,
                        amount,
                        currency,
                        transferType,
                        timestamp,
                        status,
                        failureReason != null ? String.format(",%n  \"failure_reason\": \"%s\"", failureReason) : "");

                if (!RUNNING.get())
                    break;

                // Use a stable messageId (transactionId) for tracking
                String messageId = transactionId;
                long startNanos = System.nanoTime();
                INFLIGHT.put(messageId, startNanos); // local-log
                SENT.increment();

                // Build record; keep key as transferType (your partitioning), but add headers
                // for tracking
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, transferType, value);
                record.headers()
                        .add(new RecordHeader("message_id", messageId.getBytes()))
                        .add(new RecordHeader("transfer_type", transferType.getBytes()))
                        .add(new RecordHeader("status", status.getBytes()));

                try {
                    producer.send(record, (metadata, exception) -> {
                        Long start = INFLIGHT.remove(messageId);
                        long latencyMs = (start == null) ? -1
                                : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                        if (exception != null) {
                            FAILED.increment();
                            if (latencyMs >= 0)
                                recordLatency(latencyMs);
                            String msg = "❌ NACK id=" + messageId + " key=" + transferType + " err="
                                    + exception.getClass().getSimpleName() + ": " + exception.getMessage();
                            System.err.println(msg);
                            // Keep a short ring of last failures (up to ~20)
                            if (LAST_FAILURES.size() > 20)
                                LAST_FAILURES.remove(0);
                            LAST_FAILURES.add(msg);
                        } else {
                            ACKED.increment();
                            if (latencyMs >= 0)
                                recordLatency(latencyMs);
                            System.out.printf("✅ ACK id=%s | key=%s | p=%d | off=%d | ts=%d | latencyMs=%d%n",
                                    messageId, record.key(), metadata.partition(), metadata.offset(),
                                    metadata.timestamp(), latencyMs);
                        }
                    });
                } catch (Exception e) {
                    FAILED.increment();
                    INFLIGHT.remove(messageId);
                    System.err.println("❌ Send invocation failed id=" + messageId + " : " + e.getMessage());
                }

                // Gentle pacing; if you want max throughput, lower/remove this
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            if (RUNNING.get()) {
                System.out.println("[EXIT] Normal termination → flushing and closing...");
                try {
                    producer.flush();
                    producer.close(Duration.ofSeconds(30));
                } catch (Exception e) {
                    System.err.println("[EXIT] Error during flush/close: " + e.getMessage());
                } finally {
                    stats.shutdownNow();
                    printFinalSummary();
                }
            }
        }

        System.out.println("[DONE] Producer terminated.");
    }

    // -------------------------- Helpers --------------------------
    private static void printFinalSummary() {
        long sent = SENT.sum();
        long ack = ACKED.sum();
        long fail = FAILED.sum();
        long inflight = INFLIGHT.size();
        long totalLatency = TOTAL_LATENCY_MS.sum();
        long maxLatency = MAX_LATENCY_MS.get();
        long avgLatency = ack > 0 ? totalLatency / ack : 0;

        System.out.println("------------------------------------------------------------");
        System.out.printf("[SUMMARY] sent=%d acked=%d failed=%d inflight=%d avgLatencyMs=%d maxLatencyMs=%d%n",
                sent, ack, fail, inflight, avgLatency, maxLatency);
        if (!LAST_FAILURES.isEmpty()) {
            System.out.println("[SUMMARY] last failures:");
            LAST_FAILURES.forEach(s -> System.out.println("  " + s));
        }
        if (inflight > 0) {
            System.out.println("[SUMMARY] WARNING: some records still in-flight when closing:");
            INFLIGHT.keySet().stream().limit(10).forEach(id -> System.out.println("  inflight id=" + id));
            if (inflight > 10)
                System.out.println("  ...");
        }
        System.out.println("------------------------------------------------------------");
    }
}
