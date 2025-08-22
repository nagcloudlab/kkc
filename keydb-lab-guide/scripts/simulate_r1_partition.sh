#!/bin/bash

M1=keydb-master1
R1=keydb-replica

echo "🚧 Simulating M1 ↛ R1 network partition (one-way)..."

# Block R1 from receiving packets from M1
docker exec $R1 bash -c "iptables -A INPUT -s $M1 -j DROP"
docker exec $R1 bash -c "iptables -A OUTPUT -d $M1 -j DROP"

echo "✅ Replica $R1 is now disconnected from master $M1."
echo "🔍 Run 'info replication' inside $R1 to verify."
