#FROM ghcr.io/graalvm/native-image:22.3.1 as build
FROM ghcr.io/graalvm/native-image:muslib-ol9-java17-22.3.2 as build

ENV LANG=C.UTF-8

RUN useradd -u 10001 jssluser

RUN microdnf -y install findutils xz

RUN curl --location --output upx-4.0.2-amd64_linux.tar.xz "https://github.com/upx/upx/releases/download/v4.0.2/upx-4.0.2-amd64_linux.tar.xz" && \
    tar -xJf "upx-4.0.2-amd64_linux.tar.xz" && \
    cp upx-4.0.2-amd64_linux/upx /bin/

ADD . .

COPY Manifest.txt jssl.jks /

COPY java.security ${JAVA_HOME}/conf/security

RUN mkdir /build

RUN javac -d /build src/main/java/com/burgaud/jssl/Server.java && \
    jar cfvm /jssl.jar /Manifest.txt -C /build .

RUN native-image --static --no-fallback --libc=musl -jar /jssl.jar -o /jssl-server -H:IncludeResources=".*/jssl.jks$" && \
    strip /jssl-server && \
    upx --best /jssl-server

FROM scratch
COPY --from=build /jssl-server /jssl-server
COPY --from=build /jssl.jks /jssl.jks
COPY --from=build /etc/passwd /etc/passwd

ENV LANG=C.UTF-8

USER jssluser

ENTRYPOINT [ "/jssl-server" ]
