/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.testutil.TestJodUser;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractServiceTest {

  @ServiceConnection
  static final PostgreSQLContainer<?> postgreSQLContainer = TestUtil.createPostgresSQLContainer();

  static {
    // Singleton containers are started before the tests and stopped after all tests have been run.
    // https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
    postgreSQLContainer.start();
  }

  @Autowired protected TestEntityManager entityManager;
  protected TestJodUser user;
  protected TestJodUser user2;

  @BeforeEach
  public void setup() {
    var yksilo = new Yksilo(UUID.randomUUID());
    yksilo.setTervetuloapolku(true);
    this.user = new TestJodUser(entityManager.persist(yksilo).getId());
    this.user2 = new TestJodUser(entityManager.persist(new Yksilo(UUID.randomUUID())).getId());
  }

  /** Simulates commit by flushing and clearing the entity manager. */
  @AfterEach
  public void simulateCommit() {
    entityManager.flush();
    entityManager.clear();
  }
}
