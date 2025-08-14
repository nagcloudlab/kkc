package com.example;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class MyCustomInterceptor implements ProducerInterceptor {

    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        // Example: Add a custom header to the record
        record.headers().add("npci-kafka-cluster-region", "chennai".getBytes());

        // You can also modify the record here if needed
        // For example, you could change the value or key based on some logic

        return record; // Return the modified or original record
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // track the acknowledgment of the record
        //..
        // update WAL file , record as sent..
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
