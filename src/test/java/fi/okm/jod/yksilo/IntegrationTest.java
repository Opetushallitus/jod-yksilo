/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("SLOW")
public abstract class IntegrationTest {

  @Autowired protected ObjectMapper objectMapper;

  @ServiceConnection
  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
      TestUtil.createPostgreSqlContainer();

  @ServiceConnection
  private static final GenericContainer<?> REDIS_CONTAINER = TestUtil.createRedisContainer();

  static {
    // Singleton containers are started before the tests and stopped after all tests have been run.
    // https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
    POSTGRES_CONTAINER.start();
    REDIS_CONTAINER.start();
  }

  protected <T> T getResponse(MvcResult result, Class<T> clazz)
      throws UnsupportedEncodingException, JsonProcessingException {
    final String json = result.getResponse().getContentAsString();
    return this.objectMapper.readValue(json, clazz);
  }
}
