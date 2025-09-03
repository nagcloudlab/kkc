



wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.13-3.9.1.tgz
tar -xzf kafka_2.13-3.9.1.tgz
rm kafka_2.13-3.9.1.tgz
mv kafka_2.13-3.9.1 node1
cp -r node1 node2
cp -r node1 node3




uuid=$(uuidgen)
node1/bin/kafka-storage.sh format -t $uuid -c node1/config/kraft/server.properties
node2/bin/kafka-storage.sh format -t $uuid -c node2/config/kraft/server.properties
node3/bin/kafka-storage.sh format -t $uuid -c node3/config/kraft/server.properties



node1/bin/kafka-server-start.sh  node1/config/kraft/server.properties
node2/bin/kafka-server-start.sh  node2/config/kraft/server.properties
node3/bin/kafka-server-start.sh  node3/config/kraft/server.properties



node1/bin/kafka-topics.sh --create --topic foo --partitions 3 --replication-factor 3 --bootstrap-server localhost:9092 --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties



node1/bin/kafka-topics.sh --create --topic bar --partitions 3 --replication-factor 3 --bootstrap-server localhost:9092 --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties



node1/bin/kafka-topics.sh --create --topic baz --partitions 3 --replication-factor 3 --bootstrap-server localhost:9092 --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties


cd kafka-client
mvn clean compile exec:java -Dexec.mainClass="com.example.KafkaProducerClient"


node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --add --allow-principal "User:bank1.com" \
  --operation All --topic foo \
  --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties

node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --add --allow-principal "User:bank2.com" \
  --operation All --topic bar \
  --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties

node1/bin/kafka-acls.sh \
  --bootstrap-server localhost:9092 \
  --add --allow-principal "User:t1user" \
  --operation All --topic baz \
  --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties


export KAFKA_OPTS="-Djava.security.auth.login.config=/Users/nag/kkc/security/kafka-security/jaas.conf"


node1/bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter --entity-type users --entity-name t1user \
  --add-config 'SCRAM-SHA-512=[password=T1Secret!]' \
  --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties

node1/bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter --entity-type users --entity-name t2user \
  --add-config 'SCRAM-SHA-512=[password=T2Secret!]' \
  --command-config /Users/nag/kkc/security/kafka-security/admin-client.properties

  