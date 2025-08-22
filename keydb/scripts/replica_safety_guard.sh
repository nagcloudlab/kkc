#!/bin/bash

# CONFIGURABLES
HOST="localhost"
PORT=6379
REQUIRED_SLAVES=2
MAX_LAG=5

# Get number of connected slaves (replicas)
connected_slaves=$(redis-cli -h $HOST -p $PORT INFO replication | grep ^connected_slaves: | cut -d':' -f2)

# Get current min-slaves-to-write setting
current_min=$(redis-cli -h $HOST -p $PORT CONFIG GET min-slaves-to-write | tail -n1)

echo "🧪 Current connected replicas: $connected_slaves"

if [ "$connected_slaves" -lt "$REQUIRED_SLAVES" ]; then
  if [ "$current_min" -ne "$REQUIRED_SLAVES" ]; then
    echo "🚫 Too few replicas! Enforcing write block..."
    redis-cli -h $HOST -p $PORT CONFIG SET min-slaves-to-write $REQUIRED_SLAVES
    redis-cli -h $HOST -p $PORT CONFIG SET min-slaves-max-lag $MAX_LAG
  else
    echo "⚠️ Already enforcing write protection (min=$current_min)"
  fi
else
  echo "✅ Enough replicas connected ($connected_slaves). Writes allowed."
  redis-cli -h $HOST -p $PORT CONFIG SET min-slaves-to-write 0
fi
