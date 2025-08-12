package com.example;

public class CustomPartitioner implements org.apache.kafka.clients.producer.Partitioner {

    @Override
    public void configure(java.util.Map<String, ?> configs) {
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes,
            org.apache.kafka.common.Cluster cluster) {
        String transferType = (String) key;
        switch (transferType) {
            case "IMPS":
                // IMPS goes to partition 0
                return 0;
            case "RTGS":
                // RTGS goes to partition 0
                return 0;
            case "NEFT":
                // NEFT goes to partition 1
                return 1;
            case "UPI":
                // UPI goes to partition 2
                return 2;
            default:
                // Default case, send to partition 0
                return 0;
        }
    }

    @Override
    public void close() {
    }

}
