FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-headless AS baseimage

RUN :\
    && dnf -qy update \
    && dnf -qy upgrade \
    && dnf -qy install shadow-utils \
    && adduser --system app --shell /bin/false --home /opt/app \
    && dnf -qy remove shadow-utils \
    && dnf -qy clean all

FROM baseimage
COPY --chmod=444 build/libs/jod-yksilo*.jar /opt/app/application.jar
USER app
WORKDIR /opt/app

ENV JAVA_OPTIONS="-XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=256m"
ENTRYPOINT ["/bin/bash","-c",\
 "exec /usr/bin/java -XX:+ExitOnOutOfMemoryError \
 $JAVA_OPTIONS -jar /opt/app/application.jar \"$@\"", "--"]
