package com.example;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class ProducerInterceptor implements org.apache.kafka.clients.producer.ProducerInterceptor<String,String>{

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        // add headers to the record
        record.headers().add("dc", "npci-chennai".getBytes());
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // This method is called when the record has been acknowledged by the broker
        if (exception != null) {
            System.err.println("Error sending record: " + exception.getMessage());
        } else {
            System.out.println("Record sent successfully to topic: " + metadata.topic() + " partition: " + metadata.partition());
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
