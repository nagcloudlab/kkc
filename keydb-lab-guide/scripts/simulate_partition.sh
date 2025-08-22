#!/bin/bash

M1=keydb-master1
M2=keydb-master2

echo "🚧 Simulating split-brain: blocking communication between $M1 and $M2..."

docker exec $M1 bash -c "iptables -A OUTPUT -d $M2 -j DROP"
docker exec $M2 bash -c "iptables -A OUTPUT -d $M1 -j DROP"

echo "✅ Split-brain simulation complete. $M1 and $M2 can no longer communicate."
