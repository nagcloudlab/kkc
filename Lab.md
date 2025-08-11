


sudo apt update

install Java and Maven
------------------------------------------------

sudo apt install -y openjdk-17-jdk
java --version


sudo apt install -y maven
mvn --version

install Zookeeper and Kafka
------------------------------------------------

wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.13-3.9.1.tgz
tar -xzf kafka_2.13-3.9.1.tgz
mv kafka_2.13-3.9.1 kafka1
cp -r kafka1 kafka2
cp -r kafka1 kafka3


on each Kafka-server, edit the server.properties file:

broker.id=101 | 102 | 103
listeners=PLAINTEXT://:9092 |PLAINTEXT://:9093 | PLAINTEXT://:9094
log.dirs=/tmp/kafka-logs-101 | /tmp/kafka-logs-102 | /tmp/kafka-logs-103


# Start Zookeeper
cd kafka1
bin/zookeeper-server-start.sh config/zookeeper.properties
# Start Kafka servers
bin/kafka-server-start.sh config/server.properties &
cd ../kafka2
bin/kafka-server-start.sh config/server.properties &
cd ../kafka3
bin/kafka-server-start.sh config/server.properties &


Install Kafka UI
------------------------------------------------

mkdir kafka-ui
cd kafka-ui
curl -L https://github.com/provectus/kafka-ui/releases/download/v0.7.2/kafka-ui-api-v0.7.2.jar --output kafka-ui-api-v0.7.2.jar
touch application.yml
kafka:
  clusters:
    - name: local
      bootstrapServers: localhost:9092
java -Dspring.config.additional-location=application.yml --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED -jar kafka-ui-api-v0.7.2.jar      



Deploy a simple Kafka producer
------------------------------------------------

cd user_activity_producer
mvn clean compile exec:java -Dexec.mainClass="com.example.RandomUserActivityProducer"