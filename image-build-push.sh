#!/usr/bin/env bash
# Build and Publish
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

protoc --plugin=protoc-gen-grpc-java=protoc-gen-grpc-java-1.66.0-osx-aarch_64.exe --grpc-java_out=src/main/java src/main/resources/proto/expenseCategorizer.proto

VERSION=2.0.0
mvn clean package -X
docker build -t alokkusingh/home-etl-service:latest -t alokkusingh/home-etl-service:${VERSION} --build-arg JAR_FILE=target/home-etl-service-1.0.0-SNAPSHOT.jar .
docker push alokkusingh/home-etl-service:latest
docker push alokkusingh/home-etl-service:${VERSION}