package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;

public class ProducerClient {
    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put("client.id", "kafka-producer-client1");
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // default partitioner
        //properties.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        // custom partitioner
        properties.put("partitioner.class", "com.example.CustomPartitioner");

        // batch size
        properties.put("batch.size", "16384"); // 16 KB
        properties.put("linger.ms", "5"); // 5 ms
        // compression type
        properties.put("compression.type", "none"); // none, gzip, snappy, lz4, zstd

        KafkaProducer<String,String> producer = new KafkaProducer<>(properties);


        String topic = "transfer-events";
        List<String>  transferTypes = List.of("IMPS", "RTGS", "NEFT", "UPI");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            // Simulate a transfer event with a random type
            String transferType = transferTypes.get(i % transferTypes.size());
            String key = transferType;
            String value = "transfer-event-" + i;
            ProducerRecord<String,String> record = new ProducerRecord<>(topic,key,value);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Error sending message: " + exception.getMessage());
                } else {
                    System.out.printf("Message sent to topic %s partition %d offset %d%n",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        }

        producer.flush();
        producer.close();
        System.out.println("Messages sent successfully.");


    }
}