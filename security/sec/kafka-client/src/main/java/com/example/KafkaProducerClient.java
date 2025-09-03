package com.example;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaProducerClient {
    public static void main(String[] args) {

        System.out.println("Kafka Producer Client is running...");

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // ssl/tls
        props.put("security.protocol", "SSL");
        props.put("ssl.truststore.location", "/Users/nag/kkc/security/sec/kafka-truststore.p12");
        props.put("ssl.truststore.password", "changeit");
        props.put("ssl.truststore.type", "PKCS12");

        // keystore
        props.put("ssl.keystore.location", "/Users/nag/kkc/security/sec/npci-client.p12");
        props.put("ssl.keystore.password", "changeit");
        props.put("ssl.keystore.type", "PKCS12");
        props.put("ssl.key.password", "changeit");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        // Producer logic would go here

        String topic = "t1";
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