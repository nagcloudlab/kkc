


wget https://dlcdn.apache.org/cassandra/5.0.5/apache-cassandra-5.0.5-bin.tar.gz
tar -xvzf apache-cassandra-5.0.5-bin.tar.gz
rm apache-cassandra-5.0.5-bin.tar.gz
mv apache-cassandra-5.0.5 cassandra1
cp -r cassandra1 cassandra2
cp -r cassandra1 cassandra3
cp -r cassandra1 cassandra4
cp -r cassandra1 cassandra5
cp -r cassandra1 cassandra6
cp -r cassandra1 cassandra7
cp -r cassandra1 cassandra8


# Update cassandra-env.sh for each node

JMX_PORT="" 7199 # for node 1
JMX_PORT="" 7299 # for node 2
JMX_PORT="" 7399 # for node 3
JMX_PORT="" 7499 # for node 4
JMX_PORT="" 7599 # for node 5
JMX_PORT="" 7699 # for node 6
JMX_PORT="" 7799 # for node 7
JMX_PORT="" 7899 # for node 8

# Update cassandra.yaml for each node

listen_address: 127.0.0.1
rpc_address: 127.0.0.1
seed_provider:
  - class_name: org.apache.cassandra.locator.SimpleSeedProvider
    parameters:
         - seeds: "127.0.0.1,127.0.0.5"
endpoint_snitch: GossipingPropertyFileSnitch

# Update cassandra-rackdc.properties for each node ( 1 t to 4 )
dc=dc1
rack=rack1

# Update cassandra-rackdc.properties for each node ( 5 to 8 )
dc=dc2
rack=rack1

# Update jvm.server.options for each node
-Xms512M -Xmx512M