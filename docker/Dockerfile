FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-headless AS baseimage

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

USER app
WORKDIR /opt/app
COPY --from=app --chmod=444 /tmp/application/lib /opt/app/lib
COPY --from=app --chmod=444 /tmp/application/application.jar /opt/app

ENV JAVA_OPTIONS="-XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=256m"
ENTRYPOINT ["/bin/bash","-c",\
 "exec /usr/bin/java -XX:+ExitOnOutOfMemoryError \
 $JAVA_OPTIONS -jar /opt/app/application.jar \"$@\"", "--"]

VOLUME ["/tmp"]
