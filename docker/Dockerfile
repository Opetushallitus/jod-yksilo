FROM public.ecr.aws/amazoncorretto/amazoncorretto:25-al2023-headless AS baseimage

RUN :\
    && dnf -qy update \
    && dnf -qy upgrade \
    && dnf -qy install shadow-utils \
    && adduser --system app --shell /bin/false --home /opt/app \
    && dnf -qy remove shadow-utils \
    && dnf -qy clean all

FROM baseimage AS app

COPY build/libs/jod-yksilo*.jar /tmp/application.jar
RUN cd /tmp && java -Djarmode=tools -jar /tmp/application.jar extract

FROM baseimage

COPY --from=app --chmod=444 /tmp/application/lib /opt/app/lib
COPY --from=app --chmod=444 /tmp/application/application.jar /opt/app
RUN chmod +x /opt/app /opt/app/lib

USER app
WORKDIR /opt/app

ENV JAVA_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=384m"
# For netty: https://github.com/netty/netty/wiki/Java-24-and-sun.misc.Unsafe
ENV NATIVE_OPTIONS="--sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["/bin/bash","-c","exec /usr/bin/java -XX:+ExitOnOutOfMemoryError \
 $NATIVE_OPTIONS \
 $JAVA_UNSAFE_OPTIONS \
 -jar /opt/app/application.jar \"$@\"", "--"]

VOLUME ["/tmp"]
