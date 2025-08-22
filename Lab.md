
install git
------------------------------------------------
```bash
sudo apt update
sudo apt install -y git
git --version
```

clone my repository
------------------------------------------------
```bash
git clone http://github.com/nagcloudlab/kkc.git
```


install Java and Maven
------------------------------------------------

```bash
sudo apt install -y openjdk-17-jdk
java --version

sudo apt install -y maven
mvn --version
```

Deploy Zookeeper and Kafka
------------------------------------------------

```bash
wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.13-3.9.1.tgz
tar -xzf kafka_2.13-3.9.1.tgz
rm kafka_2.13-3.9.1.tgz
mv kafka_2.13-3.9.1 kafka1
cp -r kafka1 kafka2
cp -r kafka1 kafka3
```


On each Kafka server, edit the server.properties file:

```properties
broker.id=101 | 102 | 103
listeners=PLAINTEXT://:9092 |PLAINTEXT://:9093 | PLAINTEXT://:9094
log.dirs=/tmp/kafka-logs-101 | /tmp/kafka-logs-102 | /tmp/kafka-logs-103
```


Start Zookeeper
( in future, we'll use KRaft mode, so this step will be removed)
```bash
kafka/bin/zookeeper-server-start.sh kafka1/config/zookeeper.properties 
```

Start Kafka servers
```bash
kafka/bin/kafka-server-start.sh zk-kafka/config/server.properties
kafka/bin/kafka-server-start.sh zk-kafka/config/server.properties
kafka/bin/kafka-server-start.sh zk-kafka/config/server.properties
```


Install Kafka UI
------------------------------------------------

```bash
mkdir kafka-ui
cd kafka-ui
curl -L https://github.com/provectus/kafka-ui/releases/download/v0.7.2/kafka-ui-api-v0.7.2.jar --output kafka-ui-api-v0.7.2.jar
touch application.yml
```

Add the following content to application.yml:

```yaml
kafka:
  clusters:
    - name: local
      bootstrapServers: localhost:9092
```

```bash
java -Dspring.config.additional-location=application.yml --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED -jar kafka-ui-api-v0.7.2.jar   
```   

open http://localhost:8080 in your browser to access Kafka UI


Deploy a simple Kafka producer
------------------------------------------------

```bash
cd user_activity_producer
mvn clean compile exec:java -Dexec.mainClass="com.example.RandomUserActivityProducer"
```


Deploy a simple Kafka consumer
------------------------------------------------

```bash
cd user_activity_consumer
mvn clean compile exec:java -Dexec.mainClass="com.example.UserActivityConsumer"
```



deploy redis/keydb
------------------------------------------------

sudo apt install redis-server -y
( stop redis-server if it is running , becouse we will run multiple instances )
sudo systemctl stop redis-server 

```bash
mkdir redis-master
cd redis-master
touch redis.conf
```

```bash
mkdir redis-replica1
cd redis-replica1
touch redis.conf
```

# In redis.conf for 6380
```conf
port 6380
replicaof localhost 6379
```

```bash
mkdir redis-replica2
cd redis-replica2
touch redis.conf
```

# In redis.conf for 6381
```conf
port 6381
replicaof localhost 6379
```


```bash
cd redis-master
redis-server redis.conf
cd 6380
redis-server redis.conf
cd 6381
redis-server redis.conf
```

```bash
redis-cli -p 6379
INFO REPLICATION
```




Deploy cassandra
------------------------------------------------

```bash
wget https://dlcdn.apache.org/cassandra/5.0.5/apache-cassandra-5.0.5-bin.tar.gz
tar -xzf apache-cassandra-5.0.5-bin.tar.gz
rm apache-cassandra-5.0.5-bin.tar.gz
mv apache-cassandra-5.0.5 cassandra1
cp -r cassandra1 cassandra2
cp -r cassandra1 cassandra3
```

on each Cassandra, edit the cassandra.yaml file:

```yaml
listen_address: 127.0.0.2 | 127.0.0.3 | 127.0.0.4
rpc_address: 127.0.0.2 | 127.0.0.3 | 127.0.0.4
seeds: "127.0.0.2:7000"
```

jvm.server options 

-Xms500m
-Xmx500m


on each Cassandra, edit the cassandra-env.sh file: ( line 235 )
```bash
JMX_PORT=7199 | 7299 | 7399
```

in ubuntu, we need to add the following lines to /etc/hosts file:

```bash
sudo bash -c 'cat >>/etc/hosts <<EOF
127.0.0.2   node1
127.0.0.3   node2
127.0.0.4   node3
EOF'


sudo cat /etc/hosts
sudo ip addr add 127.0.0.2/8 dev lo
sudo ip addr add 127.0.0.3/8 dev lo
sudo ip addr add 127.0.0.4/8 dev lo
```

Start Cassandra servers

```bash
cassandra1/bin/cassandra -f
cassandra2/bin/cassandra -f
cassandra3/bin/cassandra -f

cassandra1/bin/nodetool status

```

```cqlsh
cqlsh

CREATE KEYSPACE useractivityks WITH REPLICATION = {'class': 'NetworkTopologyStrategy', 'replication_factor': 3};

CREATE TABLE IF NOT EXISTS useractivityks.useractivities (
    user_id varchar,
    activity_timestamp timestamp,
    activity_type varchar,
    PRIMARY KEY ((user_id), activity_timestamp)
);
select * from useractivityks.useractivities;
```