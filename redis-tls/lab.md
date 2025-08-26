mkdir redis-certs
cd redis-certs

openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt \
  -subj "/C=IN/ST=State/L=City/O=YourOrg/CN=Redis-Test-CA"


openssl genrsa -out redis.key 2048

openssl req -new -key redis.key -out redis.csr -subj "/C=IN/ST=State/L=City/O=YourOrg/CN=localhost"
openssl x509 -req -in redis.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out redis.crt -days 3650 -sha256


redis.conf


port 0                    # Disable non-TLS port for demo (or keep if dual-mode)
tls-port 6379             # Enable TLS on default port
tls-cert-file /path/to/redis-certs/redis.crt
tls-key-file /path/to/redis-certs/redis.key
tls-ca-cert-file /path/to/redis-certs/ca.crt
tls-auth-clients no       # Set to 'yes' for client certificate validation (mTLS)


redis-server /path/to/redis.conf


redis-cli --tls \
  --cert ./redis-certs/redis.crt \
  --cacert ./redis-certs/ca.crt \
  -h 127.0.0.1 -p 6379



-----------------------------
mTLS (Mutual TLS) Configuration
-----------------------------


openssl genrsa -out client.key 2048

openssl req -new -key client.key -out client.csr -subj "/C=IN/ST=State/L=City/O=YourOrg/CN=localhost"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out client.crt -days 3650 -sha256


redis-cli --tls \
  --cert ./redis-certs/client.crt \
  --key ./redis-certs/client.key \
  --cacert ./redis-certs/ca.crt \
  -h 127.0.0.1 -p 6380