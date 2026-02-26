/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import fi.okm.jod.yksilo.testutil.TestTracingConfig;
import fi.okm.jod.yksilo.testutil.TestUtil;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("SLOW")
@Import(TestTracingConfig.class)
public abstract class IntegrationTest {

  @ServiceConnection
  private static final PostgreSQLContainer POSTGRES_CONTAINER =
      TestUtil.createPostgreSqlContainer();

  @ServiceConnection
  private static final GenericContainer<?> REDIS_CONTAINER = TestUtil.createRedisContainer();

  static {
    // Singleton containers are started before the tests and stopped after all tests have been run.
    // https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
    POSTGRES_CONTAINER.start();
    REDIS_CONTAINER.start();
  }
}
