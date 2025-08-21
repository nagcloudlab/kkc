



## Format the storage

kafka4/bin/kafka-storage.sh random-uuid
kafka4/bin/kafka-storage.sh format -t dmSq-xQCSb-ywm66RbHNdQ -c kraft-kafka/kafka-server-201.properties
kafka5/bin/kafka-storage.sh format -t dmSq-xQCSb-ywm66RbHNdQ -c kraft-kafka/kafka-server-202.properties
kafka6/bin/kafka-storage.sh format -t dmSq-xQCSb-ywm66RbHNdQ -c krakraft-kafkaft/kafka-server-203.properties




# Start Kafka Brokers

kafka4/bin/kafka-server-start.sh kraft-kafka/kafka-server-201.properties
kafka5/bin/kafka-server-start.sh krafkraft-kafkat/kafka-server-202.properties
kafka6/bin/kafka-server-start.sh kraft-kafka/kafka-server-203.properties


# kafka UI

cd kafka-ui
java -Dspring.config.additional-location=application.yml --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED -jar kafka-ui-api-v0.7.2.jar  



