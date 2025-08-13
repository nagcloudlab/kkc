package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;

public class ProducerClient {
    public static void main(String[] args) {

        Properties properties = new Properties();

        // Basic configuration
        //-----------------------------------
        properties.put("client.id", "kafka-producer-client1");
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // partitioner
        //-----------------------------------
        // default partitioner
        //properties.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        // custom partitioner
        //properties.put("partitioner.class", "com.example.CustomPartitioner");

        // Acknowledgment
        //-----------------------------------
        // Ack
        //properties.put("acks", "0"); // No acknowledgment  ( E.g non-critical data ) -> more throughput, less reliability
        //properties.put("acks", "1"); // Leader acknowledgment ( E.g critical data ) -> more throughput, less reliability
        properties.put("acks", "all"); // All replicas acknowledgment ( E.g critical data )  -> less throughput, more reliability

        // Retries and timeouts ( due to transient errors )
        //--------------------------------------------------------
        properties.put("retries", Integer.MAX_VALUE); // Number of retries in case of failure
        properties.put("retry.backoff.ms", "100"); // 100 ms delay between retries

        // Delivery timeout
        // This is the maximum time the producer will wait for a message to be acknowledged by the
        // broker before considering it failed.
        // If the message is not acknowledged within this time, it will be retried based on
        // the retry configuration.
        // This is useful for ensuring that messages are not stuck indefinitely in the producer.
        // It is also useful for preventing the producer from waiting too long for a response
        // from the broker, which can lead to performance issues.
        //--------------------------------------------------------
        properties.put("delivery.timeout.ms", "120000"); // 120 seconds (2 minutes)

        // Max in-flight requests
        // This is the maximum number of requests that can be sent to the broker without waiting for
        // a response. This is useful for controlling the flow of messages and preventing the producer
        // from overwhelming the broker with too many requests at once.
        // If this is set to a low value, the producer will wait for a response from the broker
        // before sending the next request. If this is set to a high value,
        // the producer will send multiple requests in parallel, which can improve throughput but
        // can also lead to increased memory usage and potential out-of-memory errors.
        //--------------------------------------------------------
        properties.put("max.in.flight.requests.per.connection", "5"); // 5 requests per connection


        // Buffering
        // This is the amount of memory allocated to the producer for buffering messages
        // before they are sent to the broker. This is useful for controlling the flow of messages
        // and preventing the producer from overwhelming the broker with too many messages at once.
        //---------------------------------------------------------
        properties.put("buffer.memory", "33554432"); // 32 MB

        // Max block time
        // This is the maximum time the producer will block waiting for buffer space to become available.
        // If the buffer is full and the producer cannot send messages immediately,
        // it will block until space becomes available or the specified time elapses.
        // If the time elapses and space is still not available, the producer will throw an
        // exception indicating that it could not send the message.
        // This is useful for preventing the producer from blocking indefinitely and
        // potentially causing performance issues in the application.
        //---------------------------------------------------------
        properties.put("max.block.ms", "60000"); // 60 seconds (1 minute)


        // batch size
        // This is the maximum size of a batch of messages that the producer will send to the broker.
        // If the batch size is exceeded, the producer will send the batch immediately,
        // even if the linger time has not elapsed.
        // This is useful for controlling the flow of messages and preventing the producer from
        // overwhelming the broker with too many messages at once.
        // The batch size is specified in bytes, and the default value is 16384 bytes
        // (16 KB). This means that the producer will send a batch of messages
        // when the total size of the messages in the batch reaches 16 KB or when the
        // linger time has elapsed, whichever comes first.
        //---------------------------------------------------------
        properties.put("batch.size", "16384"); // 16 KB
        properties.put("linger.ms", "5"); // 5 ms
        // compression type
        // This is the type of compression to use for the messages sent by the producer.
        // Compression can help reduce the size of the messages sent over the network,
        // which can improve throughput and reduce network bandwidth usage.
        // The available compression types are:
        // - none: No compression (default)
        // - gzip: Gzip compression
        // - snappy: Snappy compression
        // - lz4: LZ4 compression
        // - zstd: Zstandard compression
        //---------------------------------------------------------
        properties.put("compression.type", "none"); // none, gzip, snappy, lz4, zstd


        // request size
        // This is the maximum size of a request that the producer will send to the broker.
        // If the request size exceeds this limit, the producer will throw an exception.
        // This is useful for preventing the producer from sending requests that are too large
        // and potentially causing performance issues in the application.
        // The default value is 1048576 bytes (1 MB), which means that the
        // producer will throw an exception if the request size exceeds 1 MB.
        //---------------------------------------------------------
        properties.put("max.request.size", "1048576"); // 1 MB

        // request timeout
        // This is the maximum time the producer will wait for a response from the broker
        // before considering the request failed. If the response is not received within this time,
        // the producer will throw an exception indicating that the request failed.
        // This is useful for preventing the producer from waiting too long for a response
        // from the broker, which can lead to performance issues in the application.
        // The default value is 30000 milliseconds (30 seconds), which means that the
        // producer will throw an exception if the response is not received within 30 seconds.
        //---------------------------------------------------------
        properties.put("request.timeout.ms", "30000"); // 30 seconds (30,000 ms)


        // Medatadata fetch timeout
        // This is the maximum time the producer will wait for metadata from the broker
        // before considering the request failed. If the metadata is not received within this time,
        // the producer will throw an exception indicating that the request failed.
        // This is useful for preventing the producer from waiting too long for metadata,
        // which can lead to performance issues in the application.
        // The default value is 60000 milliseconds (60 seconds), which means that the
        // producer will throw an exception if the metadata is not received within 60 seconds.
        //---------------------------------------------------------
        properties.put("metadata.fetch.timeout.ms", "60000"); // 60 seconds (60,000 ms)

        // Metadata max age
        // This is the maximum age of the metadata cached by the producer.
        // If the metadata is older than this value, the producer will fetch new metadata from the
        // broker before sending the next request. This is useful for ensuring that the producer
        // has the most up-to-date metadata about the broker and the topics it is producing to
        // The default value is 300000 milliseconds (5 minutes), which means that the
        // producer will fetch new metadata if the cached metadata is older than 5 minutes.
        //---------------------------------------------------------
        properties.put("metadata.max.age.ms", "300000"); // 5 minutes (300


        // Idempotence
        // This is a feature that ensures that messages are sent exactly once to the broker,
        // even in the presence of retries and failures. When idempotence is enabled,
        // the producer will assign a unique sequence number to each message and ensure that
        // messages with the same sequence number are not sent multiple times.
        // This is useful for preventing duplicate messages in the topic and ensuring that
        // messages are processed exactly once by the consumer.
        // To enable idempotence, set the following properties:
        //---------------------------------------------------------
        properties.put("enable.idempotence", "true"); // Enable idempotence


        // interceptors
        // Interceptors are used to intercept messages before they are sent to the broker.
        // They can be used for logging, monitoring, or modifying messages before they are sent.
        // To use interceptors, set the following property:
        // properties.put("interceptor.classes", "com.example.MyInterceptor");
        // Note: Interceptors are not enabled by default, so you need to explicitly set the property
        // to enable them. You can specify multiple interceptors by separating their class names with commas
        //------------------------------------------
        properties.put("interceptor.classes", "com.example.ProducerInterceptor");



        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        String topic = "transfer-events";
        List<String> transferTypes = List.of("IMPS", "RTGS", "NEFT", "UPI");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            // Simulate a transfer event with a random type
            String transferType = transferTypes.get(i % transferTypes.size());
            String key = null;
            String value = "transfer-event-" + i;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Error sending message: " + exception.getMessage());
                } else {
                    System.out.printf("Message sent to topic %s partition %d offset %d%n",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
            // Simulate a delay between messages
            // This is just for demonstration purposes; in a real application, you might not want to
            try {
                Thread.sleep(1000); // 1000 ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer interrupted: " + e.getMessage());
                break;
            }
        }

        producer.flush();
        producer.close();
        System.out.println("Messages sent successfully.");


    }
}


// How to achieve high throughput in Kafka producer? ( i.e sending more messages per second )
// batching
// compression
// No-Ack
// max-in-flight-requests
// Good nUmber of partitions
// Good partitioner

// How to achieve safety ( durability ) in Kafka producer? ( i.e sending more messages per second )

// RF at-least 3
// Ack=all & Min-In-sync=2
// retry
// buffer

// How to achieve less latency in Kafka producer? ( i.e sending more messages per second )
// Avoid/less batching
// Ack=leader
// Good N/W Bandwidth
// receive/send buffers
//....

