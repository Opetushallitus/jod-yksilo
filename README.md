# JOD Yksilö Backend

Part of
the [Digital Service Ecosystem for Continuous Learning (JOD) project](https://wiki.eduuni.fi/pages/viewpage.action?pageId=404882394).

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

* Install a OpenJDK 21 distribution (
  e.g. [Eclipse Temurin™](https://adoptium.net/temurin/releases/)).
* Clone the repository.
* Run the application with `./gradlew bootRun`.

## Development

Build and test application with `./gradlew build`

To locally use the inference endpoints (e.g. /api/ehdotus/osaamiset, work-in-progress),
some additional configuration is required. Please see the (currently internal) documentation at
https://wiki.eduuni.fi/pages/viewpage.action?pageId=488735698

### Code Style

Code style is enforced using Spotless and Checkstyle (based on Google Java Style).

* You can format the code with `./gradlew spotlessApply`.
* If using IntelliJ IDEA, the Checkstyle-IDEA and google-java-format plugins are recommended.

* Variable declarations should prefer local type inference (`var`) over explicit types where type is
  clear from the context (e.g. already present on the right-hand side of the assignment, or
  otherwise impled).
* Naming is hard, so avoid unnecessary intermediate variables, unless they improve readability.
* When naming things, do not repeat information that is already present in the context (e.g.
  enclosing class name or method argument types).

### Database migrations

The database migrations are managed with Flyway, located in the
`src/main/resources/db/migration` directory.

The migrations should be named in the format `V<yyymmdd>.<id>__<description>.sql`, where `<id>` is
the Jira issue number of the migration, and `<description>` is a short description of the migration.
For example, `V20250708.1234__add_new_table.sql`.

