FROM container-registry.oracle.com/graalvm/native-image:21-muslib-ol9 as build

ENV LANG=C.UTF-8

WORKDIR /jssl-server

RUN useradd -u 10001 jssluser

RUN microdnf -y install findutils xz

RUN curl --location --output upx-4.1.0-amd64_linux.tar.xz "https://github.com/upx/upx/releases/download/v4.1.0/upx-4.1.0-amd64_linux.tar.xz" && \
    tar -xJf "upx-4.1.0-amd64_linux.tar.xz" && \
    cp upx-4.1.0-amd64_linux/upx /bin/

RUN mkdir -p ./native/bin

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN keytool -noprompt -genkeypair \
        -dname "CN=jssl-server.com, OU=Burgaud, O=Burgaud, C=FRANCE" \
        -keyalg RSA -alias selfsigned \
        -keystore jssl.jks -storepass password \
        -validity 360 -keysize 2048

RUN ./gradlew installDist --no-daemon

RUN native-image --static --no-fallback --libc=musl -jar ./build/install/jssl-server/lib/jssl-server.jar -o /jssl-server/native/bin/jssl-server

RUN strip /jssl-server/native/bin/jssl-server && \
    upx --best /jssl-server/native/bin/jssl-server

FROM scratch
COPY --from=build /jssl-server/native/bin/jssl-server /jssl-server
COPY --from=build /jssl-server/jssl.jks /jssl.jks
COPY --from=build /etc/passwd /etc/passwd

ENV LANG=C.UTF-8

USER jssluser

ENTRYPOINT [ "/jssl-server" ]
