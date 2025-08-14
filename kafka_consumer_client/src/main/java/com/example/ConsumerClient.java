package com.example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ConsumerClient {
    public static void main(String[] args) {

        Properties properties = new Properties();

        // client id
        //---------------------------------------
        properties.put("client.id", "consumer-client-1");

        // bootstrap servers
        //---------------------------------------
        properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");

        // group id
        //---------------------------------------
        properties.put("group.id", "consumer-group-1");

        // static group membership
        //---------------------------------------
        //properties.put("group.instance.id", "consumer-instance-1"); // uncomment to enable


        // polling behavior
        //---------------------------------------
        properties.put("max.poll.records", "500"); // max number of records to fetch in one poll
        properties.put("fetch.min.bytes", "1"); // minimum amount of data to fetch
        properties.put("fetch.max.bytes", "52428800"); // maximum time to wait for data
        properties.put("max.partition.fetch.bytes", "1048576"); // maximum number of bytes
        properties.put("max.poll.interval.ms", "300000"); // maximum time between polls


        // Heartbeat and session timeout
        //---------------------------------------
        properties.put("session.timeout.ms", "45000"); // session timeout for the consumer
        properties.put("heartbeat.interval.ms", "3000"); // heartbeat interval for the consumer

        // commit behavior
        //---------------------------------------
        // option-1: Auto commit
        //properties.put("enable.auto.commit", "true");
        //properties.put("auto.commit.interval.ms", "5000"); // auto commit interval
        // option-2: Manual commit
        properties.put("enable.auto.commit", "false");

        // key and value deserializer
        //---------------------------------------
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");


        // Partition assignment strategy
        //---------------------------------------
         properties.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");


        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);


        // Shutdown hook
        //---------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down consumer...");
            consumer.wakeup();
        }));


        // subscribe to topics
        //---------------------------------------
        consumer.subscribe(List.of("transfer-events"),new RebalanceListener()); // 3 partitions , 0 -> 101, 1 -> 102, 2 -> 103

        // start consuming messages ( Polling loop ) ( fetching messages )
        //---------------------------------------
        try {
            while (true) {
               //System.out.println("Polling for messages...");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); // e.g 100
                //System.out.println("Received " + records.count() + " records.");
                // Sync | Async process the records
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message: " + record.value() + " from partition: " + record.partition() + " at offset: " + record.offset());
                    try {
                        TimeUnit.MILLISECONDS.sleep(3);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                consumer.commitAsync(); // commit the offsets asynchronously
            }
        } catch (WakeupException e) {
            // This exception is expected on shutdown, so we can ignore it
            System.out.println("Consumer wakeup exception caught, shutting down gracefully.");
        } finally {
            consumer.close(); // close the consumer
            System.out.println("Consumer closed.");
        }

    }
}