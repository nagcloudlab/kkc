package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

public class SecureKafkaProducer {
    public static void main(String[] args) {
        Properties props = new Properties();

        // Kafka broker addresses with SSL ports
        props.put("bootstrap.servers", "localhost:29091,localhost:29092,localhost:29093");

        // Serializer classes
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // TLS/SSL Configuration
        props.put("security.protocol", "SSL");

        // props.put("ssl.keystore.location", "/Users/nag/kafka-security/client.p12");
        // props.put("ssl.keystore.password", "changeit");
        // props.put("ssl.keystore.type", "PKCS12");
        // props.put("ssl.key.password", "changeit");

        props.put("ssl.truststore.location",
                "/Users/nag/kkc/security/kafka-security/kafka-truststore.p12");
        props.put("ssl.truststore.password", "changeit");
        props.put("ssl.truststore.type", "PKCS12");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String topic = "test-topic";
            String key = "key1";
            String value = "Hello, secure Kafka!";

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            RecordMetadata metadata = producer.send(record).get();

            System.out.printf("Sent message to topic=%s partition=%d offset=%d%n",
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
