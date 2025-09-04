


# start kafka nodes with jmx exporter agent
--------------------------------------------------

uuid=$(uuidgen)

node1/bin/kafka-storage.sh format -t $uuid -c node1/config/kraft/server.properties
node2/bin/kafka-storage.sh format -t $uuid -c node2/config/kraft/server.properties
node3/bin/kafka-storage.sh format -t $uuid -c node3/config/kraft/server.properties


export KAFKA_OPTS="-javaagent:/Users/nag/kkc/monitoring/jmx_prometheus_javaagent-1.4.0.jar=7071:/Users/nag/kkc/monitoring/kafka-jmx-config.yml"
node1/bin/kafka-server-start.sh node1/config/kraft/server.properties


export KAFKA_OPTS="-javaagent:/Users/nag/kkc/monitoring/jmx_prometheus_javaagent-1.4.0.jar=7072:/Users/nag/kkc/monitoring/kafka-jmx-config.yml"
node2/bin/kafka-server-start.sh node2/config/kraft/server.properties

export KAFKA_OPTS="-javaagent:/Users/nag/kkc/monitoring/jmx_prometheus_javaagent-1.4.0.jar=7073:/Users/nag/kkc/monitoring/kafka-jmx-config.yml"
node3/bin/kafka-server-start.sh node3/config/kraft/server.properties





node1/bin/kafka-producer-perf-test.sh \
  --topic payments \
  --num-records 1000000 \
  --throughput 1000 \
  --producer-props bootstrap.servers=localhost:9092 \
  --record-size 100



node1/bin/kafka-console-consumer.sh \
  --topic payments \
  --bootstrap-server localhost:9092 \
  --group slow-consumer \
  --from-beginning \
  --max-messages 1000




node1/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list


node1/bin/kafka-console-consumer.sh \
  --topic payments \
  --bootstrap-server localhost:9092 \
  --group slow-consumer \
  --from-beginning \
  --consumer-property enable.auto.commit=true \
  --consumer-property auto.commit.interval.ms=1000 \
  --max-messages 100

node1/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group slow-consumer



curl -LO https://github.com/danielqsj/kafka_exporter/releases/download/v1.9.0/kafka_exporter-1.9.0.darwin-amd64.tar.gz
tar -xvf kafka_exporter-1.9.0.darwin-amd64.tar.gz
cd kafka_exporter-1.9.0.darwin-amd64
./kafka_exporter \
  --kafka.server=localhost:9092 \
  --kafka.server=localhost:9094 \
  --kafka.server=localhost:9096 \
  --group.filter='.*' \
  --topic.filter='.*' \
  --log.level=info


curl -s http://localhost:9308/metrics | grep kafka




start cassandra exporter as standalone process


java -jar cassandra_exporter-2.3.8.jar ce1_config.yml
java -jar cassandra_exporter-2.3.8.jar ce2_config.yml
java -jar cassandra_exporter-2.3.8.jar ce3_config.yml