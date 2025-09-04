package com.example;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaProducerClient {
    public static void main(String[] args) {

        System.out.println("Kafka Producer Client is running...");

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9098");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // SSL/TLS Configuration
        // props.put("security.protocol", "SSL");

        // --- Security: SASL over SSL ---
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "SCRAM-SHA-512");

        // JAAS inline config for SCRAM user (must exist on the cluster via
        // kafka-configs.sh)
        props.put(
                "sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                        "username=\"t3user\" " +
                        "password=\"T3Secret!\";");

        props.put("ssl.truststore.location", "/Users/nag/kkc/security/kafka-security/certs/kafka-truststore.p12");
        props.put("ssl.truststore.password", "changeit");
        props.put("ssl.truststore.type", "PKCS12");

        // key store configuration (if client authentication is needed)
        // props.put("ssl.keystore.location",
        // "/Users/nag/kkc/security/kafka-security/certs/client.p12");
        // props.put("ssl.keystore.location",
        // "/Users/nag/kkc/security/kafka-security/certs/bank1.p12");
        // props.put("ssl.keystore.password", "changeit");
        // props.put("ssl.key.password", "changeit");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        // Producer logic would go here

        String topic = "test-topic";
        System.out.println("Producing messages to topic: " + topic);

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key1", "value1");
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            } else {
                System.out.println("Message sent to topic: " + metadata.topic() + " partition: " + metadata.partition()
                        + " offset: " + metadata.offset());
            }
        });

        producer.close();

    }
}