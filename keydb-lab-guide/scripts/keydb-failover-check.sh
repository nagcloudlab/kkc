#!/bin/bash

# Container names (adjust if different)
M1=keydb-master1
M2=keydb-master2
REPLICA_CONTAINER=keydb-replica

# Run redis-cli inside the replica container
exec_in_replica() {
  docker exec $REPLICA_CONTAINER redis-cli "$@"
}

# Get current upstream master info from replica
current_master=$(exec_in_replica info replication | grep "master_host" | awk -F: '{print $2}' | tr -d '\r')

# Check if M1 is up (reachable from replica container)
is_m1_up=$(docker exec $REPLICA_CONTAINER redis-cli -h $M1 -p 6379 PING 2>/dev/null)

if [[ "$is_m1_up" == "PONG" ]]; then
    echo "$(date) ✅ M1 is UP"
    
    if [[ "$current_master" != "$M1" ]]; then
        echo "$(date) 🔄 Switching back replica to M1"
        exec_in_replica replicaof $M1 6379
    else
        echo "$(date) ✅ Already replicating from M1"
    fi
else
    echo "$(date) ❌ M1 is DOWN"
    
    if [[ "$current_master" != "$M2" ]]; then
        echo "$(date) 🔄 Switching replica to M2"
        exec_in_replica replicaof $M2 6379
    else
        echo "$(date) ✅ Already replicating from M2"
    fi
fi
