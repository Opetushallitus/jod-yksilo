FROM eclipse-temurin:21-jre-alpine
COPY build/libs/tmt-mock-*.jar /opt/app/application.jar
RUN chmod +x /opt/app
WORKDIR /opt/app
CMD ["java", "-jar", "/opt/app/application.jar"]
