# JSSL Server

JSSL Server is a test server enabling all SSL/TLS protocal for the purpose of testing JSSL, a tool which checks misconfigured TLS servers.


## Build

Building the native version of the executable requires a Java GraalVM version with the native tools installed. If you use the `justfile`, similar to a `Makefile`, you will need to install [`just`](https://github.com/casey/just).

The resulting server will require to access a keystore file named `jssl.jks`. You can generate the file with `just gen-keypair`, or, if you don't have `just` installed on your computer, look at the task `gen-keypair` in the `justfile`.

### Jar

Building a jar file and executing the server from the jar file does not require `GraalVM` or `just` to be installed. A recent version of Java supporting multiline string is sufficient (Java 17 or 19).

```
$ ./gradlew jar
```

Then:

```
$ java -Djava.security.properties==./java.security -jar build/libs/jssl-server.jar

```

The `java.security.properties` value needs to be overridden to enable all SSL/TLS protocols (from SSLv3 to TLS1.3).

### Native Local

To generate a native version of the server locally, you need to use GraalVM. You can install GraalVM with [sdkman](https://sdkman.io/), as follow:

```
$ sdk install java 22.3.r19-grl
```

To use GraalVM version 22.3.r19-grl in a terminal:

```
$ sdk use java 22.3.r19-grl
```

Before compiling to a native executable, you will need to modify `${JAVA_HOME}/conf/security/java.security` and comment out the following lines:

```
#jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \
#    DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL
```

It is necessary as the native executable inherits from the Java configuration at compile time. Then:

```
$ ./gradlew jar
$ ./gradlew nativeCompile
```

The executable `jssl-server` will be available in the `build/native/nativeCompile` folder.

You can execute `jssl-server` which require the keystore `jssl.jks` in the directory the server is launch. For example, from the project root, you can execute the following:

```
$ build/native/nativeCompile/jssl-server
```

To get additional information, display the help as follows:

```
$ build/native/nativeCompile/jssl-server --help
```

### Native Container

The following steps require to have `docker` installed on the local machine.

```
docker build -t jssl-server .
```

To execute the container:

```
$ docker run --rm -it -p 9999:9999 jssl-server:latest
```

To test if the server is running, you can use `curl`:

```
$ curl -v -k https://localhost:9999
```
