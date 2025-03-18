/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.benmanes.caffeine.cache.Ticker;
import fi.okm.jod.yksilo.repository.AmmattiRepository;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/data/ammatti.sql")
@Import({AmmattiService.class})
class AmmattiServiceTest {
  @Autowired private AmmattiRepository repository;
  @Autowired private TestEntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  private AmmattiService service;
  private TestTicker ticker;
  private TransactionTemplate transactionTemplate;

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgreSQLContainer = TestUtil.createPostgresSQLContainer();

  @BeforeEach
  void setUp() {
    ticker = new TestTicker();
    service = new AmmattiService(repository, ticker, Runnable::run);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Test
  void shouldFindAll() {
    var result = service.findAll(0, 10);
    assertEquals(7, result.payload().maara());
    assertEquals(1, result.version());
  }

  @Test
  void shouldFindByUri() {
    var result =
        service.findBy(0, 10, Set.of(URI.create("urn:ammatti1"), URI.create("urn:osaaminen1")));
    assertThat(result.payload().sisalto())
        .extracting("uri")
        .containsExactly(URI.create("urn:ammatti1"));
  }

  @Test
  @DirtiesContext
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void shouldReloadCacheIfVersionChanges() {

    var result = service.findAll(0, 10);
    assertEquals(1, result.version());
    var maara = result.payload().maara();

    transactionTemplate.execute(
        status -> {
          entityManager
              .getEntityManager()
              .createNativeQuery("UPDATE ammatti_versio SET versio = versio + 1")
              .executeUpdate();

          entityManager
              .getEntityManager()
              .createNativeQuery(
                  "INSERT INTO ammatti (id, uri, koodi) VALUES (8, 'urn:ammatti8', 'koodi8')")
              .executeUpdate();

          entityManager.flush();
          return null;
        });
    ticker.set(AmmattiService.CACHE_DURATION.toNanos() + 1);
    result = service.findAll(0, 10);
    assertEquals(2, result.version());
    assertEquals(maara + 1, result.payload().maara());
  }

  static class TestTicker implements Ticker {
    private volatile long time = 0;

    @Override
    public long read() {
      return time;
    }

    void set(long duration) {
      time = duration;
    }
  }
}
