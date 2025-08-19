


```cql

CREATE KEYSPACE finance
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 3
};


CREATE TABLE finance.transfer_events (
  transaction_id TEXT PRIMARY KEY,
  from_account TEXT,
  to_account TEXT,
  amount DOUBLE,
  currency TEXT,
  transfer_type TEXT,
  timestamp TEXT,
  status TEXT,
  failure_reason TEXT
);
```