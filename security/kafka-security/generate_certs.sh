#!/bin/bash
set -e

PASSWORD="changeit"
CA_KEY="kafka-ca.key"
CA_CERT="kafka-ca.crt"
CA_SERIAL="kafka-ca.srl"
SAN_CONFIG="localhost-san.cnf"

# Create OpenSSL SAN config
cat > $SAN_CONFIG <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = req_ext
prompt = no

[req_distinguished_name]
CN = localhost

[req_ext]
subjectAltName = DNS:localhost,DNS:node1,DNS:node2,DNS:node3
EOF

echo "Generating CA key and certificate..."
openssl genrsa -out $CA_KEY 4096
openssl req -x509 -new -nodes -key $CA_KEY -sha256 -days 3650 -out $CA_CERT -subj "/CN=Kafka Test CA"

for NODE in node1 node2 node3; do
  echo "Generating key and CSR for $NODE..."
  openssl genrsa -out ${NODE}.key 2048
  openssl req -new -key ${NODE}.key -out ${NODE}.csr -config $SAN_CONFIG

  echo "Signing certificate for $NODE with CA..."
  openssl x509 -req -in ${NODE}.csr -CA $CA_CERT -CAkey $CA_KEY -CAcreateserial \
    -out ${NODE}.crt -days 365 -sha256 -extfile $SAN_CONFIG -extensions req_ext

  echo "Creating PKCS#12 keystore for $NODE..."
  openssl pkcs12 -export -in ${NODE}.crt -inkey ${NODE}.key -out ${NODE}.p12 -name $NODE \
    -CAfile $CA_CERT -caname root -password pass:$PASSWORD
done

echo "Import CA cert into truststore..."
keytool -importcert -file $CA_CERT -alias root-ca -keystore kafka-truststore.p12 \
  -storetype PKCS12 -storepass $PASSWORD -noprompt

echo "All certificates and keystores generated successfully."
