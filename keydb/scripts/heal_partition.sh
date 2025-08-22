#!/bin/bash

M1=keydb-master1
M2=keydb-master2

echo "🧹 Healing network partition: restoring communication between $M1 and $M2..."

docker exec $M1 bash -c "iptables -D OUTPUT -d $M2 -j DROP"
docker exec $M2 bash -c "iptables -D OUTPUT -d $M1 -j DROP"

echo "✅ Network partition healed. $M1 and $M2 can now communicate."
