#!/usr/bin/env just --justfile

APP := "jssl-server"
VERSION := "0.5.0"

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
        -dname "CN=burgaud.com, OU=AB, O=AB, L=Minneapolis, S=MN, C=USA" \
        -keyalg RSA -alias selfsigned \
        -keystore jssl.jks -storepass password \
        -validity 360 -keysize 2048

# Build a docker image
docker-build:
    docker build -t andreburgaud/{{APP}}:latest .
    docker tag andreburgaud/{{APP}}:latest andreburgaud/{{APP}}:{{VERSION}}

# Native compile via container (Linux only)
native-linux: clean
    mkdir ./bin
    docker create --name {{APP}}-build andreburgaud/{{APP}}:{{VERSION}}
    docker cp {{APP}}-build:/{{APP}} ./bin
    docker rm -f {{APP}}-build
    zip -j bin/{{APP}}-{{VERSION}}_linux_{{arch()}}.zip ./bin/{{APP}}

# Generate the jar file
gradle-jar:
    ./gradlew jar

# Generate the jar file
gradle-native: gradle-jar
    ./gradlew nativeCompile

# Clean build and release artifacts
clean:
    ./gradlew clean
    -rm -rf ./bin

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
