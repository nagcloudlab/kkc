

install maven in ubuntu

```bash
sudo apt update
sudo apt install maven  
```

```bash
cd <client-application-directory>
mvn clean compile exec:java -Dexec.mainClass="com.example.<MainClassName>" -Dexec.args="<arguments>"
```