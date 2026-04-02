/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.testutil;

import java.nio.charset.StandardCharsets;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class TestUtil {

  private static final String POSTGRES_VERSION = "postgres:16-alpine";
  private static final String REDIS_VERSION = "redis:7-alpine";

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

  private static final String INITDB_DIR = "/docker-entrypoint-initdb.d/";
  private static final String INITDB_SRC = "scripts/docker-entrypoint-initdb.d/";

  /**
   * Creates a PostgreSQL test container that replicates the exact same setup as local dev
   * (compose.yml).
   *
   * <p>The container is initialized as the default {@code test} superuser, which runs the init
   * scripts. The {@code getUsername()} and {@code getPassword()} methods are overridden, so that
   * {@code @ServiceConnection} configures Spring's datasource to connect as the non-superuser.
   */
  @SuppressWarnings({"resource"})
  public static PostgreSQLContainer createPostgreSqlContainer() {
    return new PostgreSQLContainer(TestUtil.POSTGRES_VERSION) {
      @Override
      public String getUsername() {
        return "yksilo";
      }

      @Override
      public String getPassword() {
        return "yksilo";
      }
    }.withDatabaseName("yksilo")
        .withEnv("LANG", "en_US.UTF-8")
        .withEnv("LC_ALL", "en_US.UTF-8")
        .withCopyToContainer(MountableFile.forHostPath(INITDB_SRC), INITDB_DIR);
  }

  @SuppressWarnings({"resource"})
  public static GenericContainer<?> createRedisContainer() {
    return new GenericContainer<>(DockerImageName.parse(REDIS_VERSION)).withExposedPorts(6379);
  }
}
