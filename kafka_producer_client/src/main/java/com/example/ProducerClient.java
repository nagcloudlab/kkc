package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProducerClient {

    private static final Random RANDOM = new Random();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true); // Volatile | Atomic


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

        // -if partition specified in the record, it will use that partition
        // - if key is null, it will use the default partitioner to determine the
        // partition ( round-robin )
        // - if key is not null, it will use the hash of the key to determine the
        // partition

        // properties.put("partitioner.class",
        // "org.apache.kafka.clients.producer.internals.DefaultPartitioner");

        // if you want to use a custom partitioner, you can specify it here
        properties.put("partitioner.class", "com.example.MyCustomPartitioner");

        // batching
        // -----------------------------------------
        properties.put("batch.size", "16384"); // 16 KB
        properties.put("linger.ms", "5"); // 5 milliseconds

        // Compression
        // -----------------------------------------
        properties.put("compression.type", "none"); // impact : More CPU usage, less network traffic
        // options: none, gzip, snappy, lz4, zstd

        // Max-In-Flight Requests Per Connection
        // -----------------------------------------
        properties.put("max.in.flight.requests.per.connection", "5"); // 5 requests per connection

        // Request Size
        // -----------------------------------------
        properties.put("max.request.size", "1048576"); // 1 MB

        // Acknowledgements
        // -----------------------------------------
        // properties.put("acks", "0"); // 0 means no acknowledgment
        // properties.put("acks", "1"); // 1 means wait for the leader to acknowledge
        properties.put("acks", "all"); // all means wait for all in-sync replicas to acknowledge

        // Request Timeout
        // -----------------------------------------
        properties.put("request.timeout.ms", "30000"); // 30 seconds

        // Retry Configuration ( transient errors )
        // -----------------------------------------
        // after request timeout, producer will retry sending the message
        // if leader is not available or if there is a transient error
        // if no enough replicas are available, producer will retry sending the message
        properties.put("retries", Integer.MAX_VALUE); // Number of retries on failure
        properties.put("retry.backoff.ms", "100"); // 100 milliseconds between retries

        // Delivery Timeout
        // -----------------------------------------
        properties.put("delivery.timeout.ms", "120000"); // 120 seconds

        // Idempotence
        // -----------------------------------------
        // if you want to enable idempotence, you can set the following properties
        // if idempotence is enabled, the producer will ensure that messages are not
        // duplicated
        properties.put("enable.idempotence", "true"); // Enable idempotence

        // interceptors
        // -----------------------------------------
        // if you want to use interceptors, you can specify them here
        properties.put("interceptor.classes", "com.example.MyCustomInterceptor");

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // -------------------------- Shutdown Hook --------------------------
        // Register EARLY so Ctrl+C always triggers a graceful shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Ctrl+C detected → stopping new sends, flushing, and closing...");
            RUNNING.set(false); // Stop producing immediately (no new producer.send() calls)
            try {
                // Flush: waits for all queued & in-flight records to complete (success or terminal failure)
                producer.flush();
                // Close with a cap: also ensures internal resources close cleanly
                producer.close(Duration.ofSeconds(30));
                System.out.println("[SHUTDOWN] Flush & close complete.");
            } catch (Exception e) {
                System.err.println("[SHUTDOWN] Error during flush/close: " + e.getMessage());
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
                // Build a message (quickly skip if shutdown started)
                if (!RUNNING.get()) break;

                String transferType = transferTypes.get(RANDOM.nextInt(transferTypes.size()));
                String status       = statusTypes.get(RANDOM.nextInt(statusTypes.size()));

                String transactionId = UUID.randomUUID().toString();
                String fromAccount   = "ACC" + (100000 + RANDOM.nextInt(900000));
                String toAccount     = "ACC" + (100000 + RANDOM.nextInt(900000));
                double amount        = Math.round((100 + RANDOM.nextDouble() * 9900) * 100.0) / 100.0;
                String currency      = "INR";
                String timestamp     = Instant.now().toString();
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

                // Do not send if shutdown already began
                if (!RUNNING.get()) break;

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, transferType, value);

                try {
                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            System.err.println("❌ Send failed: " + exception.getMessage());
                        } else {
                            System.out.printf("✅ Sent: Key=%s | Partition=%d | Offset=%d%n",
                                    transferType, metadata.partition(), metadata.offset());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error producing message: " + e.getMessage());
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
            // If we exit the loop without Ctrl+C, ensure resources are released.
            // If the shutdown hook already closed the producer, this will no-op gracefully.
            if (RUNNING.get()) {
                System.out.println("[EXIT] Normal termination → flushing and closing...");
                try {
                    producer.flush();
                    producer.close(Duration.ofSeconds(30));
                } catch (Exception e) {
                    System.err.println("[EXIT] Error during flush/close: " + e.getMessage());
                }
            }
        }

        System.out.println("[DONE] Producer terminated.");
    }
}




// Service Factors

// 1. Throughput: The producer can send messages at a high rate, depending on
// the configuration.
// 2. Latency: The time taken to send a message can vary based on the
// configuration and the network conditions.
// 3. Durability: The producer can ensure that messages are not lost by using
// acknowledgments and retries.
// 4. Availability: The producer can continue to send messages even if some
// brokers are down, as long as there are enough in-sync replicas available.

// How to Achieve High Throughput and Low Latency
// 1. Use batching: By configuring the batch size and linger time, the producer
// can send multiple messages in a single request, reducing the number of
// requests sent to the broker.
// 2. Use compression: By compressing the messages, the producer can reduce the
// size of the messages sent over the network, reducing the network traffic and
// improving throughput.
// 3. Use idempotence: By enabling idempotence, the producer can ensure that
// messages are not duplicated,
// which can help in achieving higher throughput and lower latency.
// 4. Use appropriate partitioning: By using a custom partitioner, the producer
// can control how messages are distributed across partitions, which can help in
// achieving better load balancing and higher throughput.
// 5. Use appropriate acknowledgments: By configuring the acknowledgments, the
// producer can control how many replicas need to acknowledge the message before
// considering it sent, which can help in achieving better durability and
// availability.
// 6. Use appropriate retry configuration: By configuring the retries and retry
// backoff, the producer can handle transient errors and ensure that messages
// are sent successfully, which can help in achieving better durability and
// availability.
// 7. Use appropriate request timeout: By configuring the request timeout, the
// producer can control how long it waits for a response from the broker before
// retrying, which can help in achieving better availability.

// How to Achieve High Availability
// 1. Use multiple brokers: By configuring the producer to connect to multiple
// brokers, it can continue to send messages even if some brokers are down.
// 2. Use appropriate acknowledgments:
// By configuring the acknowledgments to wait for all in-sync replicas to
// acknowledge the message,
// the producer can ensure that messages are not lost even if some brokers are
// down.
// 3. Use appropriate retry configuration: By configuring the retries and retry
// backoff, the producer can handle transient errors and ensure that messages
// are sent successfully, which can help in achieving better availability
// 4. Use appropriate request timeout: By configuring the request timeout, the
// producer can control how long it waits for a response from the broker before
// retrying, which can help in achieving better availability.

// How to Handle Failures
// 1. Use retries: By configuring the retries and retry backoff, the producer
// can handle transient errors and ensure that messages are sent successfully.
// 2. Use appropriate acknowledgments: By configuring the acknowledgments to
// wait for all in-sync replicas to acknowledge the message, the producer can
// ensure
// that messages are not lost even if some brokers are down.
// 3. Use appropriate request timeout: By configuring the request timeout, the
// producer can control
