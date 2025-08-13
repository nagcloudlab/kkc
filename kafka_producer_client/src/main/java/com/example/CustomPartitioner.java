package com.example;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

public class CustomPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        // logic to determine partition based on key
        String transferType = (String) key;
        switch (transferType){
            case "IMPS":
                return 0; // IMPS transfers go to partition 0
            case "RTGS":
                return 0; // RTGS transfers go to partition 1
            case "NEFT":
                return 1; // NEFT transfers go to partition 2
            case "UPI":
                return 2; // UPI transfers go to partition 3
            default:
                return cluster.partitionCountForTopic(topic) - 1; // Default to last partition for unknown types
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
