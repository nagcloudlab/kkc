

wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.13-3.9.1.tgz
tar -xzf kafka_2.13-3.9.1.tgz
rm kafka_2.13-3.9.1.tgz
mv kafka_2.13-3.9.1 node1
cp -r node1 node2
cp -r node1 node3


create topic t1 with 3 partitions and replication factor 3
node1/bin/kafka-topics.sh --create --topic t1 --partitions 3 --replication-factor 3 \
  --bootstrap-server localhost:9092 \
  --command-config /Users/nag/kkc/security/sec/admin.properties

create topic t2 with 3 partitions and replication factor 3  
node1/bin/kafka-topics.sh --create --topic t2 --partitions 3 --replication-factor 3 \
  --bootstrap-server localhost:9092 \
  --command-config /Users/nag/kkc/security/sec/admin.properties


node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --add --allow-principal "User:localhost" \
  --operation All --topic t1 \
  --command-config /Users/nag/kkc/security/sec/admin.properties

node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --add --allow-principal "User:npci" \
  --operation All --topic t2 \
  --command-config /Users/nag/kkc/security/sec/admin.properties


list acls for topic t1
node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --list --topic t1 \
  --command-config /Users/nag/kkc/security/sec/admin.properties  