package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;

import java.util.Properties;

public class ProducerClient {
    public static void main(String[] args) {

        Properties properties = new Properties();

        // Client Id
        // -----------------------------------------
        properties.put("client.id", "producer-client-1");

        // Bootstrap Servers -> To get the metadata of the cluster
        // -----------------------------------------
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");

        // Serde (Serializer/Deserializer)
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
        // properties.put("partitioner.class", "com.example.MyCustomPartitioner");

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down producer...");
            // Any cleanup code can go here
            producer.flush();
        }));

        String topic = "transfer-events";
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String key = null;
            String value = "This is a test message " + i;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            try {
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("Error sending message: " + exception.getMessage());
                        if (exception instanceof TimeoutException) {
                            System.out.println("Message sending timed out.");
                        }
                    } else {
                        System.out.printf("Message sent successfully to topic %s partition %d offset %d%n",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
            } catch (Exception e) {
                System.err.println("Error producing message: " + e.getMessage());
                // Fallback action, if kafka is down or unreachable
            }

            // Simulate some delay to control the rate of message production
            try {
                Thread.sleep(1000); // Sleep for 1000 milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.err.println("Producer interrupted: " + e.getMessage());
            }

        }

        producer.flush();
        producer.close();

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
