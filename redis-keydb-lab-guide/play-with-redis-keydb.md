
install redis-server in ubuntu

```bash
sudo apt update
sudo apt install redis-server
```
check status & stop redis-server

```bash
sudo systemctl status redis-server
sudo systemctl stop redis-server
```

--

start redis-server in standalone mode ( master )

```bash
mkdir redis-master
cd redis-master
touch redis.conf
sudo redis-server redis.conf
```

use redis-cli to connect to redis-server

```bash
redis-cli -h localhost -p 6379
PING
```
