# JSSL Server

JSSL Server is a test server enabling all SSL/TLS protocols (SSLv3 through TLSv1.3) to test [JSSL Client](https://github.com/andreburgaud/jssl), a tool that checks misconfigured TLS servers.

The simplest way to use **JSSL Server** is to pull the Docker image available at https://hub.docker.com/r/andreburgaud/jssl-server.

## Start the JSSL Server Container

```
$ docker run --rm -it -p 9999:9999 andreburgaud/jssl-server:0.7.0

   _ ___ ___ _    ___
 _ | / __/ __| |  / __| ___ _ ___ _____ _ _
| || \__ \__ \ |__\__ \/ -_) '_\ V / -_) '_|
 \__/|___/___/____|___/\___|_|  \_/\___|_|

JSSL Test Server version 0.7.0 - Java version 17.0.7
(c) 2023 Andre Burgaud

Starting single threaded JSSL Test Server at localhost:9999
Enabled protocols: SSLv3, TLSv1, TLSv1.1, TLSv1.2, TLSv1.3

```

To validate that the server is running, you can execute the following:

```
$ curl -v -k https://localhost:9999
```

To test if **JSSL Server** supports TLS1.2:

```
$ echo -n "Q" | openssl s_client -connect localhost:9999 -tls1_2
```

Your local [OpenSSL](https://www.openssl.org/) may be limited by the SSL/TLS versions enabled during the OpenSSL build. 
To get the full spectrum of the SSL/TLS protocols available for testing in **JSSL Server**, 
you can use **JSSL Client** available at https://github.com/andreburgaud/jssl.  

## Build

The primary options to build **JSSL Server** is to create a `jar` file or to build a `Docker` image. 

### Jar File

Building a jar file requires a recent version of Java supporting multiline string like Java 17 or 19.

```
$ ./gradlew jar
```

To execute the Jar file, you also need to generate a `keystore` file (the following is an example you can modify):

```
$ keytool -noprompt -genkeypair \
    -dname "CN=example.com, OU=some_group, O=some_company, L=Paris, C=FRANCE" \
    -keyalg RSA -alias selfsigned \
    -keystore jssl.jks -storepass password \
    -validity 360 -keysize 2048
```

Then:

```
$ java -Djava.security.properties==./java.security -jar build/libs/jssl-server.jar
```

The `java.security.properties` value needs to be overridden to enable all SSL/TLS protocols (from SSLv3 to TLS1.3).


### Docker Container

The following steps require to have `docker` installed on the local machine.

```
docker build -t jssl-server .
```

The build process includes a first stage that build a native image with [GraalVM](https://www.graalvm.org/) and compress it with [UPX](https://upx.github.io/).
Then, the build copies the final executable into a [scratch image](https://hub.docker.com/_/scratch). The final image less than 10MB.  

To run the container:

```
$ docker run --rm -it -p 9999:9999 jssl-server:latest
```

You can test the container with `curl` or `openssl` as described in section **Start the JSSL Server Container** at the top of the README.

### Native Image

If you are on Linux x86-64, you can build the Docker image and then extract the files needed to run JSSL Server locally:

```
$ docker build -t jssl-server .
$ docker create --name jssl-server-copy jssl-server:latest
$ docker cp jssl-server-copy:/jssl-server .
$ docker cp jssl-server-copy:/jssl.jks .
$ docker rm -f jssl-server-copy
```

To start the server:

```
$ ./jssl-server
```

## License

JSSL Server is available under the [MIT License](LICENSE)
