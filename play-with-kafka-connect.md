

https://downloads.datastax.com/#akc

--------------------------------------------------
kafka connect cluster with 2 workers
--------------------------------------------------

```bash
kafka/bin/connect-distributed.sh kafka1/config/connect-distributed-worker1.properties
kafka/bin/connect-distributed.sh kafka1/config/connect-distributed-worker2.properties
```


get the list of connector plugins
```bash
curl -X GET http://localhost:8083/connector-plugins
```

create cassandra sink connector
```bash
curl -X POST -H "Content-Type: application/json" --data '{
  "name": "cassandra-sink-connector",
  "config": {
    "connector.class": "com.datastax.oss.kafka.sink.CassandraSinkConnector",
    "tasks.max": "1",
    "topics": "neft-transfer-events",
    "topic.neft-transfer-events.finance.transfer_events_neft.mapping": "transaction_id=value.transaction_id, from_account=value.from_account, to_account=value.to_account, amount=value.amount, currency=value.currency, transfer_type=value.transfer_type, timestamp=value.timestamp, status=value.status, failure_reason=value.failure_reason",
    "contactPoints": "127.0.0.2",
    "port": "9042",
    "keyspace": "finance",
    "loadBalancing.localDc": "datacenter1",
    "consistencyLevel": "QUORUM",
    "writeTimeoutMillis": "3000",
    "readTimeoutMillis": "3000",
    "batchSizeBytes": "0",
    "batchSizeRows": "32",
    "schema.refreshIntervalMillis": "30000",
    "schema.autoCreate": "true",
    "schema.autoUpdate": "true",

    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "key.converter.schemas.enable": "false"
  }
}' http://localhost:8083/connectors


```


get the list of connectors
```bash
curl -X GET http://localhost:8083/connectors
```

get the status of a connector
```bash
curl -X GET http://localhost:8083/connectors/cassandra-sink-connector/status
```

delete a connector
```bash
curl -X DELETE http://localhost:8083/connectors/cassandra-sink-connector
```
