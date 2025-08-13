package com.example;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

public class MyCustomPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        String transferType = (String) key;
        switch (transferType){
            case "IMPS":
                return 0; // Partition 0 for IMPS
            case "RTGS":
                return 0; // Partition 0 for RTGS
            case "NEFT":
                return 1; // Partition 1 for NEFT
            case "UPI":
                return 2; // Partition 2 for PUSH
            default:
                return cluster.partitionCountForTopic(topic) - 1; // Default to last partition if unknown type
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
