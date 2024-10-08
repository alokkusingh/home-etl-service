# Home ETL Service 
Home Stack ETL Service

## Functionality
- Reads downloaded bank PDF statements (with password/without password protection) or imported CSV files from bank site
- Parses it (based on plugable parsing logic)
- Process the records (transaction categorization, amount extraction)
- Writes to H2/MySql DB
- Finally export to csv format order by transaction date to be imported to Excel or Google Sheet

### Supported Bank Statemetnts
1. Citi Bank Saving Account 
2. Kotak Mahindra Bank Saving Account 
3. HDFC Bank

### How to run
````
java -jar target/home-etl-service-1.0.0-SNAPSHOT.jar --file.path.base.dir=/home/alok/data/git/BankStatements
````

### Build
#### Set JAVA_HOME (in case mvn run through terminal)
```shell
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```
#### Generate Proto file
```shell
protoc --java_out=src/main/java src/main/resources/proto/expenseCategorizer.proto
```
#### Generate gRPC Stub
```shell
protoc --plugin=protoc-gen-grpc-java=protoc-gen-grpc-java-1.66.0-osx-aarch_64.exe --grpc-java_out=src/main/java src/main/resources/proto/expenseCategorizer.proto
```
1. Maven Package
   ```shell
   mvn clean package -X
   ```
2. Docker Build, Push & Run
   ```shell
   docker build -t alokkusingh/home-etl-service:latest -t alokkusingh/home-etl-service:2.0.0 --build-arg JAR_FILE=target/home-etl-service-1.0.0-SNAPSHOT.jar .
   ```
   ```shell
   docker push alokkusingh/home-etl-service:latest
   ```
   ```shell
   docker push alokkusingh/home-etl-service:2.0.0
   ```
   ```shell
   docker run -d -v /home/alok/data/git/BankStatements:/Users/aloksingh/BankStatements:rw,Z -p 8081:8081 --rm --name home-etl-service alokkusingh/home-etl-service
   ```
   
### Manual commands
```shell
docker run -it --entrypoint /bin/bash -v /home/alok/data/git/BankStatements:/Users/aloksingh/BankStatements:rw,Z -p 8081:8081 --rm --name home-etl-service alokkusingh/home-etl-service
```
```shell
java -Djava.security.egd=file:/dev/urandom -Dspring.profiles.active=prod -Dspring.datasource.url=jdbc:mysql://192.168.1.200:32306/home-stack -Dspring.datasource.hikari.minimum-idle=5 \
-Dspring.datasource.hikari.connection-timeout=20000 -Dspring.datasource.hikari.maximum-pool-size=10 -Dspring.datasource.hikari.idle-timeout=10000 \
-Dpring.datasource.hikari.max-lifetime=1000 -Dspring.datasource.hikari.auto-commit=true -jar /opt/app.jar
```
```shell
docker run -v /home/alok/data/git/BankStatements:/Users/aloksingh/BankStatements:rw,Z -p 8081:8081 --rm --name home-etl-service alokkusingh/home-etl-service \
--java.security.egd=file:/dev/urandom --spring.profiles.active=prod --spring.datasource.url=jdbc:mysql://192.168.1.200:32306/home-stack --spring.datasource.hikari.minimum-idle=5 \
--spring.datasource.hikari.connection-timeout=20000 --spring.datasource.hikari.maximum-pool-size=10 --spring.datasource.hikari.idle-timeout=10000 \
--pring.datasource.hikari.max-lifetime=1000 --spring.datasource.hikari.auto-commit=true
```
```shell
curl --location 'http://localhost:8081/home/etl/gsheet/refresh/expense' --header 'Accept: text/event-stream;charset=UTF-8'
```
```shell
curl --location 'http://localhost:8081/home/etl/gsheet/refresh/expense'
```