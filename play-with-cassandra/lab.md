

Fresh start (wipe any old state first):

docker network create cass-net 2>/dev/null || true
docker compose -f compose.dc1.yml down -v
docker compose -f compose.dc2.yml down -v


Start everything:

docker compose -f compose.dc1.yml up -d
docker compose -f compose.dc2.yml up -d


Join non-seeds one-by-one (dc1 then dc2):

docker exec -it dc1-n2 nodetool join
docker exec -it dc1-n3 nodetool join
docker exec -it dc1-n4 nodetool join

docker exec -it dc2-n2 nodetool join
docker exec -it dc2-n3 nodetool join
docker exec -it dc2-n4 nodetool join


Verify DCs:

docker exec -it dc1-seed1 nodetool status   # Datacenter: dc1 (4 nodes UN)
docker exec -it dc2-seed1 nodetool status   # Datacenter: dc2 (4 nodes UN)


If you prefer automatic joining (no nodetool join), remove the JVM_EXTRA_OPTS lines and instead start one non-seed at a time until each reaches UN before launching the next.


---------------------------------------------------------------

