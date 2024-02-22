# JOD Yksilö Backend

Part of the [Digital Service Ecosystem for Continuous Learning (JOD) project](https://wiki.eduuni.fi/pages/viewpage.action?pageId=404882394).

---

Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
The Ministry of Economic Affairs and Employment, The Finnish National Agency of
Education (Opetushallitus) and The Finnish Development and Administration centre
for ELY Centres and TE Offices (KEHA).

Licensed under the European Union Public Licence EUPL-1.2 or later.

---

## Getting Started

The backend application is a Spring Boot 3 application that requires Java 21 and uses Gradle
as a build tool.

- Install a OpenJDK 21 distribution (e.g. [Eclipse Temurin™](https://adoptium.net/temurin/releases/)).
- Clone the repository.
- Run the application with `./gradlew bootRun`.

## Development

- Build and test application with `./gradlew build`
- Code style is enforced using Spotless and Checkstyle (based on Google Java Style).
  - You can format the code with `./gradlew spotlessApply`.
  - If using IntelliJ IDEA, the Checkstyle-IDEA and google-java-format plugins are recommended.

### Keycloak IdP

- Run development Keycloak with `docker compose -f docker/keycloak/docker-compose.yml -p jod-yksilo up -d`.
- Navigate to [JOD realm keys](http://localhost:8080/admin/master/console/#/jod/realm-settings/keys/providers) and do following steps:
  - Remove **rsa-generated** key provider.
  - Remove **rsa-enc-generated** key provider.
  - Add **rsa** provider using `rp.key` as private key and `rp.crt` as certificate.
  - Add **rsa-enc** provider using `rp.key` as private key and `rp.crt` as certificate.
