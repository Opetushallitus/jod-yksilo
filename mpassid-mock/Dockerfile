FROM eclipse-temurin:25-jre-alpine
COPY build/libs/mpassid-mock-*.jar /opt/app/application.jar
RUN chmod +x /opt/app
WORKDIR /opt/app
CMD ["java", "-jar", "/opt/app/application.jar"]

