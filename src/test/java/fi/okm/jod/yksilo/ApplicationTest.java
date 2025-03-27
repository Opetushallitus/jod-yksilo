/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.testutil.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@AutoConfigureMockMvc
@EnableAutoConfiguration(
    exclude = ObservationAutoConfiguration.class /* excluded due to intermittent test failures */)
class ApplicationTest implements IntegrationTest {

  @Autowired protected MockMvc mockMvc;

  @Container @ServiceConnection
  private static final GenericContainer<?> REDIS_CONTAINER = TestUtil.createRedisContainer();

  @Container @ServiceConnection
  private static final PostgreSQLContainer<?> POSTGRES_SQL_CONTAINER =
      TestUtil.createPostgresSQLContainer();

  @Test
  void contextLoads() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
