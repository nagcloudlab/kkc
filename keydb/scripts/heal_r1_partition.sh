#!/bin/bash

M1=keydb-master1
R1=keydb-replica

echo "🧹 Healing M1 ↛ R1 partition..."

docker exec $R1 bash -c "iptables -D INPUT -s $M1 -j DROP || true"
docker exec $R1 bash -c "iptables -D OUTPUT -d $M1 -j DROP || true"

echo "✅ Replica $R1 can now communicate with master $M1 again."
