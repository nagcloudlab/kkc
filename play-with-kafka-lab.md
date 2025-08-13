


kafka topic management
========================


# Create a topic

```bash
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic topic1 --partitions 1
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic topic1

kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic topic2 --partitions 2
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic topic2

kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic topic3 --partitions 3
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic topic3

kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic topic4 --partitions 40
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic topic4


kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic topic1
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic topic2
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic topic3
kafka1/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic topic4

``` 


Event -> Topic -> Partition -> Offset


Kafka Producer benchmarking
==========================


a. basic producer performance test
--------------------------------
Explanation of parameters:

--topic – Kafka topic to produce to (must already exist unless you have auto-create enabled).
--num-records – Total number of messages to send.
--record-size – Size of each message in bytes.
--throughput – Messages per second; -1 means unlimited.
--producer-props – Key-value pairs of producer configurations.


```bash
kafka1/bin/kafka-producer-perf-test.sh \
  --topic test-topic \
  --num-records 1000000 \
  --record-size 100 \
  --throughput -1 \
  --producer-props bootstrap.servers=localhost:9092
```

b. producer performance test with compression
-----------------------------------

```bash
kafka1/bin/kafka-producer-perf-test.sh \
  --topic test-topic \
  --num-records 500000 \
  --record-size 200 \
  --throughput 100000 \
  --producer-props bootstrap.servers=localhost:9092 compression.type=lz4 acks=1
```

You can adjust:
----------------
compression.type – Compression algorithm (none, gzip, snappy, lz4, zstd).
acks (0, 1, or all) – Controls durability vs performance.
linger.ms – Time to batch before sending.
batch.size – Controls batching for throughput.
buffer.memory – Total producer memory.


------------------
1000000 records sent, 199999.2 records/sec (19.07 MB/sec), 50 ms avg latency, 150 ms max latency.
------------------
Throughput: 199,999.2 records/sec → ~200k msgs/sec.
Data rate: 19.07 MB/sec (MiB/s) → with ~100-byte payloads, that matches 200k × 100B ≈ 19 MiB/s.
Volume sent: 1,000,000 × 100B = ~95.4 MiB (~100 MB decimal).
Elapsed time: ~1,000,000 / 199,999.2 ≈ 5 seconds to complete.
Latency: 50 ms average, 150 ms max end-to-end (producer send → broker ack).
If acks=all, this includes replication wait; with acks=1/0, you’d expect lower latency (and different durability).


