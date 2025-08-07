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
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/ammatti.sql")
@Import({AmmattiService.class})
class AmmattiServiceTest extends AbstractServiceTest {
  @Autowired private AmmattiRepository repository;
  @Autowired private TestEntityManager entityManager;

  private AmmattiService service;
  private TestTicker ticker;

  @BeforeEach
  @Override
  public void setup() {
    ticker = new TestTicker();
    service = new AmmattiService(repository, ticker, Runnable::run);
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
  void shouldReloadCacheIfVersionChanges() {

    var result = service.findAll(0, 10);
    assertEquals(1, result.version());
    var maara = result.payload().maara();

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
