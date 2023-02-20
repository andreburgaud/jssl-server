FROM ghcr.io/graalvm/native-image:22.3.1 as build

ADD . .

COPY Manifest.txt jssl.jks /

COPY java.security ${JAVA_HOME}/conf/security

RUN microdnf -y install gzip zlib-static

RUN mkdir /build

RUN javac -d /build src/main/java/com/burgaud/jssl/Server.java

RUN jar cfvm /jssl.jar /Manifest.txt -C /build .

RUN jar tf /jssl.jar

ARG RESULT_LIB="/musl"
RUN mkdir ${RESULT_LIB} && \
    curl -L -o musl.tar.gz https://more.musl.cc/10.2.1/x86_64-linux-musl/x86_64-linux-musl-native.tgz && \
    tar -xvzf musl.tar.gz -C ${RESULT_LIB} --strip-components 1

ENV CC=/musl/bin/gcc

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.13.tar.gz && \
    mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && \
    cd zlib && ./configure --static --prefix=/musl && \
    make && make install && \
    cd / && rm -rf /zlib && rm -f /zlib.tar.gz

ENV PATH="$PATH:/musl:/musl/bin"

RUN native-image --static --libc=musl -jar /jssl.jar -o /jssl-server

FROM scratch
COPY --from=build /jssl-server /jssl-server
COPY --from=build /jssl.jks /jssl.jks

ENTRYPOINT [ "/jssl-server" ]
