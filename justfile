#!/usr/bin/env just --justfile

APP := "jssl-server"
VERSION := "0.8.0"
NATIVE_DIR := "native"

alias db := docker-build
alias nl := native-linux
alias gkp := gen-keypair
alias ghp := github-push
alias gj := gradle-jar
alias c := clean

# Default recipe (this list)
default:
    @just --list

# Generate keystore jssl.jks for the server
gen-keypair:
    keytool -noprompt -genkeypair \
        -dname "CN=jssl-server.com, OU=Burgaud, O=Burgaud, C=FRANCE" \
        -keyalg RSA -alias selfsigned \
        -keystore jssl.jks -storepass password \
        -validity 360 -keysize 2048

# Build a docker image
docker-build: clean
    docker build -t andreburgaud/{{APP}}:latest .
    docker tag andreburgaud/{{APP}}:latest andreburgaud/{{APP}}:{{VERSION}}

# Native compile via container (Linux only)
native-linux: docker-build
    mkdir ./bin
    docker create --name {{APP}}-build andreburgaud/{{APP}}:{{VERSION}}
    docker cp {{APP}}-build:/{{APP}} ./bin
    docker cp {{APP}}-build:/jssl.jks ./bin
    docker rm -f {{APP}}-build
    zip -j bin/{{APP}}-{{VERSION}}_linux_{{arch()}}.zip ./bin/{{APP}} ./bin/jssl.jks

# Generate the jar file
gradle-jar:
    ./gradlew jar

# Generate the jar file
gradle-native: gradle-jar
    ./gradlew nativeCompile

# Generate the jar file
native-image: clean
    ./gradlew installDist
    mkdir -p {{NATIVE_DIR}}/bin
    native-image --initialize-at-build-time={{APP}} -Djava.security.properties==./java.security --no-fallback -jar build/install/jssl-server/lib/jssl-server.jar -o {{NATIVE_DIR}}/bin/{{APP}}

# Clean build and release artifacts
clean:
    ./gradlew clean
    -rm -rf ./bin

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
