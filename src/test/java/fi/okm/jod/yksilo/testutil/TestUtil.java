/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.testutil;

import fi.okm.jod.yksilo.domain.JodUser;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class TestUtil {

  public static final String POSTGRES_VERSION = "postgres:16-alpine";
  public static final String REDIS_VERSION = "redis:7-alpine";

  private TestUtil() {
    // Utility class.
  }

  public static String getContentFromFile(String filename, Class<?> clazz) {
    try (var inputStream = clazz.getResourceAsStream(filename)) {
      if (inputStream == null) {
        throw new RuntimeException(
            filename + " was NOT found in test resource folder: " + clazz.getPackageName());
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Could not read file: " + filename, e);
    }
  }

  public static void authenticateUser(JodUser jodUser) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new TestingAuthenticationToken(jodUser, null));
    SecurityContextHolder.setContext(context);
  }

  public static PostgreSQLContainer<?> createPostgresSQLContainer() {
    return new PostgreSQLContainer<>(TestUtil.POSTGRES_VERSION)
        .withEnv("LANG", "en_US.UTF-8")
        .withEnv("LC_ALL", "en_US.UTF-8");
  }

  public static GenericContainer<?> createRedisContainer() {
    return new GenericContainer<>(DockerImageName.parse(REDIS_VERSION)).withExposedPorts(6379);
  }
}
