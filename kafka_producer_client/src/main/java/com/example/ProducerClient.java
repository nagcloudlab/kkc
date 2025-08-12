package com.example;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerClient {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put("client.id", "producer-client");
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // default partitioner
        // properties.put("partitioner.class",
        // "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        // custom partitioner
        // properties.put("partitioner.class", "com.example.CustomPartitioner");

        // batch size
        properties.put("batch.size", "16384"); // 16KB
        // linger.ms
        properties.put("linger.ms", "5"); // 5 millisecond

        // buffer memory
        properties.put("buffer.memory", "33554432"); // 32MB
        properties.put("max.block.ms", "60000"); // 60 seconds

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // ransfer Event Generator
        // ---------------------------------------------------
        List<String> transferTypes = List.of("NEFT", "IMPS", "UPI", "RTGS");
        List<String> statusTypes = List.of("PENDING", "SUCCESS", "FAILED");
        String topic = "transfer-events";

        for (int i = 0; i < Integer.MAX_VALUE; i++) {

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
                    failureReason != null ? String.format(",\n  \"failure_reason\": \"%s\"", failureReason) : "");

            // -------------------------- Send Message --------------------------
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, 0, transferType, value);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("❌ Send failed: " + exception.getMessage());
                } else {
                    System.out.printf("✅ Sent: Key=%s | Partition=%d | Offset=%d%n",
                            transferType, metadata.partition(), metadata.offset());
                }
            });

            try {
                TimeUnit.MILLISECONDS.sleep(1); // Simulate real delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // -------------------------- Cleanup --------------------------
        producer.flush();
        producer.close();
        System.out.println("🎉 Kafka Producer completed.");
    }
}