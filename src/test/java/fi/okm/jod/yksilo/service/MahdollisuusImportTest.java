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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SqlConfig(separator = ";;;")
@Sql(scripts = {"/data/mock-tunnistus.sql", "/schema.sql", "/data.sql", "/data/mahdollisuudet.sql"})
@DirtiesContext
@Import({TyomahdollisuusService.class, KoulutusmahdollisuusService.class})
public class MahdollisuusImportTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withEnv("LANG", "en_US.UTF-8")
          .withEnv("LC_ALL", "en_US.UTF-8");

  @Autowired protected TestEntityManager entityManager;

  @Autowired private TyomahdollisuusService tyomahdollisuusService;
  @Autowired private KoulutusmahdollisuusService koulutusmahdollisuusService;

  @Test
  void shouldImportMahdollisuusData() {
    assertDoesNotThrow(
        () -> {
          entityManager
              .getEntityManager()
              .createStoredProcedureQuery("tyomahdollisuus_data.import")
              .execute();
        });
    assertDoesNotThrow(
        () -> {
          entityManager
              .getEntityManager()
              .createStoredProcedureQuery("koulutusmahdollisuus_data.import")
              .execute();
        });

    var k =
        assertDoesNotThrow(
            () ->
                koulutusmahdollisuusService.get(
                    UUID.fromString("30080e88-f292-48a3-9835-41950817abd3")));
    assertEquals(KoulutusmahdollisuusTyyppi.TUTKINTO, k.tyyppi());
    assertThat(k.jakaumat()).isNotEmpty();

    var t =
        assertDoesNotThrow(
            () ->
                tyomahdollisuusService.get(
                    UUID.fromString("bc77f514-8573-11ef-8f0c-b767f527df04")));
    assertThat(t.jakaumat()).isNotEmpty();
  }
}
