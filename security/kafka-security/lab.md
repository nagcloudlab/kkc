

# Download and extract Kafka
wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.13-3.9.1.tgz
tar -xzf kafka_2.13-3.9.1.tgz
rm kafka_2.13-3.9.1.tgz
mv kafka_2.13-3.9.1 node1
cp -r node1 node2
cp -r node1 node3   


# Create data directories
uuid=$(uuidgen)
node1/bin/kafka-storage.sh format -t $uuid -c node1/config/kraft/server.properties
node2/bin/kafka-storage.sh format -t $uuid -c node2/config/kraft/server.properties
node3/bin/kafka-storage.sh format -t $uuid -c node3/config/kraft/server.properties  


# Start Kafka nodes
node1/bin/kafka-server-start.sh  node1/config/kraft/server.properties
node2/bin/kafka-server-start.sh  node2/config/kraft/server.properties
node3/bin/kafka-server-start.sh  node3/config/kraft/server.properties


# Create topic with TLS
node1/bin/kafka-topics.sh --create --topic test-topic1 --bootstrap-server localhost:29091,localhost:29092,localhost:29093 --partitions 3 --replication-factor 3 --command-config /Users/nag/kafka-security/admin.properties


# Produce messages with TLS
node1/bin/kafka-console-producer.sh --topic test-topic1 --producer.config /Users/nag/kafka-security/producer.properties --bootstrap-server localhost:29091,localhost:29092,localhost:29093



